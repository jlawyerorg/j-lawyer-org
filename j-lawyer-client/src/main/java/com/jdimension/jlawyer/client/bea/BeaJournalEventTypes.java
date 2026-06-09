package com.jdimension.jlawyer.client.bea;

import java.util.HashMap;

public class BeaJournalEventTypes {

    private static final HashMap<String, String> DISPLAY_NAMES = new HashMap<>();

    static {
        DISPLAY_NAMES.put("MESSAGE_ZEITPUNKT_EMPFANG", "Zeitpunkt des Empfangs");
        DISPLAY_NAMES.put("MESSAGE_ZEITPUNKT_ERFOLGREICHER_VERSAND", "Zeitpunkt des erfolgreichen Versands");
        DISPLAY_NAMES.put("MESSAGE_OEFFNEN_DER_NACHRICHT", "Nachricht geöffnet");
        DISPLAY_NAMES.put("MESSAGE_ABHOLEN_DER_NACHRICHT_KSW", "Nachricht über Kanzleisoftware abgeholt");
        DISPLAY_NAMES.put("MESSAGE_SIGNED", "Nachricht signiert");
        DISPLAY_NAMES.put("CERTIFICATES_CHECKED", "Zertifikate geprüft");
    }

    public static String getDisplayName(String eventType) {
        if (eventType == null) {
            return "";
        }
        String display = DISPLAY_NAMES.get(eventType);
        if (display != null) {
            return display;
        }
        // case-insensitive lookup
        display = DISPLAY_NAMES.get(eventType.toUpperCase());
        if (display != null) {
            return display;
        }
        // generischer Fallback: MESSAGE_-Prefix entfernen, _ durch Leerzeichen, capitalize
        String result = eventType;
        if (result.startsWith("MESSAGE_")) {
            result = result.substring(8);
        }
        result = result.replace('_', ' ').toLowerCase();
        if (!result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }
        return result;
    }
}
