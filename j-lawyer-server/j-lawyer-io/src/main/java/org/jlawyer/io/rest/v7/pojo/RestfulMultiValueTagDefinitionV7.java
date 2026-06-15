package org.jlawyer.io.rest.v7.pojo;

import java.util.List;

public class RestfulMultiValueTagDefinitionV7 {

    private String tagName;
    private List<String> values;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
