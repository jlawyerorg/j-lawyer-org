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

import com.jdimension.jlawyer.ai.Configuration;

/**
 * One configurable parameter a model exposes (RestfulModelParameterV8): its key ({@code id}) and a
 * human-readable {@code description}. The client renders one input per parameter so the user sees
 * what can be configured; the entered values are stored in the prompt's {@code configuration}.
 */
public class RestfulModelParameterV8 {

    private String id;
    private String description;

    public RestfulModelParameterV8() {
    }

    public static RestfulModelParameterV8 fromConfiguration(Configuration c) {
        RestfulModelParameterV8 dto = new RestfulModelParameterV8();
        dto.id = c.getId();
        dto.description = c.getDescription();
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
