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
public class AiCapability implements Serializable {
    
    protected static long serialVersionUID = 1L;
    
    public static final String REQUESTTYPE_TRANSCRIBE="transcribe";
    public static final String REQUESTTYPE_TRANSLATE="translate";
    public static final String REQUESTTYPE_SUMMARIZE="summarize";
    public static final String REQUESTTYPE_EXPLAIN="explain";
    public static final String REQUESTTYPE_CHAT="chat";
    public static final String REQUESTTYPE_VISION="vision";
    public static final String REQUESTTYPE_GENERATE="generate";
    public static final String REQUESTTYPE_EXTRACT="extract";
    
    public static final String USAGETYPE_INTERACTIVE="interactive";
    public static final String USAGETYPE_AUTOMATED="automated";
    
    public static final String INPUTTYPE_STRING="STRING";
    public static final String INPUTTYPE_FILE="FILE";
    public static final String INPUTTYPE_NONE="NONE";
    
    private String name;
    private String description;
    private String requestType;
    private String usageTypes;
    private String modelType;
    private boolean async=false;
    private boolean customPrompts=false;
    
    private Prompt defaultPrompt=null;
    
    private List<Configuration> configurations=new ArrayList<>();
    
    private List<Parameter> parameters=new ArrayList<>();
    
    private List<Input> input=new ArrayList<>();
    
    private List<Output> output=new ArrayList<>();

    private static final List<String> capabilities=new ArrayList<>();
    
    static {
        capabilities.add(REQUESTTYPE_CHAT);
        capabilities.add(REQUESTTYPE_EXPLAIN);
        capabilities.add(REQUESTTYPE_GENERATE);
        capabilities.add(REQUESTTYPE_SUMMARIZE);
        capabilities.add(REQUESTTYPE_TRANSCRIBE);
        capabilities.add(REQUESTTYPE_TRANSLATE);
        capabilities.add(REQUESTTYPE_VISION);
    }
    
    public static List<String> capabilities() {
        return capabilities;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        AiCapability clone=new AiCapability();
        clone.async=async;
        clone.customPrompts=customPrompts;
        clone.defaultPrompt=defaultPrompt;
        clone.description=description;
        clone.input=input;
        clone.modelType=modelType;
        clone.name=name;
        clone.output=output;
        clone.parameters=parameters;
        clone.requestType=requestType;
        return clone;
    }
    
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
     * @return the requestType
     */
    public String getRequestType() {
        return requestType;
    }

    /**
     * @param requestType the requestType to set
     */
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    /**
     * @return the modelType
     */
    public String getModelType() {
        return modelType;
    }

    /**
     * @param modelType the modelType to set
     */
    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    /**
     * @return the async
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * @param async the async to set
     */
    public void setAsync(boolean async) {
        this.async = async;
    }

    /**
     * @return the defaultPrompt
     */
    public Prompt getDefaultPrompt() {
        return defaultPrompt;
    }

    /**
     * @param defaultPrompt the defaultPrompt to set
     */
    public void setDefaultPrompt(Prompt defaultPrompt) {
        this.defaultPrompt = defaultPrompt;
    }

    /**
     * @return the parameters
     */
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    public boolean hasParameters() {
        return parameters!=null && !parameters.isEmpty();
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the input
     */
    public List<Input> getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(List<Input> input) {
        this.input = input;
    }

    /**
     * @return the output
     */
    public List<Output> getOutput() {
        return output;
    }

    /**
     * @param output the output to set
     */
    public void setOutput(List<Output> output) {
        this.output = output;
    }

    /**
     * @return the customPrompts
     */
    public boolean isCustomPrompts() {
        return customPrompts;
    }

    /**
     * @param customPrompts the customPrompts to set
     */
    public void setCustomPrompts(boolean customPrompts) {
        this.customPrompts = customPrompts;
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

    /**
     * @return the usageTypes
     */
    public String getUsageTypes() {
        return usageTypes;
    }

    /**
     * @param usageTypes the usageTypes to set
     */
    public void setUsageTypes(String usageTypes) {
        this.usageTypes = usageTypes;
    }
    
    
}
