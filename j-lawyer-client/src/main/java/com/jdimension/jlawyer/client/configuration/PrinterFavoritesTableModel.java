package com.jdimension.jlawyer.client.configuration;

import com.jdimension.jlawyer.client.print.PrinterFavorite;
import com.jdimension.jlawyer.client.print.PrinterSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;

/**
 * Editable view of live printers and saved, potentially unavailable favourites.
 */
public final class PrinterFavoritesTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Favorit", "Drucker", "Anzeigename", "Status"};
    private final List<Row> rows = new ArrayList<>();
    private PrinterSnapshot snapshot;

    public PrinterFavoritesTableModel(PrinterSnapshot snapshot, List<PrinterFavorite> savedFavorites) {
        rebuild(snapshot, savedFavorites);
    }

    public void updateSnapshot(PrinterSnapshot updatedSnapshot) {
        rebuild(updatedSnapshot, getSelectedFavorites());
    }

    private void rebuild(PrinterSnapshot updatedSnapshot, List<PrinterFavorite> favorites) {
        this.snapshot = updatedSnapshot == null ? PrinterSnapshot.empty() : updatedSnapshot;
        Map<String, PrinterFavorite> favoriteByName = new LinkedHashMap<>();
        if (favorites != null) {
            for (PrinterFavorite favorite : favorites) {
                if (favorite != null) {
                    favoriteByName.putIfAbsent(favorite.getPrinterName(), favorite);
                }
            }
        }

        Map<String, Row> rowByName = new LinkedHashMap<>();
        for (String printerName : this.snapshot.getPrinterNames()) {
            PrinterFavorite favorite = favoriteByName.get(printerName);
            rowByName.put(printerName, new Row(printerName, true, true, favorite != null,
                    favorite == null ? "" : favorite.getDisplayLabel()));
        }
        for (PrinterFavorite favorite : favoriteByName.values()) {
            rowByName.putIfAbsent(favorite.getPrinterName(), new Row(favorite.getPrinterName(),
                    false, true, true, favorite.getDisplayLabel()));
        }

        rows.clear();
        rows.addAll(rowByName.values());
        rows.sort(Comparator.comparing(Row::getPrinterName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Row::getPrinterName));
        fireTableDataChanged();
    }

    public List<PrinterFavorite> getSelectedFavorites() {
        List<PrinterFavorite> favorites = new ArrayList<>();
        for (Row row : rows) {
            if (row.selected) {
                favorites.add(new PrinterFavorite(row.printerName, row.displayLabel));
            }
        }
        return favorites;
    }

    public boolean isAvailable(int modelRow) {
        return rows.get(modelRow).available;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return row.selected;
            case 1:
                return row.printerName;
            case 2:
                return row.displayLabel;
            case 3:
                if (!row.available) {
                    return "derzeit nicht verfügbar";
                }
                return row.printerName.equals(snapshot.getDefaultPrinterName())
                        ? "verfügbar (Standarddrucker)" : "verfügbar";
            default:
                throw new IllegalArgumentException("Unknown column " + columnIndex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        if (columnIndex == 0) {
            return row.canToggle;
        }
        return columnIndex == 2 && row.available && row.selected;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        if (columnIndex == 0) {
            row.selected = Boolean.TRUE.equals(value);
            fireTableRowsUpdated(rowIndex, rowIndex);
        } else if (columnIndex == 2 && row.available && row.selected) {
            row.displayLabel = value == null ? "" : value.toString().trim();
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    private static final class Row {

        private final String printerName;
        private final boolean available;
        private final boolean canToggle;
        private boolean selected;
        private String displayLabel;

        private Row(String printerName, boolean available, boolean canToggle, boolean selected,
                String displayLabel) {
            this.printerName = printerName;
            this.available = available;
            this.canToggle = canToggle;
            this.selected = selected;
            this.displayLabel = displayLabel;
        }

        private String getPrinterName() {
            return printerName;
        }
    }
}
