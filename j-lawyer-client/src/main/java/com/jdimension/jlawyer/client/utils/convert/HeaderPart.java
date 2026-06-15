package com.jdimension.jlawyer.client.utils.convert;

import static com.jdimension.jlawyer.client.utils.convert.Helper.templateHeaderBody;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

class HeaderPart {

    private final String template;
    private final String name;
    private final String data;

    private HeaderPart(Builder builder) {
        this.template = builder.template;
        this.name = builder.name;
        this.data = builder.data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTemplate() {
        return template;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    static class Builder {

        private String template = templateHeaderBody;
        private String name;
        private String data;

        public Builder template(String template) {
            this.template = template;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder data(String data) {
            if (isNotBlank(data)) {
                this.data = data;
            }
            return this;
        }

        public Builder subject(String data) {
            if (isNotBlank(data)) {
                this.data = "<b>" + data + "</b>";
            }
            return this;
        }

        public HeaderPart build() {
            return new HeaderPart(this);
        }

    }
}
