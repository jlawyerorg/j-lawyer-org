package com.jdimension.jlawyer.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines a tool that the LLM can call. Tool definitions are sent by the client
 * and forwarded to j-lawyer-ai in the request.
 */
public class ToolDefinition implements Serializable {

    protected static long serialVersionUID = 1L;

    public static final String RISK_LOW = "niedrig";
    public static final String RISK_MEDIUM = "mittel";
    public static final String RISK_HIGH = "hoch";

    private String id;
    private String description;
    private List<ToolParameter> parameters = new ArrayList<>();
    private String riskLevel = RISK_LOW;

    public ToolDefinition() {
    }

    public ToolDefinition(String id, String description, List<ToolParameter> parameters) {
        this.id = id;
        this.description = description;
        this.parameters = parameters;
    }

    public ToolDefinition(String id, String description, List<ToolParameter> parameters, String riskLevel) {
        this.id = id;
        this.description = description;
        this.parameters = parameters;
        this.riskLevel = riskLevel;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
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
     * @return the parameters
     */
    public List<ToolParameter> getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(List<ToolParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the riskLevel
     */
    public String getRiskLevel() {
        return riskLevel;
    }

    /**
     * @param riskLevel the riskLevel to set
     */
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
