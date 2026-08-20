/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jdimension.jlawyer.ai;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author jens
 */
public class ConfigurationUtils {

    /**
     * Converts a Java Properties-format string into a list of ConfigurationData
     * objects.
     *
     * @param propsString the properties-format string
     * @return list of ConfigurationData, empty list if input is null or empty
     */
    public static List<ConfigurationData> fromProperties(String propsString) {
        List<ConfigurationData> result = new ArrayList<>();
        if (propsString == null || propsString.isEmpty()) {
            return result;
        }
        Properties props = new Properties();
        try {
            props.load(new StringReader(propsString));
        } catch (IOException ex) {
            return result;
        }
        for (String key : props.stringPropertyNames()) {
            ConfigurationData cd = new ConfigurationData();
            cd.setId(key);
            cd.setValue(props.getProperty(key));
            result.add(cd);
        }
        return result;
    }

    /**
     * Converts a list of ConfigurationData objects into a Java
     * Properties-format string.
     *
     * @param configs the configuration data list
     * @return properties-format string, empty string if input is null or empty
     */
    public static String toProperties(List<ConfigurationData> configs) {
        if (configs == null || configs.isEmpty()) {
            return "";
        }
        Properties props = new Properties();
        for (ConfigurationData cd : configs) {
            if (cd.getId() != null && cd.getValue() != null) {
                props.setProperty(cd.getId(), cd.getValue());
            }
        }
        StringWriter writer = new StringWriter();
        try {
            props.store(writer, null);
        } catch (IOException ex) {
            return "";
        }
        // Remove the comment line with the timestamp that Properties.store() adds
        String result = writer.toString();
        StringBuilder sb = new StringBuilder();
        for (String line : result.split("\\n")) {
            if (!line.startsWith("#")) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
