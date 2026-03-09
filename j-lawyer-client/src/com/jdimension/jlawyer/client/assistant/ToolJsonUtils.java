/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Shared JSON utility methods for AI tool execution.
 *
 * @author jens
 */
public class ToolJsonUtils {

    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private ToolJsonUtils() {
    }

    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String error(String msg) {
        return "{\"error\": \"" + escapeJson(msg) + "\"}";
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        synchronized (ISO_FORMAT) {
            return ISO_FORMAT.format(date);
        }
    }

    public static Date parseIsoDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            synchronized (ISO_FORMAT) {
                return ISO_FORMAT.parse(dateStr.trim());
            }
        } catch (ParseException ex) {
            // try alternate format without time
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr.trim());
            } catch (ParseException ex2) {
                return null;
            }
        }
    }

    public static JLawyerServiceLocator getLocator() throws Exception {
        return JLawyerServiceLocator.getInstance(ClientSettings.getInstance().getLookupProperties());
    }
}
