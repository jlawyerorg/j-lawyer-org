package com.jdimension.jlawyer.dropscan;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

public class DropscanApiClient {

    private static final Logger log = Logger.getLogger(DropscanApiClient.class.getName());
    private static final String BASE_URL = "https://api.dropscan.de/v1";
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final String bearerToken;

    public DropscanApiClient(String bearerToken) {
        if (bearerToken == null || bearerToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Bearer token must not be null or empty");
        }
        this.bearerToken = bearerToken.trim();
    }

    private HttpURLConnection createConnection(String path, String method) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        return conn;
    }

    private HttpURLConnection createBinaryConnection(String path) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            throw new IOException("Dropscan API returned status " + status + " with no response body");
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Dropscan API returned status " + status + ": " + sb.toString());
        }
        return sb.toString();
    }

    private byte[] readBinaryResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            String error = "";
            if (conn.getErrorStream() != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    error = sb.toString();
                }
            }
            throw new IOException("Dropscan API returned status " + status + ": " + error);
        }
        try (InputStream is = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    private String postJson(String path, String jsonBody) throws IOException {
        HttpURLConnection conn = createConnection(path, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }
        return readResponse(conn);
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // Handle ISO 8601 with timezone
            String normalized = dateStr;
            if (normalized.contains("+") || normalized.endsWith("Z")) {
                int tzIdx = normalized.lastIndexOf('+');
                if (tzIdx < 0) {
                    tzIdx = normalized.lastIndexOf('Z');
                }
                if (tzIdx > 0) {
                    normalized = normalized.substring(0, tzIdx);
                }
            }
            if (normalized.contains(".")) {
                normalized = normalized.substring(0, normalized.indexOf('.'));
            }
            synchronized (ISO_FORMAT) {
                return ISO_FORMAT.parse(normalized);
            }
        } catch (Exception e) {
            log.warn("Failed to parse date: " + dateStr, e);
            return null;
        }
    }

    public List<DropscanScanbox> getScanboxes() throws Exception {
        HttpURLConnection conn = createConnection("/scanboxes", "GET");
        String response = readResponse(conn);
        JsonArray arr = (JsonArray) Jsoner.deserialize(response);
        List<DropscanScanbox> result = new ArrayList<>();
        for (Object obj : arr) {
            JsonObject json = (JsonObject) obj;
            DropscanScanbox box = new DropscanScanbox();
            box.setId(((Number) json.get("id")).intValue());
            box.setNumber((String) json.get("number"));
            Object autoScan = json.get("auto_scan");
            box.setAutoScan(autoScan != null && (Boolean) autoScan);
            Object autoDestroy = json.get("auto_destroy");
            box.setAutoDestroy(autoDestroy != null ? ((Number) autoDestroy).intValue() : null);
            box.setEmailInboxAddress((String) json.get("email_inbox_address"));
            result.add(box);
        }
        return result;
    }

    public List<DropscanMailing> getMailings(String scanboxId, String status, String olderThan) throws Exception {
        StringBuilder path = new StringBuilder("/scanboxes/").append(scanboxId).append("/mailings");
        List<String> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            params.add("current_status=" + status);
        }
        if (olderThan != null && !olderThan.isEmpty()) {
            params.add("older_than=" + olderThan);
        }
        if (!params.isEmpty()) {
            path.append("?");
            path.append(String.join("&", params));
        }
        HttpURLConnection conn = createConnection(path.toString(), "GET");
        String response = readResponse(conn);
        JsonArray arr = (JsonArray) Jsoner.deserialize(response);
        List<DropscanMailing> result = new ArrayList<>();
        for (Object obj : arr) {
            result.add(parseMailing((JsonObject) obj));
        }
        return result;
    }

    public DropscanMailing getMailingDetails(String scanboxId, String uuid) throws Exception {
        HttpURLConnection conn = createConnection("/scanboxes/" + scanboxId + "/mailings/" + uuid, "GET");
        String response = readResponse(conn);
        JsonObject json = (JsonObject) Jsoner.deserialize(response);
        return parseMailing(json);
    }

    private DropscanMailing parseMailing(JsonObject json) {
        DropscanMailing m = new DropscanMailing();
        m.setUuid((String) json.get("uuid"));
        m.setScanboxId(((Number) json.get("scanbox_id")).intValue());
        Object recipientId = json.get("recipient_id");
        if (recipientId != null) {
            m.setRecipientId(((Number) recipientId).intValue());
        }
        m.setStatus((String) json.get("status"));
        m.setReceivedVia((String) json.get("received_via"));
        m.setReceivedAt(parseDate((String) json.get("received_at")));
        m.setUpdatedAt(parseDate((String) json.get("updated_at")));
        m.setScannedAt(parseDate((String) json.get("scanned_at")));
        return m;
    }

    public byte[] getEnvelopeImage(String scanboxId, String uuid) throws Exception {
        HttpURLConnection conn = createBinaryConnection("/scanboxes/" + scanboxId + "/mailings/" + uuid + "/envelope");
        return readBinaryResponse(conn);
    }

    public byte[] getMailingPdf(String scanboxId, String uuid) throws Exception {
        HttpURLConnection conn = createBinaryConnection("/scanboxes/" + scanboxId + "/mailings/" + uuid + "/pdf");
        return readBinaryResponse(conn);
    }

    public String getMailingPlaintext(String scanboxId, String uuid) throws Exception {
        HttpURLConnection conn = createConnection("/scanboxes/" + scanboxId + "/mailings/" + uuid + "/plaintext", "GET");
        conn.setRequestProperty("Accept", "text/plain");
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public byte[] getMailingZip(String scanboxId, String uuid) throws Exception {
        HttpURLConnection conn = createBinaryConnection("/scanboxes/" + scanboxId + "/mailings/" + uuid + "/zip");
        return readBinaryResponse(conn);
    }

    public DropscanActionRequest requestScan(String scanboxId, String uuid) throws Exception {
        String json = "{\"action_type\":\"scan\"}";
        String response = postJson("/scanboxes/" + scanboxId + "/mailings/" + uuid + "/action_requests", json);
        return parseActionRequest((JsonObject) Jsoner.deserialize(response));
    }

    public DropscanActionRequest requestDestroy(String scanboxId, String uuid) throws Exception {
        String json = "{\"action_type\":\"destroy\"}";
        String response = postJson("/scanboxes/" + scanboxId + "/mailings/" + uuid + "/action_requests", json);
        return parseActionRequest((JsonObject) Jsoner.deserialize(response));
    }

    public DropscanActionRequest requestForward(String scanboxId, String uuid, String addressId, String date) throws Exception {
        String json = "{\"action_type\":\"forward\",\"forwarding_options\":{\"address_id\":\"" + addressId + "\",\"date\":\"" + date + "\"}}";
        String response = postJson("/scanboxes/" + scanboxId + "/mailings/" + uuid + "/action_requests", json);
        return parseActionRequest((JsonObject) Jsoner.deserialize(response));
    }

    public void cancelActionRequest(String scanboxId, String uuid, String actionRequestId) throws Exception {
        HttpURLConnection conn = createConnection("/scanboxes/" + scanboxId + "/mailings/" + uuid + "/action_requests/" + actionRequestId, "DELETE");
        int status = conn.getResponseCode();
        if (status != 204 && status < 200 || status >= 300) {
            throw new IOException("Dropscan API returned status " + status + " when canceling action request");
        }
    }

    private DropscanActionRequest parseActionRequest(JsonObject json) {
        DropscanActionRequest ar = new DropscanActionRequest();
        ar.setId(((Number) json.get("id")).intValue());
        ar.setMailingUuid((String) json.get("mailing_uuid"));
        ar.setActionType((String) json.get("action_type"));
        ar.setCreatedAt(parseDate((String) json.get("created_at")));
        return ar;
    }
}
