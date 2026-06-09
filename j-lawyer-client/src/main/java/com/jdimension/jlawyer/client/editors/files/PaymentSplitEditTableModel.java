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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * TableModel for editing payment split allocations (editable amounts).
 *
 * @author jens
 */
public class PaymentSplitEditTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
        "Komponente",
        "Offen",
        "Zahlung"
    };

    private List<PaymentAllocation> allocations = new ArrayList<>();
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
    private BigDecimal totalAvailable = BigDecimal.ZERO;

    public PaymentSplitEditTableModel() {
    }

    public PaymentSplitEditTableModel(List<PaymentAllocation> allocations, BigDecimal totalAvailable) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
        this.totalAvailable = totalAvailable;
    }

    public void setAllocations(List<PaymentAllocation> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
        fireTableDataChanged();
    }

    public List<PaymentAllocation> getAllocations() {
        return allocations;
    }

    public void setTotalAvailable(BigDecimal totalAvailable) {
        this.totalAvailable = totalAvailable;
    }

    public BigDecimal getTotalAvailable() {
        return totalAvailable;
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
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Only "Zahlung" column is editable
        return columnIndex == 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= allocations.size()) {
            return null;
        }

        PaymentAllocation allocation = allocations.get(rowIndex);

        switch (columnIndex) {
            case 0: // Komponente
                if (allocation.getComponent() != null) {
                    return allocation.getComponent().getName();
                }
                return "";

            case 1: // Offen
                return currencyFormat.format(allocation.getOriginalOpenAmount()) + " €";

            case 2: // Zahlung (editable)
                return currencyFormat.format(allocation.getAmount());

            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= allocations.size()) {
            return;
        }

        if (columnIndex == 2) { // Zahlung column
            PaymentAllocation allocation = allocations.get(rowIndex);
            try {
                String valueStr = aValue.toString().replace("€", "").replace(",", ".").trim();
                BigDecimal newAmount = new BigDecimal(valueStr);

                // Don't allow negative amounts
                if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                    newAmount = BigDecimal.ZERO;
                }

                // Don't allow amount greater than open balance
                BigDecimal maxAmount = allocation.getOriginalOpenAmount();
                if (newAmount.compareTo(maxAmount) > 0) {
                    newAmount = maxAmount;
                }

                allocation.setAmount(newAmount);

                // Recalculate remaining balance
                allocation.setRemainingBalance(maxAmount.subtract(newAmount));
                allocation.setFullyPaid(newAmount.compareTo(maxAmount) == 0);

                fireTableCellUpdated(rowIndex, columnIndex);

            } catch (NumberFormatException e) {
                // Invalid input, ignore
            }
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
     * Gets the sum of all allocated amounts.
     *
     * @return Total allocated amount
     */
    public BigDecimal getTotalAllocated() {
        return allocations.stream()
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gets the remaining unallocated amount.
     *
     * @return Remaining amount
     */
    public BigDecimal getRemaining() {
        return totalAvailable.subtract(getTotalAllocated());
    }

    /**
     * Checks if the allocation is valid (total allocated equals total available).
     *
     * @return true if valid
     */
    public boolean isValid() {
        BigDecimal diff = getRemaining().abs();
        return diff.compareTo(new BigDecimal("0.01")) <= 0;
    }
}
