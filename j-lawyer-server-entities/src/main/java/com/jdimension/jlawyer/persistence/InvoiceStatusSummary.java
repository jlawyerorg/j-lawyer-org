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
 * Summary of invoices for a given status, containing count and total gross amount.
 * Used to transfer aggregated invoice data without loading full entity objects.
 *
 * @author jens
 */
public class InvoiceStatusSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private int status;
    private long count;
    private BigDecimal totalGross;

    public InvoiceStatusSummary() {
    }

    public InvoiceStatusSummary(int status, long count, BigDecimal totalGross) {
        this.status = status;
        this.count = count;
        this.totalGross = totalGross != null ? totalGross : BigDecimal.ZERO;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(BigDecimal totalGross) {
        this.totalGross = totalGross;
    }
}
