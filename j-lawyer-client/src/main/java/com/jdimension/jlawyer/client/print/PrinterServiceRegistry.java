package com.jdimension.jlawyer.client.print;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.apache.log4j.Logger;

/**
 * Keeps a non-blocking snapshot of the printers reported by the operating system.
 */
public final class PrinterServiceRegistry {

    private static final Logger log = Logger.getLogger(PrinterServiceRegistry.class.getName());
    private static final PrinterServiceRegistry INSTANCE = new PrinterServiceRegistry();

    private final Object refreshLock = new Object();
    private final List<Consumer<PrinterSnapshot>> completionCallbacks = new ArrayList<>();
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Printer-Service-Refresh");
        thread.setDaemon(true);
        return thread;
    });

    private volatile PrinterSnapshot snapshot = PrinterSnapshot.empty();
    private volatile long lastRefreshAttemptMillis;
    private boolean refreshPending;

    private PrinterServiceRegistry() {
    }

    public static PrinterServiceRegistry getInstance() {
        return INSTANCE;
    }

    public PrinterSnapshot getSnapshot() {
        return snapshot;
    }

    public void refreshAsync(Consumer<PrinterSnapshot> completionCallback) {
        boolean scheduleRefresh = false;
        synchronized (refreshLock) {
            if (completionCallback != null) {
                completionCallbacks.add(completionCallback);
            }
            if (!refreshPending) {
                refreshPending = true;
                scheduleRefresh = true;
            }
        }
        if (scheduleRefresh) {
            refreshExecutor.execute(this::refreshSnapshot);
        }
    }

    /**
     * Requests at most one asynchronous refresh during the supplied interval.
     */
    public void refreshIfOlderThan(long minimumAgeMillis) {
        PrinterSnapshot current = snapshot;
        long freshnessReference = Math.max(current.getCompletedAtMillis(), lastRefreshAttemptMillis);
        if (freshnessReference == 0L
                || System.currentTimeMillis() - freshnessReference >= minimumAgeMillis) {
            refreshAsync(null);
        }
    }

    private void refreshSnapshot() {
        lastRefreshAttemptMillis = System.currentTimeMillis();
        long started = System.nanoTime();
        boolean success = false;
        int printerCount = snapshot.getPrinterNames().size();
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            PrinterSnapshot refreshed = createSnapshot(services, defaultService, System.currentTimeMillis());
            snapshot = refreshed;
            printerCount = refreshed.getPrinterNames().size();
            success = true;
        } catch (Throwable ex) {
            log.error("Error refreshing operating-system printer snapshot", ex);
        } finally {
            long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
            log.info("Refreshing the printer snapshot took " + elapsedMillis
                    + " ms (success=" + success + ", printers=" + printerCount + ")");
            notifyCompletionCallbacks();
        }
    }

    private void notifyCompletionCallbacks() {
        List<Consumer<PrinterSnapshot>> callbacks;
        synchronized (refreshLock) {
            callbacks = new ArrayList<>(completionCallbacks);
            completionCallbacks.clear();
            refreshPending = false;
        }
        PrinterSnapshot current = snapshot;
        for (Consumer<PrinterSnapshot> callback : callbacks) {
            try {
                callback.accept(current);
            } catch (Throwable ex) {
                log.error("Error notifying printer snapshot listener", ex);
            }
        }
    }

    static PrinterSnapshot createSnapshot(PrintService[] services, PrintService defaultService,
            long completedAtMillis) {
        Set<String> uniqueNames = new LinkedHashSet<>();
        if (services != null) {
            for (PrintService service : services) {
                if (service != null && service.getName() != null && !service.getName().trim().isEmpty()) {
                    uniqueNames.add(service.getName());
                }
            }
        }
        List<String> names = new ArrayList<>(uniqueNames);
        names.sort(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));
        String defaultName = defaultService == null ? null : defaultService.getName();
        return new PrinterSnapshot(names, defaultName, completedAtMillis);
    }

    public static PrintService resolveCurrentPrinter(String printerName) {
        if (printerName == null || printerName.trim().isEmpty()) {
            return null;
        }
        return findByName(PrintServiceLookup.lookupPrintServices(null, null), printerName);
    }

    static PrintService findByName(PrintService[] services, String printerName) {
        if (services == null || printerName == null) {
            return null;
        }
        for (PrintService service : services) {
            if (service != null && printerName.equals(service.getName())) {
                return service;
            }
        }
        return null;
    }
}
