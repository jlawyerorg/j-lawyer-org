package com.jdimension.jlawyer.client.print;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of the latest completed operating-system printer discovery.
 */
public final class PrinterSnapshot {

    private static final PrinterSnapshot EMPTY = new PrinterSnapshot(Collections.emptyList(), null, 0L);

    private final List<String> printerNames;
    private final String defaultPrinterName;
    private final long completedAtMillis;

    public PrinterSnapshot(List<String> printerNames, String defaultPrinterName, long completedAtMillis) {
        this.printerNames = Collections.unmodifiableList(new ArrayList<>(printerNames));
        this.defaultPrinterName = defaultPrinterName;
        this.completedAtMillis = completedAtMillis;
    }

    public static PrinterSnapshot empty() {
        return EMPTY;
    }

    public List<String> getPrinterNames() {
        return printerNames;
    }

    public String getDefaultPrinterName() {
        return defaultPrinterName;
    }

    public long getCompletedAtMillis() {
        return completedAtMillis;
    }

    public boolean contains(String printerName) {
        return printerNames.contains(printerName);
    }
}
