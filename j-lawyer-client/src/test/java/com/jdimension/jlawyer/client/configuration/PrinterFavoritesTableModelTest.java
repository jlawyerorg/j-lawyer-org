package com.jdimension.jlawyer.client.configuration;

import com.jdimension.jlawyer.client.print.PrinterFavorite;
import com.jdimension.jlawyer.client.print.PrinterSnapshot;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PrinterFavoritesTableModelTest {

    @Test
    public void testUnavailableFavoriteCanOnlyBeRemoved() {
        PrinterFavoritesTableModel model = new PrinterFavoritesTableModel(
                snapshot("Office"), Arrays.asList(new PrinterFavorite("Missing", "Unterwegs")));
        int missingRow = findRow(model, "Missing");

        assertFalse(model.isAvailable(missingRow));
        assertTrue(model.isCellEditable(missingRow, 0));
        assertFalse(model.isCellEditable(missingRow, 2));

        model.setValueAt(false, missingRow, 0);
        assertTrue(model.getSelectedFavorites().isEmpty());
        assertTrue(model.isCellEditable(missingRow, 0));
    }

    @Test
    public void testRefreshPreservesUnsavedSelectionAndLabel() {
        PrinterFavoritesTableModel model = new PrinterFavoritesTableModel(
                snapshot("Office"), Collections.emptyList());
        int officeRow = findRow(model, "Office");
        model.setValueAt(true, officeRow, 0);
        model.setValueAt("Büro", officeRow, 2);

        model.updateSnapshot(snapshot("Office", "Fax"));

        assertEquals(1, model.getSelectedFavorites().size());
        assertEquals("Office", model.getSelectedFavorites().get(0).getPrinterName());
        assertEquals("Büro", model.getSelectedFavorites().get(0).getDisplayLabel());
    }

    private static int findRow(PrinterFavoritesTableModel model, String printerName) {
        for (int row = 0; row < model.getRowCount(); row++) {
            if (printerName.equals(model.getValueAt(row, 1))) {
                return row;
            }
        }
        throw new AssertionError("Printer row not found: " + printerName);
    }

    private static PrinterSnapshot snapshot(String... printerNames) {
        return new PrinterSnapshot(Arrays.asList(printerNames),
                printerNames.length == 0 ? null : printerNames[0], 1L);
    }
}
