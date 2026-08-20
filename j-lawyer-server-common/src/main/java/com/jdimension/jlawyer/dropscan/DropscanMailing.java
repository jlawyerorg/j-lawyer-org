package com.jdimension.jlawyer.dropscan;

import java.io.Serializable;
import java.util.Date;

public class DropscanMailing implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_RECEIVED = "received";
    public static final String STATUS_SCAN_REQUESTED = "scan_requested";
    public static final String STATUS_SCANNED = "scanned";
    public static final String STATUS_DESTROY_REQUESTED = "destroy_requested";
    public static final String STATUS_DESTROYED = "destroyed";
    public static final String STATUS_FORWARD_REQUESTED = "forward_requested";
    public static final String STATUS_FORWARDED = "forwarded";

    private String uuid;
    private int scanboxId;
    private String scanboxNumber;
    private int recipientId;
    private String recipientName;
    private String status;
    private String receivedVia;
    private Date receivedAt;
    private Date updatedAt;
    private Date scannedAt;

    public DropscanMailing() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getScanboxId() {
        return scanboxId;
    }

    public void setScanboxId(int scanboxId) {
        this.scanboxId = scanboxId;
    }

    public String getScanboxNumber() {
        return scanboxNumber;
    }

    public void setScanboxNumber(String scanboxNumber) {
        this.scanboxNumber = scanboxNumber;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReceivedVia() {
        return receivedVia;
    }

    public void setReceivedVia(String receivedVia) {
        this.receivedVia = receivedVia;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Date scannedAt) {
        this.scannedAt = scannedAt;
    }

    public boolean isScanned() {
        return STATUS_SCANNED.equals(status) || STATUS_DESTROY_REQUESTED.equals(status) || STATUS_FORWARD_REQUESTED.equals(status);
    }

    public static String getStatusDisplayName(String status) {
        if (status == null) return "";
        switch (status) {
            case STATUS_RECEIVED: return "Empfangen";
            case STATUS_SCAN_REQUESTED: return "Scan angefordert";
            case STATUS_SCANNED: return "Gescannt";
            case STATUS_DESTROY_REQUESTED: return "Vernichtung angefordert";
            case STATUS_DESTROYED: return "Vernichtet";
            case STATUS_FORWARD_REQUESTED: return "Weiterleitung angefordert";
            case STATUS_FORWARDED: return "Weitergeleitet";
            default: return status;
        }
    }

    public String getStatusDisplayName() {
        return getStatusDisplayName(this.status);
    }

    public static String getReceivedViaDisplayName(String receivedVia) {
        if (receivedVia == null) return "";
        switch (receivedVia) {
            case "inbox": return "Briefeingang";
            case "forwarding": return "Weiterleitung";
            case "upload": return "Upload";
            default: return receivedVia;
        }
    }

    public String getReceivedViaDisplayName() {
        return getReceivedViaDisplayName(this.receivedVia);
    }
}
