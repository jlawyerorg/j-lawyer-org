package org.jlawyer.io.rest.v8.pojo;

public class RestfulBeaEebConfirmationV8 {

    private String senderSafeId;
    private String recipientSafeId;
    private String abgabeDate;

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

    public String getAbgabeDate() {
        return abgabeDate;
    }

    public void setAbgabeDate(String abgabeDate) {
        this.abgabeDate = abgabeDate;
    }

}
