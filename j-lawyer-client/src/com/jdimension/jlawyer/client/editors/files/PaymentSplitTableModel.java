/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.editors.files;

import com.jdimension.jlawyer.persistence.PaymentAllocation;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * TableModel for displaying payment split allocations in preview mode (read-only).
 *
 * @author jens
 */
public class PaymentSplitTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
        "Nr.",
        "Komponente",
        "Offen",
        "Zahlung",
        "Rest",
        "Status"
    };

    private List<PaymentAllocation> allocations = new ArrayList<>();
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00 €");

    public PaymentSplitTableModel() {
    }

    public PaymentSplitTableModel(List<PaymentAllocation> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
    }

    public void setAllocations(List<PaymentAllocation> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
        fireTableDataChanged();
    }

    public List<PaymentAllocation> getAllocations() {
        return allocations;
    }

    @Override
    public int getRowCount() {
        return allocations.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: // Nr.
                return Integer.class;
            case 5: // Status
                return String.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // Read-only in preview mode
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= allocations.size()) {
            return null;
        }

        PaymentAllocation allocation = allocations.get(rowIndex);

        switch (columnIndex) {
            case 0: // Nr.
                return rowIndex + 1;

            case 1: // Komponente
                if (allocation.getComponent() != null) {
                    String name = allocation.getComponent().getName();
                    if (allocation.isInterestAllocation()) {
                        return "  └─ Zinsen (" + name + ")";
                    } else {
                        return name;
                    }
                }
                return "";

            case 2: // Offen
                return currencyFormat.format(allocation.getOriginalOpenAmount());

            case 3: // Zahlung
                return currencyFormat.format(allocation.getAmount());

            case 4: // Rest
                return currencyFormat.format(allocation.getRemainingBalance());

            case 5: // Status
                if (allocation.isFullyPaid()) {
                    return "✓ getilgt";
                } else {
                    return "teilweise";
                }

            default:
                return null;
        }
    }

    /**
     * Gets the allocation at the specified row.
     *
     * @param row Row index
     * @return PaymentAllocation or null
     */
    public PaymentAllocation getAllocationAt(int row) {
        if (row >= 0 && row < allocations.size()) {
            return allocations.get(row);
        }
        return null;
    }

    /**
     * Gets the total payment amount across all allocations.
     *
     * @return Total payment amount
     */
    public BigDecimal getTotalPaymentAmount() {
        return allocations.stream()
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
