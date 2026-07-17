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
package org.jlawyer.io.rest.v8;

import javax.ejb.Local;
import javax.ws.rs.core.Response;
import org.jlawyer.io.rest.v8.pojo.RestfulAssistantConfigV8;
import org.jlawyer.io.rest.v8.pojo.RestfulAssistantPromptV8;
import org.jlawyer.io.rest.v8.pojo.RestfulAssistantReplacementV8;

@Local
public interface AssistantEndpointLocalV8 {

    Response getConfigs();

    Response createConfig(RestfulAssistantConfigV8 config);

    Response updateConfig(RestfulAssistantConfigV8 config);

    Response deleteConfig(String id);

    Response getStatus();

    Response getConfigLog(String id);

    Response getPrompts();

    Response createPrompt(RestfulAssistantPromptV8 prompt);

    Response updatePrompt(RestfulAssistantPromptV8 prompt);

    Response deletePrompt(String id);

    Response getReplacements();

    Response createReplacement(RestfulAssistantReplacementV8 replacement);

    Response updateReplacement(RestfulAssistantReplacementV8 replacement);

    Response deleteReplacement(String id);

}
