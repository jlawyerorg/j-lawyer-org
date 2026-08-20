/*
 * Copyright (C) j-lawyer.org
 *
 * Licensed under the GNU Affero General Public License, version 3.
 * See the LICENSE file distributed with this project.
 */
package org.jlawyer.io.rest.v7.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated case suggestions for an opened email: matching cases (each tagged with its
 * {@code source}), the sender's matching contacts, phone numbers extracted from the body, and the
 * parsed sender name/email (so a client can offer to create a contact when none matched).
 */
public class RestfulCaseSuggestionsV7 implements Serializable {

    private List<RestfulSuggestedCaseV7> suggestedCases = new ArrayList<>();
    private List<RestfulSuggestedContactV7> contacts = new ArrayList<>();
    private List<String> phoneNumbers = new ArrayList<>();
    private String senderName;
    private String senderEmail;

    public List<RestfulSuggestedCaseV7> getSuggestedCases() {
        return suggestedCases;
    }

    public void setSuggestedCases(List<RestfulSuggestedCaseV7> suggestedCases) {
        this.suggestedCases = suggestedCases;
    }

    public List<RestfulSuggestedContactV7> getContacts() {
        return contacts;
    }

    public void setContacts(List<RestfulSuggestedContactV7> contacts) {
        this.contacts = contacts;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
}
