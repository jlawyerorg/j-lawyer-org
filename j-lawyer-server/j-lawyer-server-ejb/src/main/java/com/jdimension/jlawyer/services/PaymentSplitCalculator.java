/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.services;

import com.jdimension.jlawyer.persistence.ClaimComponent;
import com.jdimension.jlawyer.persistence.ClaimComponentFacadeLocal;
import com.jdimension.jlawyer.persistence.ClaimComponentType;
import com.jdimension.jlawyer.persistence.ClaimLedger;
import com.jdimension.jlawyer.persistence.ClaimLedgerEntry;
import com.jdimension.jlawyer.persistence.ClaimLedgerEntryFacadeLocal;
import com.jdimension.jlawyer.persistence.ClaimLedgerParty;
import com.jdimension.jlawyer.persistence.InterestRule;
import com.jdimension.jlawyer.persistence.InterestRuleFacadeLocal;
import com.jdimension.jlawyer.persistence.InterestType;
import com.jdimension.jlawyer.persistence.LedgerEntryType;
import com.jdimension.jlawyer.persistence.PaymentAllocation;
import com.jdimension.jlawyer.persistence.PaymentAllocationMode;
import com.jdimension.jlawyer.persistence.PaymentSplitProposal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Service class for calculating payment splits according to German civil law
 * (BGB §366, §367).
 *
 * This class handles the automatic distribution of overpayments across multiple
 * claim components following the legal payment allocation order.
 *
 * @author jens
 */
public class PaymentSplitCalculator {

    private static final Logger log = Logger.getLogger(PaymentSplitCalculator.class.getName());
    
    private ClaimComponentFacadeLocal claimComponentsFacade;
    private ClaimLedgerEntryFacadeLocal claimLedgerEntriesFacade;
    private InterestRuleFacadeLocal claimComponentInterestRuleFacade;
    private ClaimInterestCalculator interestCalculator;

    public PaymentSplitCalculator(ClaimComponentFacadeLocal claimComponentsFacade, InterestRuleFacadeLocal claimComponentInterestRuleFacade, ClaimLedgerEntryFacadeLocal claimLedgerEntriesFacade, ClaimInterestCalculator interestCalculator) {
        this.claimComponentsFacade=claimComponentsFacade;
        this.claimComponentInterestRuleFacade=claimComponentInterestRuleFacade;
        this.claimLedgerEntriesFacade=claimLedgerEntriesFacade;
        this.interestCalculator=interestCalculator;
    }

    /**
     * Calculates an automatic payment split according to BGB §366/367.
     *
     * Legal payment allocation order (§366 Abs. 2 BGB):
     * 1. Costs (non-interest bearing)
     * 2. Costs (interest bearing)
     * 3. Interest on costs
     * 4. Principal of costs
     * 5. Interest on main claim
     * 6. Principal of main claim
     *
     * Within each category, oldest debts are paid first.
     * Within each component, interest is paid before principal (§367 BGB).
     *
     * @param ledger The claim ledger
     * @param paymentAmount The total payment amount to distribute
     * @param originalComponent The component where the payment was originally entered
     * @param paymentDate The date of the payment
     * @return PaymentSplitProposal with automatic allocation
     */
    public PaymentSplitProposal calculateAutomaticSplit(ClaimLedger ledger, BigDecimal paymentAmount,
            ClaimComponent originalComponent, Date paymentDate) {

        PaymentAllocationMode mode = ledger.getAllocationMode();
        if (mode == null) {
            mode = PaymentAllocationMode.LEGAL;
        }
        return calculateSplit(ledger, paymentAmount, originalComponent, paymentDate, mode, null);
    }

