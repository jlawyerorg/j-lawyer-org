package org.jlawyer.io.rest.v8.pojo;

import com.jdimension.jlawyer.persistence.Timesheet;
import com.jdimension.jlawyer.persistence.TimesheetPosition;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RestfulTimesheetPositionV8 {

    private static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssZ";

    private String id;
    private String name;
    private String description;
    private String principal;
    private String started;
    private String stopped;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal total;
    private String timesheetId;
    private String invoiceId;
    private boolean running;

    public RestfulTimesheetPositionV8() {
    }

    public static RestfulTimesheetPositionV8 fromTimesheetPosition(TimesheetPosition pos) {
        RestfulTimesheetPositionV8 r = new RestfulTimesheetPositionV8();
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601);
        r.setId(pos.getId());
        r.setName(pos.getName());
        r.setDescription(pos.getDescription());
        r.setPrincipal(pos.getPrincipal());
        if (pos.getStarted() != null) {
            r.setStarted(sdf.format(pos.getStarted()));
        }
        if (pos.getStopped() != null) {
            r.setStopped(sdf.format(pos.getStopped()));
        }
        r.setUnitPrice(pos.getUnitPrice());
        r.setTaxRate(pos.getTaxRate());
        r.setTotal(pos.getTotal());
        if (pos.getTimesheet() != null) {
            r.setTimesheetId(pos.getTimesheet().getId());
        }
        if (pos.getInvoice() != null) {
            r.setInvoiceId(pos.getInvoice().getId());
        }
        r.setRunning(pos.isRunning());
        return r;
    }

    public TimesheetPosition toTimesheetPosition(Timesheet ts) {
        TimesheetPosition pos = new TimesheetPosition();
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601);
        pos.setId(this.id);
        pos.setName(this.name);
        pos.setDescription(this.description);
        pos.setPrincipal(this.principal);
        try {
            if (this.started != null) {
                pos.setStarted(sdf.parse(this.started));
            }
        } catch (Exception ex) {
            pos.setStarted(new Date());
        }
        try {
            if (this.stopped != null) {
                pos.setStopped(sdf.parse(this.stopped));
            }
        } catch (Exception ex) {
            pos.setStopped(null);
        }
        if (this.unitPrice != null) {
            pos.setUnitPrice(this.unitPrice);
        }
        if (this.taxRate != null) {
            pos.setTaxRate(this.taxRate);
        }
        if (this.total != null) {
            pos.setTotal(this.total);
        }
        pos.setTimesheet(ts);
        return pos;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getStarted() {
        return started;
    }

    public void setStarted(String started) {
        this.started = started;
    }

    public String getStopped() {
        return stopped;
    }

    public void setStopped(String stopped) {
        this.stopped = stopped;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getTimesheetId() {
        return timesheetId;
    }

    public void setTimesheetId(String timesheetId) {
        this.timesheetId = timesheetId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

}
