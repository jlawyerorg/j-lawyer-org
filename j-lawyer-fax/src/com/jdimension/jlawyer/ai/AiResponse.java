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
public class AiResponse implements Serializable {
    
    private String prompt;
    private List<OutputData> outputData=new ArrayList<>();
    private String requestType;
    private String modelType;
    
    private String requestId;
    private String status;
    private String statusMessage;
    
    private float progress=0f;
    private long executionMillis=0;

    /**
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * @param prompt the prompt to set
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * @return the outputData
     */
    public List<OutputData> getOutputData() {
        return outputData;
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
     * @return the requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * @param requestId the requestId to set
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the statusMessage
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * @param statusMessage the statusMessage to set
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * @return the progress
     */
    public float getProgress() {
        return progress;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(float progress) {
        this.progress = progress;
    }

    /**
     * @return the executionMillis
     */
    public long getExecutionMillis() {
        return executionMillis;
    }

    /**
     * @param executionMillis the executionMillis to set
     */
    public void setExecutionMillis(long executionMillis) {
        this.executionMillis = executionMillis;
    }

    
    
}