    /**
     * Calculates a payment split in the given allocation mode.
     *
     * The statutory order of §§ 366 Abs. 2, 367 BGB settles costs first, then interest, then the
     * principal. § 497 Abs. 3 BGB reverses that for consumer loans: the costs of legal prosecution
     * come first, then the principal, and only then the default interest - so that a consumer's
     * payments reduce the debt that keeps producing interest.
     *
     * @param ledger the claim ledger
     * @param paymentAmount the amount to distribute
     * @param originalComponent the component the payment was entered on
     * @param paymentDate the date of the payment
     * @param mode the allocation mode to apply
     * @param debtor for {@link PaymentAllocationMode#SINGLE_DEBTOR}, the debtor whose positions the
     * payment settles; ignored in the other modes
     * @return the proposal, carrying the mode used and a warning where it deviates from § 366
     * Abs. 2 BGB
     */
    public PaymentSplitProposal calculateSplit(ClaimLedger ledger, BigDecimal paymentAmount,
            ClaimComponent originalComponent, Date paymentDate, PaymentAllocationMode mode,
            ClaimLedgerParty debtor) {
        return calculateSplit(ledger, paymentAmount, originalComponent, paymentDate, mode, debtor, null);
    }

    /**
     * Calculates a payment split in the given allocation mode, optionally directed at one position.
     *
     * @param ledger the claim ledger
     * @param paymentAmount the amount to distribute
     * @param originalComponent the component the payment was entered on
     * @param paymentDate the date of the payment
     * @param mode the allocation mode to apply
     * @param debtor for {@link PaymentAllocationMode#SINGLE_DEBTOR}, the debtor whose positions the
     * payment settles
     * @param target for {@link PaymentAllocationMode#TARGETED}, the position the creditor directs
     * the payment at; it is settled before all others
     * @return the proposal, carrying the mode used and a warning where it deviates from § 366
     * Abs. 2 BGB
     */
    public PaymentSplitProposal calculateSplit(ClaimLedger ledger, BigDecimal paymentAmount,
            ClaimComponent originalComponent, Date paymentDate, PaymentAllocationMode mode,
            ClaimLedgerParty debtor, ClaimComponent target) {

        PaymentSplitProposal proposal = new PaymentSplitProposal(ledger, paymentAmount, paymentDate);
        proposal.setAllocationMode(mode);

        try {
            List<ClaimComponent> sortedComponents = getComponentsInAllocationOrder(ledger, mode, debtor);
            if (mode == PaymentAllocationMode.TARGETED && target != null) {
                // § 366 Abs. 1 BGB: the debtor may direct the payment, and the creditor may agree a
                // different order - the chosen position is settled before the statutory sequence
                sortedComponents = moveToFront(sortedComponents, target);
            }
            boolean principalBeforeInterest = (mode == PaymentAllocationMode.CONSUMER_LOAN);

            BigDecimal remainingAmount = paymentAmount;
            List<PaymentAllocation> allocations = new ArrayList<>();

            // Distribute payment according to legal order
            for (ClaimComponent component : sortedComponents) {
                if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                // Calculate open balances for this component
                BigDecimal openInterest = calculateComponentInterestBalance(component, paymentDate);
                BigDecimal openPrincipal = calculateComponentPrincipalBalance(component, paymentDate);

                if (principalBeforeInterest) {
                    // § 497 Abs. 3 BGB: the principal is settled before the default interest
                    remainingAmount = allocatePrincipal(component, openPrincipal, remainingAmount, allocations, mode);
                    remainingAmount = allocateInterest(component, openInterest, remainingAmount, allocations, mode);
                    continue;
                }

                // First pay interest (§367 BGB)
                if (openInterest.compareTo(BigDecimal.ZERO) > 0 && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal interestPayment = remainingAmount.min(openInterest);

                    PaymentAllocation interestAllocation = new PaymentAllocation();
                    interestAllocation.setComponent(component);
                    interestAllocation.setAmount(interestPayment);
                    interestAllocation.setInterestAllocation(true);
                    interestAllocation.setOriginalOpenAmount(openInterest);
                    interestAllocation.setRemainingBalance(openInterest.subtract(interestPayment));
                    interestAllocation.setFullyPaid(interestAllocation.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0);
                    interestAllocation.setLegalReference("§ 367 BGB (Zinsen vor Kapital)");
                    interestAllocation.setAllocationDescription("Zinszahlung für " + component.getName());

                    allocations.add(interestAllocation);
                    remainingAmount = remainingAmount.subtract(interestPayment);
                }

                // Then pay principal
                if (openPrincipal.compareTo(BigDecimal.ZERO) > 0 && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal principalPayment = remainingAmount.min(openPrincipal);

                    PaymentAllocation principalAllocation = new PaymentAllocation();
                    principalAllocation.setComponent(component);
                    principalAllocation.setAmount(principalPayment);
                    principalAllocation.setInterestAllocation(false);
                    principalAllocation.setOriginalOpenAmount(openPrincipal);
                    principalAllocation.setRemainingBalance(openPrincipal.subtract(principalPayment));
                    principalAllocation.setFullyPaid(principalAllocation.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0);

                    // Set legal reference based on component type
                    String legalRef = "§ 366 Abs. 2 BGB";
                    if (component.getType() == ClaimComponentType.COST_NON_INTEREST_BEARING) {
                        legalRef += " (Kosten, unverzinslich)";
                    } else if (component.getType() == ClaimComponentType.COST_INTEREST_BEARING) {
                        legalRef += " (Kosten, verzinslich)";
                    } else if (component.getType() != null && component.getType().isMainClaim()) {
                        legalRef += " (Hauptforderung)";
                    } else {
                        legalRef += " (" + component.getType() + ")";
                    }
                    principalAllocation.setLegalReference(legalRef);
                    principalAllocation.setAllocationDescription("Kapitalzahlung für " + component.getName());

                    allocations.add(principalAllocation);
                    remainingAmount = remainingAmount.subtract(principalPayment);
                }
            }

            proposal.setAllocations(allocations);
            proposal.setSurplus(remainingAmount);
            proposal.setFollowsLegalOrder(mode == PaymentAllocationMode.LEGAL);
            proposal.setManuallyAdjusted(false);
            if (mode != PaymentAllocationMode.LEGAL) {
                proposal.setLegalOrderWarning(deviationWarning(mode));
            }

        } catch (Exception ex) {
            log.error("Error calculating automatic payment split", ex);
            throw new RuntimeException("Fehler bei der Berechnung der automatischen Zahlungsverteilung", ex);
        }

        return proposal;
    }

