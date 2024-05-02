/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.client.assistant;

public class Prompt {
    
    private String defaultPrompt;
    
    private long maxTokens=Long.MAX_VALUE;

    // Getters and setters with XmlElement annotations

    /**
     * @return the defaultPrompt
     */
    public String getDefaultPrompt() {
        return defaultPrompt;
    }

    /**
     * @param defaultPrompt the defaultPrompt to set
     */
    public void setDefaultPrompt(String defaultPrompt) {
        this.defaultPrompt = defaultPrompt;
    }
    
    /**
     * @return the maxTokens
     */
    public long getMaxTokens() {
        return maxTokens;
    }

    /**
     * @param maxTokens the maxTokens to set
     */
    public void setMaxTokens(long maxTokens) {
        this.maxTokens = maxTokens;
    }
    


}
