/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.persistence;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a proposal for splitting a payment across multiple claim components.
 * Used when a payment amount exceeds the open balance of a single component.
 *
 * This is a Data Transfer Object (DTO) and not a JPA entity.
 *
 * @author jens
 */
public class PaymentSplitProposal implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The claim ledger to which this payment applies
     */
    private ClaimLedger ledger;

    /**
     * The original component selected by the user
     */
    private ClaimComponent originalComponent;

    /**
     * Total payment amount to be split
     */
    private BigDecimal totalAmount;

    /**
     * Date of the payment
     */
    private Date paymentDate;

    /**
     * Description for the payment entries
     */
    private String description;

    /**
     * Additional comment for the payment entries
     */
    private String comment;

    /**
     * List of individual allocations to components
     */
    private List<PaymentAllocation> allocations;

    /**
     * Remaining surplus after all components are fully paid (should be zero or validation fails)
     */
    private BigDecimal surplus;

    /**
     * Indicates if manual review/adjustment was performed
     */
    private boolean manuallyAdjusted;

    /**
     * Indicates if the proposal follows the legal order (§ 366/367 BGB)
     */
    private boolean followsLegalOrder;

    /**
     * Warning message if the proposal deviates from legal order
     */
    private String legalOrderWarning;

    /**
     * Timestamp when this proposal was created
     */
    private Date createdAt;

    /**
     * User who created this proposal
     */
    private String createdBy;

    public PaymentSplitProposal() {
        this.allocations = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
        this.surplus = BigDecimal.ZERO;
        this.manuallyAdjusted = false;
        this.followsLegalOrder = true;
        this.createdAt = new Date();
    }

    public PaymentSplitProposal(ClaimLedger ledger, BigDecimal totalAmount, Date paymentDate) {
        this();
        this.ledger = ledger;
        this.totalAmount = totalAmount;
        this.paymentDate = paymentDate;
    }

    /**
     * Validates that the sum of all allocations equals the total amount
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        BigDecimal sum = allocations.stream()
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Allow small rounding differences (0.01)
        BigDecimal difference = totalAmount.subtract(sum).abs();
        return difference.compareTo(new BigDecimal("0.01")) <= 0 && surplus.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Gets the number of ledger entries that will be created from this proposal
     * @return number of entries
     */
    public int getNumberOfEntries() {
        return allocations.size();
    }

    /**
     * Adds an allocation to this proposal
     * @param allocation the allocation to add
     */
    public void addAllocation(PaymentAllocation allocation) {
        if (allocation != null) {
            this.allocations.add(allocation);
        }
    }

    /**
     * Removes an allocation from this proposal
     * @param allocation the allocation to remove
     */
    public void removeAllocation(PaymentAllocation allocation) {
        this.allocations.remove(allocation);
    }

    /**
     * Clears all allocations
     */
    public void clearAllocations() {
        this.allocations.clear();
    }

    // Getters and setters

    public ClaimLedger getLedger() {
        return ledger;
    }

    public void setLedger(ClaimLedger ledger) {
        this.ledger = ledger;
    }

    public ClaimComponent getOriginalComponent() {
        return originalComponent;
    }

    public void setOriginalComponent(ClaimComponent originalComponent) {
        this.originalComponent = originalComponent;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<PaymentAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<PaymentAllocation> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
    }

    public BigDecimal getSurplus() {
        return surplus;
    }

    public void setSurplus(BigDecimal surplus) {
        this.surplus = surplus;
    }

    public boolean isManuallyAdjusted() {
        return manuallyAdjusted;
    }

    public void setManuallyAdjusted(boolean manuallyAdjusted) {
        this.manuallyAdjusted = manuallyAdjusted;
    }

    public boolean isFollowsLegalOrder() {
        return followsLegalOrder;
    }

    public void setFollowsLegalOrder(boolean followsLegalOrder) {
        this.followsLegalOrder = followsLegalOrder;
    }

    public String getLegalOrderWarning() {
        return legalOrderWarning;
    }

    public void setLegalOrderWarning(String legalOrderWarning) {
        this.legalOrderWarning = legalOrderWarning;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "PaymentSplitProposal{" +
                "totalAmount=" + totalAmount +
                ", paymentDate=" + paymentDate +
                ", allocations=" + allocations.size() +
                ", surplus=" + surplus +
                ", valid=" + isValid() +
                ", followsLegalOrder=" + followsLegalOrder +
                '}';
    }
}
