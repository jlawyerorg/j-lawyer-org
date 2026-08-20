package com.jdimension.jlawyer.ai;

import java.io.Serializable;

/**
 * Represents a tool call requested by the LLM. Contains the tool name and
 * the arguments as a JSON string.
 */
public class ToolCall implements Serializable {

    protected static long serialVersionUID = 1L;

    private String id;
    private String toolName;
    private String arguments;

    public ToolCall() {
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
     * @return the toolName
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * @param toolName the toolName to set
     */
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    /**
     * @return the arguments
     */
    public String getArguments() {
        return arguments;
    }

    /**
     * @param arguments the arguments to set
     */
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
}
