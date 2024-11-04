/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.ai;

import java.io.Serializable;

/**
 *
 * @author jens
 */
public class Message implements Serializable {
    
    protected static long serialVersionUID = 1L;
    
    public static final String ROLE_USER="user";
    public static final String ROLE_ASSISTANT="assistant";
    public static final String ROLE_SYSTEM="system";
    public static final String ROLE_TOOL="tool";
    
    private String content=null;
    private String role=ROLE_USER;

    public Message() {
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * @param role the role to set
     */
    public void setRole(String role) {
        this.role = role;
    }
    
    
    
}
