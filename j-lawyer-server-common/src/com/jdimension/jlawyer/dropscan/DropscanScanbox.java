package com.jdimension.jlawyer.dropscan;

import java.io.Serializable;

public class DropscanScanbox implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String number;
    private boolean autoScan;
    private Integer autoDestroy;
    private String emailInboxAddress;

    public DropscanScanbox() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean isAutoScan() {
        return autoScan;
    }

    public void setAutoScan(boolean autoScan) {
        this.autoScan = autoScan;
    }

    public Integer getAutoDestroy() {
        return autoDestroy;
    }

    public void setAutoDestroy(Integer autoDestroy) {
        this.autoDestroy = autoDestroy;
    }

    public String getEmailInboxAddress() {
        return emailInboxAddress;
    }

    public void setEmailInboxAddress(String emailInboxAddress) {
        this.emailInboxAddress = emailInboxAddress;
    }

    @Override
    public String toString() {
        return number + " (ID: " + id + ")";
    }
}
