package com.jdimension.jlawyer.client.print;

import com.jdimension.jlawyer.client.settings.ClientSettings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialization and filtering for device-local printer favourites.
 */
public final class PrinterFavorites {

    private static final String FIELD_SEPARATOR = ":";

    private PrinterFavorites() {
    }

    public static List<PrinterFavorite> load(ClientSettings settings) {
        String[] entries = settings.getConfigurationArray(
                ClientSettings.CONF_DOCUMENTS_PRINTER_FAVORITES, new String[0]);
        return decode(entries);
    }

    public static void save(ClientSettings settings, List<PrinterFavorite> favorites) {
        settings.setConfigurationArray(ClientSettings.CONF_DOCUMENTS_PRINTER_FAVORITES,
                encode(favorites));
    }

    static String[] encode(List<PrinterFavorite> favorites) {
        List<String> entries = new ArrayList<>();
        for (PrinterFavorite favorite : uniqueByPrinterName(favorites).values()) {
            entries.add(encodeField(favorite.getPrinterName()) + FIELD_SEPARATOR
                    + encodeField(favorite.getDisplayLabel()));
        }
        return entries.toArray(new String[0]);
    }

    static List<PrinterFavorite> decode(String[] entries) {
        List<PrinterFavorite> favorites = new ArrayList<>();
        if (entries == null) {
            return favorites;
        }
        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            String[] fields = entry.split(FIELD_SEPARATOR, -1);
            if (fields.length != 2) {
                continue;
            }
            try {
                String printerName = decodeField(fields[0]);
                String displayLabel = decodeField(fields[1]);
                if (!printerName.trim().isEmpty()) {
                    favorites.add(new PrinterFavorite(printerName, displayLabel));
                }
            } catch (IllegalArgumentException ex) {
                // Ignore malformed local preference entries.
            }
        }
        return new ArrayList<>(uniqueByPrinterName(favorites).values());
    }

    public static List<PrinterFavorite> availableNonDefaultFavorites(
            PrinterSnapshot snapshot, List<PrinterFavorite> favorites) {
        List<PrinterFavorite> result = new ArrayList<>();
        if (snapshot == null) {
            return result;
        }
        String defaultPrinter = snapshot.getDefaultPrinterName();
        for (PrinterFavorite favorite : uniqueByPrinterName(favorites).values()) {
            if (snapshot.contains(favorite.getPrinterName())
                    && !favorite.getPrinterName().equals(defaultPrinter)) {
                result.add(favorite);
            }
        }
        result.sort(Comparator.comparing(PrinterFavorite::getMenuLabel, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PrinterFavorite::getPrinterName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PrinterFavorite::getPrinterName));
        return result;
    }

    private static Map<String, PrinterFavorite> uniqueByPrinterName(List<PrinterFavorite> favorites) {
        Map<String, PrinterFavorite> unique = new LinkedHashMap<>();
        if (favorites != null) {
            for (PrinterFavorite favorite : favorites) {
                if (favorite != null) {
                    unique.putIfAbsent(favorite.getPrinterName(), favorite);
                }
            }
        }
        return unique;
    }

    private static String encodeField(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
