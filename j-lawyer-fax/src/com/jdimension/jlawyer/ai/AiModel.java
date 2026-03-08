/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jens
 */
public class AiModel implements Serializable {

    protected static long serialVersionUID = 1L;

    private String name;
    private String description;
    private String provider;
    private List<String> supportedRequestTypes = new ArrayList<>();
    private boolean local = false;
    private boolean deductTokens = true;
    private List<Configuration> configurations = new ArrayList<>();

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * @param provider the provider to set
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * @return the supportedRequestTypes
     */
    public List<String> getSupportedRequestTypes() {
        return supportedRequestTypes;
    }

    /**
     * @param supportedRequestTypes the supportedRequestTypes to set
     */
    public void setSupportedRequestTypes(List<String> supportedRequestTypes) {
        this.supportedRequestTypes = supportedRequestTypes;
    }

    /**
     * @return the local
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * @param local the local to set
     */
    public void setLocal(boolean local) {
        this.local = local;
    }

    /**
     * @return the deductTokens
     */
    public boolean isDeductTokens() {
        return deductTokens;
    }

    /**
     * @param deductTokens the deductTokens to set
     */
    public void setDeductTokens(boolean deductTokens) {
        this.deductTokens = deductTokens;
    }

    /**
     * @return the configurations
     */
    public List<Configuration> getConfigurations() {
        return configurations;
    }

    /**
     * @param configurations the configurations to set
     */
    public void setConfigurations(List<Configuration> configurations) {
        this.configurations = configurations;
    }

}
