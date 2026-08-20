package org.jlawyer.io.rest.v8.pojo;

public class RestfulBeaEebRejectionV8 {

    private String senderSafeId;
    private String recipientSafeId;
    private String code;
    private String comment;

    public String getSenderSafeId() {
        return senderSafeId;
    }

    public void setSenderSafeId(String senderSafeId) {
        this.senderSafeId = senderSafeId;
    }

    public String getRecipientSafeId() {
        return recipientSafeId;
    }

    public void setRecipientSafeId(String recipientSafeId) {
        this.recipientSafeId = recipientSafeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
