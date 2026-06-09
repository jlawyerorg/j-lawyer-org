package org.jlawyer.io.rest.v8.pojo;

import com.jdimension.jlawyer.persistence.TimesheetPositionTemplate;
import java.math.BigDecimal;

public class RestfulTimesheetPositionTemplateV8 {

    private String id;
    private String name;
    private String description;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;

    public RestfulTimesheetPositionTemplateV8() {
    }

    public static RestfulTimesheetPositionTemplateV8 fromTemplate(TimesheetPositionTemplate tpl) {
        RestfulTimesheetPositionTemplateV8 r = new RestfulTimesheetPositionTemplateV8();
        r.setId(tpl.getId());
        r.setName(tpl.getName());
        r.setDescription(tpl.getDescription());
        r.setUnitPrice(tpl.getUnitPrice());
        r.setTaxRate(tpl.getTaxRate());
        return r;
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

}