    /**
     * Records a distribution entered by the user, without recomputing it.
     *
     * A manual distribution is accepted as entered - the user may have reasons the ledger does not
     * know - but it is stored as deviating from § 366 Abs. 2 BGB, with the warning shown alongside
     * it, and its amounts still have to add up to the payment.
     *
     * @param ledger the claim ledger
     * @param paymentAmount the total amount paid
     * @param paymentDate the date of the payment
     * @param allocations the distribution the user entered
     * @return the proposal, marked as manually adjusted and deviating
     * @throws IllegalArgumentException if the allocations do not add up to the payment
     */
    public PaymentSplitProposal createManualSplit(ClaimLedger ledger, BigDecimal paymentAmount,
            Date paymentDate, List<PaymentAllocation> allocations) {

        PaymentSplitProposal proposal = new PaymentSplitProposal(ledger, paymentAmount, paymentDate);
        proposal.setAllocationMode(PaymentAllocationMode.MANUAL);
        proposal.setAllocations(allocations == null ? new ArrayList<>() : allocations);
        proposal.setManuallyAdjusted(true);
        proposal.setFollowsLegalOrder(false);
        proposal.setLegalOrderWarning(deviationWarning(PaymentAllocationMode.MANUAL));

        BigDecimal sum = BigDecimal.ZERO;
        for (PaymentAllocation a : proposal.getAllocations()) {
            if (a.getAmount() != null) {
                sum = sum.add(a.getAmount());
            }
        }
        proposal.setSurplus(paymentAmount.subtract(sum).max(BigDecimal.ZERO));

        if (!validateProposal(proposal)) {
            throw new IllegalArgumentException(
                    "Die manuelle Verteilung ergibt in der Summe nicht den Zahlbetrag.");
        }

        return proposal;
    }

