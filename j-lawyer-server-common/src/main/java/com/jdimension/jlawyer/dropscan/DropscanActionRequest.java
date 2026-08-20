package com.jdimension.jlawyer.dropscan;

import java.io.Serializable;
import java.util.Date;

public class DropscanActionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ACTION_SCAN = "scan";
    public static final String ACTION_DESTROY = "destroy";
    public static final String ACTION_FORWARD = "forward";

    private int id;
    private String mailingUuid;
    private String actionType;
    private Date createdAt;

    public DropscanActionRequest() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMailingUuid() {
        return mailingUuid;
    }

    public void setMailingUuid(String mailingUuid) {
        this.mailingUuid = mailingUuid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
