/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.pojo;

import com.jdimension.jlawyer.persistence.ClaimComponent;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Represents the balance information for a single claim component.
 * Used to display open amounts per component and determine if payment splitting is needed.
 *
 * @author jens
 */
public class ClaimComponentBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The claim component this balance refers to
     */
    private ClaimComponent component;

    /**
     * Principal/main amount of the component
     */
    private BigDecimal principalAmount = BigDecimal.ZERO;

    /**
     * Accumulated interest for this component
     */
    private BigDecimal interestAmount = BigDecimal.ZERO;

    /**
     * Total payments made to this component
     */
    private BigDecimal paymentsAmount = BigDecimal.ZERO;

    /**
     * Open principal balance (principal - payments allocated to principal)
     */
    private BigDecimal openPrincipal = BigDecimal.ZERO;

    /**
     * Open interest balance (interest - payments allocated to interest)
     */
    private BigDecimal openInterest = BigDecimal.ZERO;

    /**
     * Total open balance (openPrincipal + openInterest)
     */
    private BigDecimal totalOpenBalance = BigDecimal.ZERO;

    /**
     * Indicates if this component is fully paid
     */
    private boolean fullyPaid = false;

    public ClaimComponentBalance() {
    }

    public ClaimComponentBalance(ClaimComponent component) {
        this.component = component;
    }

    /**
     * Calculates the total open balance from openPrincipal and openInterest
     */
    public void calculateTotalOpenBalance() {
        this.totalOpenBalance = this.openPrincipal.add(this.openInterest);
        this.fullyPaid = this.totalOpenBalance.compareTo(BigDecimal.ZERO) == 0;
    }

    // Getters and setters

    public ClaimComponent getComponent() {
        return component;
    }

    public void setComponent(ClaimComponent component) {
        this.component = component;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public BigDecimal getPaymentsAmount() {
        return paymentsAmount;
    }

    public void setPaymentsAmount(BigDecimal paymentsAmount) {
        this.paymentsAmount = paymentsAmount;
    }

    public BigDecimal getOpenPrincipal() {
        return openPrincipal;
    }

    public void setOpenPrincipal(BigDecimal openPrincipal) {
        this.openPrincipal = openPrincipal;
    }

    public BigDecimal getOpenInterest() {
        return openInterest;
    }

    public void setOpenInterest(BigDecimal openInterest) {
        this.openInterest = openInterest;
    }

    public BigDecimal getTotalOpenBalance() {
        return totalOpenBalance;
    }

    public void setTotalOpenBalance(BigDecimal totalOpenBalance) {
        this.totalOpenBalance = totalOpenBalance;
    }

    public boolean isFullyPaid() {
        return fullyPaid;
    }

    public void setFullyPaid(boolean fullyPaid) {
        this.fullyPaid = fullyPaid;
    }

    @Override
    public String toString() {
        return "ClaimComponentBalance{" +
                "component=" + (component != null ? component.getName() : "null") +
                ", principalAmount=" + principalAmount +
                ", interestAmount=" + interestAmount +
                ", paymentsAmount=" + paymentsAmount +
                ", openPrincipal=" + openPrincipal +
                ", openInterest=" + openInterest +
                ", totalOpenBalance=" + totalOpenBalance +
                ", fullyPaid=" + fullyPaid +
                '}';
    }
}
