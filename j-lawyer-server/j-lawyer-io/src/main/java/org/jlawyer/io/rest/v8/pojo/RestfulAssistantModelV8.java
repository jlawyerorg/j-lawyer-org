/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v8.pojo;

import com.jdimension.jlawyer.ai.AiModel;
import java.util.ArrayList;
import java.util.List;

/**
 * A model offered by an AI assistant server, with the request types it supports — so the client can
 * offer a model dropdown filtered to the selected request type (as the desktop prompt editor does).
 */
public class RestfulAssistantModelV8 {

    private String name;
    private String provider;
    private String description;
    /** Whether the model runs locally on the assistant server (vs. an external provider). */
    private boolean local;
    /** Whether using the model deducts from the Ingo token budget ("Ingo-Tokens" vs. "Fremd-Tokens"). */
    private boolean deductTokens;
    /** Whether the model is agent-capable (supports tool calls). */
    private boolean supportsTools;
    private List<String> supportedRequestTypes = new ArrayList<>();
    /** The configurable parameters the model exposes (so the client can offer input fields). */
    private List<RestfulModelParameterV8> configurations = new ArrayList<>();

    public RestfulAssistantModelV8() {
    }

    public static RestfulAssistantModelV8 fromModel(AiModel m) {
        RestfulAssistantModelV8 dto = new RestfulAssistantModelV8();
        dto.name = m.getName();
        dto.provider = m.getProvider();
        dto.description = m.getDescription();
        dto.local = m.isLocal();
        dto.deductTokens = m.isDeductTokens();
        dto.supportsTools = m.isSupportsTools();
        if (m.getSupportedRequestTypes() != null) {
            dto.supportedRequestTypes = new ArrayList<>(m.getSupportedRequestTypes());
        }
        if (m.getConfigurations() != null) {
            for (com.jdimension.jlawyer.ai.Configuration c : m.getConfigurations()) {
                dto.configurations.add(RestfulModelParameterV8.fromConfiguration(c));
            }
        }
        return dto;
    }

    public List<RestfulModelParameterV8> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<RestfulModelParameterV8> configurations) {
        this.configurations = configurations;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean isDeductTokens() {
        return deductTokens;
    }

    public void setDeductTokens(boolean deductTokens) {
        this.deductTokens = deductTokens;
    }

    public boolean isSupportsTools() {
        return supportsTools;
    }

    public void setSupportsTools(boolean supportsTools) {
        this.supportsTools = supportsTools;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<String> getSupportedRequestTypes() {
        return supportedRequestTypes;
    }

    public void setSupportedRequestTypes(List<String> supportedRequestTypes) {
        this.supportedRequestTypes = supportedRequestTypes;
    }

}