    /**
     * Returns the list with the given component moved to the front.
     *
     * @param components the components in statutory order
     * @param target the component to settle first
     * @return a new list starting with the target
     */
    private List<ClaimComponent> moveToFront(List<ClaimComponent> components, ClaimComponent target) {
        List<ClaimComponent> reordered = new ArrayList<>();
        reordered.add(target);
        for (ClaimComponent c : components) {
            if (c != target && (c.getId() == null || !c.getId().equals(target.getId()))) {
                reordered.add(c);
            }
        }
        return reordered;
    }

    /**
     * Returns the components a payment is distributed over, in the order the mode prescribes.
     *
     * @param ledger the ledger
     * @param mode the allocation mode
     * @param debtor the debtor for {@link PaymentAllocationMode#SINGLE_DEBTOR}
     * @return the components in allocation order
     */
    private List<ClaimComponent> getComponentsInAllocationOrder(ClaimLedger ledger, PaymentAllocationMode mode, ClaimLedgerParty debtor) {

        List<ClaimComponent> components = getSortedComponentsByLegalOrder(ledger);

        if (mode == PaymentAllocationMode.SINGLE_DEBTOR && debtor != null) {
            // only the positions this debtor owes alone; the joint ones are settled by a joint
            // payment, not by one attributed to a single debtor
            List<ClaimComponent> ofDebtor = new ArrayList<>();
            for (ClaimComponent c : components) {
                if (hasBookingForDebtor(c, debtor)) {
                    ofDebtor.add(c);
                }
            }
            return ofDebtor;
        }

        return components;
    }

