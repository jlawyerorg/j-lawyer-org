/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v7.pojo;

import com.jdimension.jlawyer.persistence.InvoicePositionTemplate;
import java.math.BigDecimal;

/**
 * A reusable invoice line-item template (Belegposition-Vorlage): a name plus default tax rate,
 * quantity and unit price that can be dropped into an invoice.
 */
public class RestfulInvoicePositionTemplateV7 {

    private String id;
    private String name;
    private String description;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private BigDecimal units = BigDecimal.ONE;
    private BigDecimal unitPrice = BigDecimal.ZERO;

    public RestfulInvoicePositionTemplateV7() {
    }

    /**
     * Wraps an entity as a POJO.
     *
     * @param t the entity
     * @return the POJO
     */
    public static RestfulInvoicePositionTemplateV7 fromEntity(InvoicePositionTemplate t) {
        RestfulInvoicePositionTemplateV7 p = new RestfulInvoicePositionTemplateV7();
        p.setId(t.getId());
        p.setName(t.getName());
        p.setDescription(t.getDescription());
        p.setTaxRate(t.getTaxRate());
        p.setUnits(t.getUnits());
        p.setUnitPrice(t.getUnitPrice());
        return p;
    }

    /**
     * Maps this POJO to an entity (for create/update).
     *
     * @return the entity
     */
    public InvoicePositionTemplate toEntity() {
        InvoicePositionTemplate t = new InvoicePositionTemplate();
        t.setId(this.id);
        t.setName(this.name);
        t.setDescription(this.description);
        t.setTaxRate(this.taxRate);
        t.setUnits(this.units);
        t.setUnitPrice(this.unitPrice);
        return t;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getUnits() { return units; }
    public void setUnits(BigDecimal units) { this.units = units; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

}
