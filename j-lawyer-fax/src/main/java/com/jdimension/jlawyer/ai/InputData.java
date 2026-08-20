/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.ai;

import java.io.Serializable;

public class InputData implements Serializable {
    
    protected static long serialVersionUID = 1L;
    
    public static final String TYPE_STRING="string";
    public static final String TYPE_FILE="file";
    
    private String type;
    private String stringData;
    private byte[] data;
    private String fileName;
    private boolean base64;

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the base64
     */
    public boolean isBase64() {
        return base64;
    }

    /**
     * @param base64 the base64 to set
     */
    public void setBase64(boolean base64) {
        this.base64 = base64;
    }

    /**
     * @return the stringData
     */
    public String getStringData() {
        return stringData;
    }

    /**
     * @param stringData the stringData to set
     */
    public void setStringData(String stringData) {
        this.stringData = stringData;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    
    


}