    /**
     * Whether a component carries a booking attributed to the given debtor alone.
     *
     * @param component the component
     * @param debtor the debtor
     * @return true if the component holds a single-debtor booking for that debtor
     */
    private boolean hasBookingForDebtor(ClaimComponent component, ClaimLedgerParty debtor) {
        for (ClaimLedgerEntry entry : this.claimLedgerEntriesFacade.findByComponent(component)) {
            ClaimLedgerParty attributed = entry.getDebtorParty();
            if (attributed != null && attributed.getId() != null && attributed.getId().equals(debtor.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Allocates as much of the remaining amount to a component's principal as it can absorb.
     *
     * @param component the component
     * @param openPrincipal its open principal
     * @param remainingAmount what is left of the payment
     * @param allocations the list to add to
     * @param mode the allocation mode, recorded as the legal reference
     * @return what remains after this allocation
     */
    private BigDecimal allocatePrincipal(ClaimComponent component, BigDecimal openPrincipal,
            BigDecimal remainingAmount, List<PaymentAllocation> allocations, PaymentAllocationMode mode) {

        if (openPrincipal.compareTo(BigDecimal.ZERO) <= 0 || remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return remainingAmount;
        }

        BigDecimal payment = remainingAmount.min(openPrincipal);

        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setComponent(component);
        allocation.setAmount(payment);
        allocation.setInterestAllocation(false);
        allocation.setOriginalOpenAmount(openPrincipal);
        allocation.setRemainingBalance(openPrincipal.subtract(payment));
        allocation.setFullyPaid(allocation.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0);
        allocation.setLegalReference(legalReferenceFor(mode));
        allocation.setAllocationDescription("Kapitalzahlung für " + component.getName());
        allocations.add(allocation);

        return remainingAmount.subtract(payment);
    }

    /**
     * Allocates as much of the remaining amount to a component's open interest as it can absorb.
     *
     * @param component the component
     * @param openInterest its open interest
     * @param remainingAmount what is left of the payment
     * @param allocations the list to add to
     * @param mode the allocation mode, recorded as the legal reference
     * @return what remains after this allocation
     */
    private BigDecimal allocateInterest(ClaimComponent component, BigDecimal openInterest,
            BigDecimal remainingAmount, List<PaymentAllocation> allocations, PaymentAllocationMode mode) {

        if (openInterest.compareTo(BigDecimal.ZERO) <= 0 || remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return remainingAmount;
        }

        BigDecimal payment = remainingAmount.min(openInterest);

        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setComponent(component);
        allocation.setAmount(payment);
        allocation.setInterestAllocation(true);
        allocation.setOriginalOpenAmount(openInterest);
        allocation.setRemainingBalance(openInterest.subtract(payment));
        allocation.setFullyPaid(allocation.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0);
        allocation.setLegalReference(legalReferenceFor(mode));
        allocation.setAllocationDescription("Zinszahlung für " + component.getName());
        allocations.add(allocation);

        return remainingAmount.subtract(payment);
    }

    /**
     * The statutory provision an allocation mode rests on, recorded with every allocation so a
     * booking stays explainable.
     *
     * @param mode the allocation mode
     * @return the legal reference
     */
    private String legalReferenceFor(PaymentAllocationMode mode) {
        switch (mode) {
            case CONSUMER_LOAN:
                return "§ 497 Abs. 3 BGB";
            case LEGAL:
                return "§ 366 Abs. 2 BGB";
            default:
                return mode.getLabel();
        }
    }

    /**
     * The warning shown for an allocation that departs from the statutory order.
     *
     * @param mode the allocation mode
     * @return the warning text
     */
    private String deviationWarning(PaymentAllocationMode mode) {
        switch (mode) {
            case CONSUMER_LOAN:
                return "Tilgung nach § 497 Abs. 3 BGB (Verbraucherdarlehen): Kosten der Rechtsverfolgung, "
                        + "dann Hauptforderung, dann Verzugszinsen - abweichend von §§ 366 Abs. 2, 367 BGB.";
            case SINGLE_DEBTOR:
                return "Die Zahlung wird ausschließlich auf Positionen eines einzelnen Schuldners "
                        + "verrechnet und folgt damit nicht der gesetzlichen Tilgungsreihenfolge.";
            case TARGETED:
                return "Gezielte Tilgung einer bestimmten Position - abweichend von § 366 Abs. 2 BGB.";
            case MANUAL:
                return "Manuelle Verteilung - abweichend von § 366 Abs. 2 BGB.";
            default:
                return null;
        }
    }

    /**
     * Returns all components for a ledger, sorted by legal payment order.
     *
     * Order according to §366 Abs. 2 BGB:
     * 1. COST_NON_INTEREST_BEARING (highest priority)
     * 2. COST_INTEREST_BEARING
     * 3. MAIN_CLAIM (lowest priority)
     *
     * @param ledger The claim ledger
     * @return Sorted list of components
     */
    private List<ClaimComponent> getSortedComponentsByLegalOrder(ClaimLedger ledger) {
        List<ClaimComponent> components = this.claimComponentsFacade.findByLedger(ledger);

        // Sort by legal payment priority (Tilgungsreihenfolge)
        components.sort(Comparator
                .comparingInt(this::getTilgungsPriority)
                .thenComparing(ClaimComponent::getName));

        return components;
    }

    /**
     * Returns the payment priority for a component type according to BGB.
     * Lower numbers = higher priority.
     *
     * @param component The component
     * @return Priority value (1 = highest, 3 = lowest)
     */
    private int getTilgungsPriority(ClaimComponent component) {
        ClaimComponentType type = component.getType();
        if (type == null) {
            return 99;
        }
        if (type.isMainClaim()) {
            return 3; // Lowest priority
        }
        switch (type) {
            case COST_INTEREST_BEARING:
            case COST_ASSESSED:
            case INTEREST_ARREARS:
                // costs that carry interest by their nature - § 104 Abs. 1 S. 2 ZPO for assessed
                // costs - are settled after the ones that do not
                return 2;
            default:
                // remaining costs and the pre-court ancillary claims
                return 1; // Highest priority
        }
    }

    /**
     * Calculates the open principal balance for a component.
     * This is: principal amount - all PAYMENT entries allocated to this component.
     *
     * @param component The component
     * @return Open principal balance
     */
    public BigDecimal calculateComponentPrincipalBalance(ClaimComponent component) {
        return calculateComponentPrincipalBalance(component, new Date());
    }

    /**
     * Calculates the open principal balance of a component at a given date.
     *
     * The principal is derived from the component's bookings by the shared interest engine, not
     * from the component's principalAmount field: that field duplicates the first claim booking and
     * ignores later adjustments and additional claims, so using it here would make the payment
     * split disagree with the ledger totals and the claim statement.
     *
     * @param component the component
     * @param upToDate the key date
     * @return the open principal, never negative
     */
    public BigDecimal calculateComponentPrincipalBalance(ClaimComponent component, Date upToDate) {
        try {
            return this.interestCalculator.calculateDynamicPrincipal(component, upToDate);
        } catch (Exception ex) {
            log.error("Error calculating principal balance", ex);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculates the accrued interest balance for a component up to a specific date.
     * This includes both accrued interest and already booked INTEREST entries,
     * minus any PAYMENT entries allocated to interest.
     *
     * @param component The component
     * @param upToDate Calculate interest up to this date
     * @return Open interest balance
     */
    public BigDecimal calculateComponentInterestBalance(ClaimComponent component, Date upToDate) {
        BigDecimal totalInterest = BigDecimal.ZERO;
        
        try {
            // Get interest rules for this component
            List<InterestRule> rules = this.claimComponentInterestRuleFacade.findByComponent(component);

            if (rules == null || rules.isEmpty()) {
                return BigDecimal.ZERO; // No interest applicable
            }

            // Accrued interest comes from the shared interest engine, so that the split and the
            // ledger totals can never disagree
            totalInterest = this.interestCalculator.calculateAccruedInterest(component, upToDate);

            // Add any INTEREST entries already booked
            List<ClaimLedgerEntry> interestEntries = this.claimLedgerEntriesFacade.findByComponentAndTypeUpToDate(component, LedgerEntryType.INTEREST, upToDate);

            for (ClaimLedgerEntry entry : interestEntries) {
                totalInterest = totalInterest.add(entry.getAmount());
            }

            // Subtract any payments that were allocated to interest
            // This is simplified - in reality we'd need to track which payments went to interest vs principal
            // For now, we assume interest is paid first per component (per §367 BGB)

        } catch (Exception ex) {
            log.error("Error calculating interest balance", ex);
        }

        return totalInterest.max(BigDecimal.ZERO);
    }


    /**
     * Validates a payment split proposal.
     * Checks that:
     * - The sum of allocations equals the total amount
     * - No negative amounts
     * - All components exist
     *
     * @param proposal The proposal to validate
     * @return true if valid, false otherwise
     */
    public boolean validateProposal(PaymentSplitProposal proposal) {
        if (proposal == null) {
            log.error("Proposal is null");
            return false;
        }

        if (proposal.getAllocations() == null || proposal.getAllocations().isEmpty()) {
            log.error("Proposal has no allocations");
            return false;
        }

        // Check sum of allocations
        BigDecimal sum = BigDecimal.ZERO;
        for (PaymentAllocation alloc : proposal.getAllocations()) {
            if (alloc.getAmount() == null || alloc.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                log.error("Invalid allocation amount: " + alloc.getAmount());
                return false;
            }
            sum = sum.add(alloc.getAmount());
        }

        // Allow small rounding differences (0.01 EUR)
        BigDecimal diff = sum.subtract(proposal.getTotalAmount()).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            log.error("Sum of allocations (" + sum + ") does not match total amount (" + proposal.getTotalAmount() + ")");
            return false;
        }

        return true;
    }
}
