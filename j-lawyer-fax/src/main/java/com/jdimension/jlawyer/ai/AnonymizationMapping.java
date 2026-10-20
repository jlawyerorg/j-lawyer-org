/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.ai;

import java.io.Serializable;

/**
 * One replacement made by the j-lawyer-ai anonymization: a placeholder such as [PERSON_1] and the
 * original value it stands for.
 *
 * @author jens
 */
public class AnonymizationMapping implements Serializable {

    protected static long serialVersionUID = 1L;

    public static final String TYPE_PERSON = "PERSON";
    public static final String TYPE_ORGANIZATION = "ORGANIZATION";
    public static final String TYPE_ADDRESS = "ADDRESS";
    public static final String TYPE_CITY = "CITY";
    public static final String TYPE_POSTAL_CODE = "POSTAL_CODE";
    public static final String TYPE_DATE_OF_BIRTH = "DATE_OF_BIRTH";
    public static final String TYPE_CASE_NUMBER = "CASE_NUMBER";
    public static final String TYPE_IBAN = "IBAN";
    public static final String TYPE_EMAIL = "EMAIL";
    public static final String TYPE_PHONE = "PHONE";
    public static final String TYPE_OTHER = "OTHER";

    private String placeholder;
    private String originalValue;
    private String entityType;

    public AnonymizationMapping() {
    }

    public AnonymizationMapping(String placeholder, String originalValue, String entityType) {
        this.placeholder = placeholder;
        this.originalValue = originalValue;
        this.entityType = entityType;
    }

    /**
     * @return the placeholder, e.g. [PERSON_1]
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * @param placeholder the placeholder to set
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    /**
     * @return the original value replaced by the placeholder
     */
    public String getOriginalValue() {
        return originalValue;
    }

    /**
     * @param originalValue the originalValue to set
     */
    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    /**
     * @return the entity type, one of the TYPE_* constants
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * @param entityType the entityType to set
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

}
