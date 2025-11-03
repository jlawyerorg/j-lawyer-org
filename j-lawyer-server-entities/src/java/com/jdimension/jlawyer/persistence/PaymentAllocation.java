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

/**
 * Represents a single payment allocation to a claim component.
 * Used as part of PaymentSplitProposal to distribute payments across multiple components.
 *
 * This is a Data Transfer Object (DTO) and not a JPA entity.
 *
 * @author jens
 */
public class PaymentAllocation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The claim component to which this allocation applies
     */
    private ClaimComponent component;

    /**
     * The amount allocated to this component
     */
    private BigDecimal amount;

    /**
     * The remaining balance of the component after this allocation
     */
    private BigDecimal remainingBalance;

    /**
     * Indicates if the component is fully paid with this allocation
     */
    private boolean fullyPaid;

    /**
     * Legal reference explaining why this allocation follows this order (e.g., "§ 367 BGB - Zinsen vor Hauptforderung")
     */
    private String legalReference;

    /**
     * Flag indicating if this allocation is for interest (Zinsen) rather than principal
     */
    private boolean isInterestAllocation;

    /**
     * Description of what this allocation covers (e.g., "Zinsen", "Hauptbetrag")
     */
    private String allocationDescription;

    /**
     * Original open amount before this allocation
     */
    private BigDecimal originalOpenAmount;

    public PaymentAllocation() {
        this.amount = BigDecimal.ZERO;
        this.remainingBalance = BigDecimal.ZERO;
        this.originalOpenAmount = BigDecimal.ZERO;
        this.fullyPaid = false;
        this.isInterestAllocation = false;
    }

    public PaymentAllocation(ClaimComponent component, BigDecimal amount) {
        this();
        this.component = component;
        this.amount = amount;
    }

    public ClaimComponent getComponent() {
        return component;
    }

    public void setComponent(ClaimComponent component) {
        this.component = component;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public boolean isFullyPaid() {
        return fullyPaid;
    }

    public void setFullyPaid(boolean fullyPaid) {
        this.fullyPaid = fullyPaid;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public void setLegalReference(String legalReference) {
        this.legalReference = legalReference;
    }

    public boolean isInterestAllocation() {
        return isInterestAllocation;
    }

    public void setInterestAllocation(boolean isInterestAllocation) {
        this.isInterestAllocation = isInterestAllocation;
    }

    public String getAllocationDescription() {
        return allocationDescription;
    }

    public void setAllocationDescription(String allocationDescription) {
        this.allocationDescription = allocationDescription;
    }

    public BigDecimal getOriginalOpenAmount() {
        return originalOpenAmount;
    }

    public void setOriginalOpenAmount(BigDecimal originalOpenAmount) {
        this.originalOpenAmount = originalOpenAmount;
    }

    @Override
    public String toString() {
        return "PaymentAllocation{" +
                "component=" + (component != null ? component.getName() : "null") +
                ", amount=" + amount +
                ", remainingBalance=" + remainingBalance +
                ", fullyPaid=" + fullyPaid +
                ", isInterest=" + isInterestAllocation +
                ", description=" + allocationDescription +
                '}';
    }
}
