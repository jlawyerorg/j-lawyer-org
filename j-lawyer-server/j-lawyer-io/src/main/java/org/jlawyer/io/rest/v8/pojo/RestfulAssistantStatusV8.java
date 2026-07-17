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

import java.util.ArrayList;
import java.util.List;

/**
 * The live connection status of one AI assistant ("Ingo") server — the web equivalent of the
 * desktop's implicit connectivity check: whether the server is reachable, the authenticated user
 * label and the models it offers. Populated by connecting to the configured URL, so it reflects the
 * real state (a per-server {@code error} is set instead of failing the whole request).
 */
public class RestfulAssistantStatusV8 {

    private String configId;
    private String name;
    private boolean reachable;
    /** The authenticated user's label reported by the server (when reachable). */
    private String userLabel;
    /** The available token budget reported by the server (-1 when unknown). */
    private int tokens = -1;
    private int tokensPerDay = -1;
    private int tokensPerMonth = -1;
    /** The models the server offers (with the request types each supports). */
    private List<RestfulAssistantModelV8> models = new ArrayList<>();
    /** A per-server error message when it could not be reached. */
    private String error;

    public RestfulAssistantStatusV8() {
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public String getUserLabel() {
        return userLabel;
    }

    public void setUserLabel(String userLabel) {
        this.userLabel = userLabel;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public int getTokensPerDay() {
        return tokensPerDay;
    }

    public void setTokensPerDay(int tokensPerDay) {
        this.tokensPerDay = tokensPerDay;
    }

    public int getTokensPerMonth() {
        return tokensPerMonth;
    }

    public void setTokensPerMonth(int tokensPerMonth) {
        this.tokensPerMonth = tokensPerMonth;
    }

    public List<RestfulAssistantModelV8> getModels() {
        return models;
    }

    public void setModels(List<RestfulAssistantModelV8> models) {
        this.models = models;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
