package com.jdimension.jlawyer.client.print;

import java.util.Objects;

/**
 * A device-local favourite identified by the exact operating-system printer name.
 */
public final class PrinterFavorite {

    private final String printerName;
    private final String displayLabel;

    public PrinterFavorite(String printerName, String displayLabel) {
        if (printerName == null || printerName.trim().isEmpty()) {
            throw new IllegalArgumentException("printerName must not be blank");
        }
        this.printerName = printerName;
        this.displayLabel = displayLabel == null ? "" : displayLabel.trim();
    }

    public String getPrinterName() {
        return printerName;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getMenuLabel() {
        return displayLabel.isEmpty() ? printerName : displayLabel;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PrinterFavorite)) {
            return false;
        }
        PrinterFavorite that = (PrinterFavorite) other;
        return printerName.equals(that.printerName) && displayLabel.equals(that.displayLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(printerName, displayLabel);
    }
}
