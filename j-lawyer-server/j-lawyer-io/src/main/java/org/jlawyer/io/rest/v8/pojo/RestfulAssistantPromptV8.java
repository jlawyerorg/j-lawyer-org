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

import com.jdimension.jlawyer.persistence.AssistantPrompt;

/**
 * A custom AI assistant prompt (RestfulAssistantPromptV8) — the web equivalent of the desktop
 * "eigene Prompts" dialog. Global (not per-user). {@code requestType} is one of the assistant
 * capabilities (transcribe / translate / summarize / explain / chat / vision / generate / extract);
 * {@code configuration} carries opaque per-model parameters (JSON, round-tripped verbatim).
 */
public class RestfulAssistantPromptV8 {

    private String id;
    private String name;
    private String requestType;
    private String prompt;
    private String systemPrompt;
    private String modelRef;
    private String configuration;

    public RestfulAssistantPromptV8() {
    }

    public static RestfulAssistantPromptV8 fromEntity(AssistantPrompt ap) {
        RestfulAssistantPromptV8 dto = new RestfulAssistantPromptV8();
        dto.id = ap.getId();
        dto.name = ap.getName();
        dto.requestType = ap.getRequestType();
        dto.prompt = ap.getPrompt();
        dto.systemPrompt = ap.getSystemPrompt();
        dto.modelRef = ap.getModelRef();
        dto.configuration = ap.getConfiguration();
        return dto;
    }

    /** Applies the DTO onto an entity (id is not overwritten here — the endpoint manages it). */
    public void applyTo(AssistantPrompt ap) {
        ap.setName(this.name);
        ap.setRequestType(this.requestType);
        ap.setPrompt(this.prompt);
        ap.setSystemPrompt(this.systemPrompt);
        ap.setModelRef(this.modelRef);
        ap.setConfiguration(this.configuration);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getModelRef() {
        return modelRef;
    }

    public void setModelRef(String modelRef) {
        this.modelRef = modelRef;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

}
