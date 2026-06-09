package org.jlawyer.io.rest.v7.pojo;

import java.util.Date;

/**
 * Request object for duplicating an invoice.
 *
 * @author jens
 */
public class RestfulInvoiceDuplicateRequestV7 {

    private String toCaseId;
    private String invoicePoolId;
    private boolean asCredit = false;
    private boolean markAsCopy = true;
    private Date periodFrom;
    private Date periodTo;
    private Date due;

    public RestfulInvoiceDuplicateRequestV7() {
    }

    /**
     * @return the toCaseId
     */
    public String getToCaseId() {
        return toCaseId;
    }

    /**
     * @param toCaseId the toCaseId to set
     */
    public void setToCaseId(String toCaseId) {
        this.toCaseId = toCaseId;
    }

    /**
     * @return the invoicePoolId
     */
    public String getInvoicePoolId() {
        return invoicePoolId;
    }

    /**
     * @param invoicePoolId the invoicePoolId to set
     */
    public void setInvoicePoolId(String invoicePoolId) {
        this.invoicePoolId = invoicePoolId;
    }

    /**
     * @return the asCredit
     */
    public boolean isAsCredit() {
        return asCredit;
    }

    /**
     * @param asCredit the asCredit to set
     */
    public void setAsCredit(boolean asCredit) {
        this.asCredit = asCredit;
    }

    /**
     * @return the markAsCopy
     */
    public boolean isMarkAsCopy() {
        return markAsCopy;
    }

    /**
     * @param markAsCopy the markAsCopy to set
     */
    public void setMarkAsCopy(boolean markAsCopy) {
        this.markAsCopy = markAsCopy;
    }

    /**
     * @return the periodFrom
     */
    public Date getPeriodFrom() {
        return periodFrom;
    }

    /**
     * @param periodFrom the periodFrom to set
     */
    public void setPeriodFrom(Date periodFrom) {
        this.periodFrom = periodFrom;
    }

    /**
     * @return the periodTo
     */
    public Date getPeriodTo() {
        return periodTo;
    }

    /**
     * @param periodTo the periodTo to set
     */
    public void setPeriodTo(Date periodTo) {
        this.periodTo = periodTo;
    }

    /**
     * @return the due
     */
    public Date getDue() {
        return due;
    }

    /**
     * @param due the due to set
     */
    public void setDue(Date due) {
        this.due = due;
    }
}
