package com.jdimension.jlawyer.client.print;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import javax.print.PrintService;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class PrinterFavoritesTest {

    @Test
    public void testFavoriteSerializationPreservesNamesAndLabels() {
        List<PrinterFavorite> input = Arrays.asList(
                new PrinterFavorite("Office Printer: West", "Empfang"),
                new PrinterFavorite("Fax Gerät ##### 2", "Faxdrucker"));

        List<PrinterFavorite> decoded = PrinterFavorites.decode(PrinterFavorites.encode(input));

        assertEquals(input, decoded);
    }

    @Test
    public void testAvailableFavoritesExcludeDefaultUnavailableAndDuplicates() {
        PrinterSnapshot snapshot = new PrinterSnapshot(
                Arrays.asList("Default", "Office", "Fax"), "Default", 1L);
        List<PrinterFavorite> favorites = Arrays.asList(
                new PrinterFavorite("Default", "Standard"),
                new PrinterFavorite("Fax", "Faxdrucker"),
                new PrinterFavorite("Missing", "Unterwegs"),
                new PrinterFavorite("Fax", "Doppelt"),
                new PrinterFavorite("Office", "Büro"));

        List<PrinterFavorite> available = PrinterFavorites.availableNonDefaultFavorites(snapshot, favorites);

        assertEquals(2, available.size());
        assertEquals("Office", available.get(0).getPrinterName());
        assertEquals("Büro", available.get(0).getMenuLabel());
        assertEquals("Fax", available.get(1).getPrinterName());
        assertEquals("Faxdrucker", available.get(1).getMenuLabel());
    }

    @Test
    public void testSnapshotNormalizesPrinterNamesWithoutChangingThem() {
        PrintService alpha = printService("alpha");
        PrintService beta = printService("Beta Printer");
        PrinterSnapshot snapshot = PrinterServiceRegistry.createSnapshot(
                new PrintService[]{alpha, null, beta, alpha, printService(" ")}, beta, 42L);

        assertArrayEquals(new String[]{"alpha", "Beta Printer"},
                snapshot.getPrinterNames().toArray(new String[0]));
        assertEquals("Beta Printer", snapshot.getDefaultPrinterName());
        assertEquals(42L, snapshot.getCompletedAtMillis());
    }

    @Test
    public void testNamedPrinterResolutionIsExactAndHasNoFallback() {
        PrintService office = printService("Office Printer");
        PrintService[] services = new PrintService[]{office, printService("Fax")};

        assertSame(office, PrinterServiceRegistry.findByName(services, "Office Printer"));
        assertNull(PrinterServiceRegistry.findByName(services, "office printer"));
        assertNull(PrinterServiceRegistry.findByName(services, "Missing"));
    }

    private static PrintService printService(String name) {
        return (PrintService) Proxy.newProxyInstance(
                PrinterFavoritesTest.class.getClassLoader(),
                new Class<?>[]{PrintService.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return name;
                    }
                    if ("toString".equals(method.getName())) {
                        return name;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class)) {
                        return false;
                    }
                    if (returnType.equals(int.class)) {
                        return 0;
                    }
                    return null;
                });
    }
}
