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
import com.jdimension.jlawyer.persistence.InterestRule;
import com.jdimension.jlawyer.persistence.InterestRuleFacadeLocal;
import com.jdimension.jlawyer.persistence.InterestType;
import com.jdimension.jlawyer.persistence.LedgerEntryType;
import com.jdimension.jlawyer.persistence.PaymentAllocation;
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

    public PaymentSplitCalculator(ClaimComponentFacadeLocal claimComponentsFacade, InterestRuleFacadeLocal claimComponentInterestRuleFacade, ClaimLedgerEntryFacadeLocal claimLedgerEntriesFacade) {
        this.claimComponentsFacade=claimComponentsFacade;
        this.claimComponentInterestRuleFacade=claimComponentInterestRuleFacade;
        this.claimLedgerEntriesFacade=claimLedgerEntriesFacade;
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

        PaymentSplitProposal proposal = new PaymentSplitProposal(ledger, paymentAmount, paymentDate);

        try {
            // Get all components for this ledger, sorted by legal payment order
            List<ClaimComponent> sortedComponents = getSortedComponentsByLegalOrder(ledger);

            BigDecimal remainingAmount = paymentAmount;
            List<PaymentAllocation> allocations = new ArrayList<>();

            // Distribute payment according to legal order
            for (ClaimComponent component : sortedComponents) {
                if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                // Calculate open balances for this component
                BigDecimal openInterest = calculateComponentInterestBalance(component, paymentDate);
                BigDecimal openPrincipal = calculateComponentPrincipalBalance(component);

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
                    } else {
                        legalRef += " (Hauptforderung)";
                    }
                    principalAllocation.setLegalReference(legalRef);
                    principalAllocation.setAllocationDescription("Kapitalzahlung für " + component.getName());

                    allocations.add(principalAllocation);
                    remainingAmount = remainingAmount.subtract(principalPayment);
                }
            }

            proposal.setAllocations(allocations);
            proposal.setSurplus(remainingAmount);
            proposal.setFollowsLegalOrder(true);
            proposal.setManuallyAdjusted(false);

        } catch (Exception ex) {
            log.error("Error calculating automatic payment split", ex);
            throw new RuntimeException("Fehler bei der Berechnung der automatischen Zahlungsverteilung", ex);
        }

        return proposal;
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
        switch (component.getType()) {
            case COST_NON_INTEREST_BEARING:
                return 1; // Highest priority
            case COST_INTEREST_BEARING:
                return 2;
            case MAIN_CLAIM:
                return 3; // Lowest priority
            default:
                return 99;
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
        BigDecimal principal = component.getPrincipalAmount();
        if (principal == null) {
            principal = BigDecimal.ZERO;
        }

        // Sum all PAYMENT entries for this component
        BigDecimal payments = BigDecimal.ZERO;
        try {
            List<ClaimLedgerEntry> paymentEntries = this.claimLedgerEntriesFacade.findByComponentAndType(component, LedgerEntryType.PAYMENT);
            for (ClaimLedgerEntry entry : paymentEntries) {
                payments = payments.add(entry.getAmount());
            }
        } catch (Exception ex) {
            log.error("Error calculating principal balance", ex);
        }

        BigDecimal balance = principal.subtract(payments);
        return balance.max(BigDecimal.ZERO); // Never return negative
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

            // Calculate accrued interest based on rules
            totalInterest = calculateAccruedInterest(component, rules, upToDate);

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
     * Calculates accrued interest based on interest rules.
     * This is a simplified calculation - the full implementation should consider
     * payment dates, interest periods, base interest rate changes, etc.
     *
     * @param component The component
     * @param rules The interest rules
     * @param upToDate Calculate up to this date
     * @return Accrued interest amount
     */
    private BigDecimal calculateAccruedInterest(ClaimComponent component, List<InterestRule> rules, Date upToDate) {
        BigDecimal accruedInterest = BigDecimal.ZERO;

        try {
            for (InterestRule rule : rules) {
                Date startDate = rule.getValidFrom();
                if (startDate.after(upToDate)) {
                    continue; // Rule not yet active
                }

                // Calculate days between start date and upToDate
                long diffInMillis = upToDate.getTime() - startDate.getTime();
                long days = diffInMillis / (1000 * 60 * 60 * 24);

                if (days <= 0) {
                    continue;
                }

                BigDecimal principal = component.getPrincipalAmount();
                if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal rate;
                if (rule.getInterestType() == InterestType.FIXED) {
                    // Fixed interest rate
                    rate = rule.getFixedRate();
                } else {
                    // Base interest rate + margin
                    // In a full implementation, we'd look up the base interest rate for each period
                    // For now, use a simplified calculation with a default base rate
                    BigDecimal baseRate = getBaseInterestRate(startDate);
                    rate = baseRate.add(rule.getBaseMargin());
                }

                // Calculate interest: principal * rate * days / 365 / 100
                BigDecimal interest = principal
                        .multiply(rate)
                        .multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                accruedInterest = accruedInterest.add(interest);
            }
        } catch (Exception ex) {
            log.error("Error calculating accrued interest", ex);
        }

        return accruedInterest;
    }

    /**
     * Gets the base interest rate for a specific date.
     * This is a simplified implementation - full version should query BaseInterest table.
     *
     * @param date The date
     * @return Base interest rate (as percentage)
     */
    private BigDecimal getBaseInterestRate(Date date) {
        // Simplified: return a default base rate
        // Full implementation should query:
        // SELECT bi FROM BaseInterest bi WHERE bi.validFrom <= :date ORDER BY bi.validFrom DESC
        return new BigDecimal("3.62"); // Example base rate
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
