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

import com.jdimension.jlawyer.ai.AiModel;
import com.jdimension.jlawyer.ai.AiUser;
import com.jdimension.jlawyer.persistence.AssistantConfig;
import com.jdimension.jlawyer.persistence.AssistantPrompt;
import com.jdimension.jlawyer.persistence.AssistantReplacement;
import com.jdimension.jlawyer.security.CachingCrypto;
import com.jdimension.jlawyer.security.CryptoProvider;
import com.jdimension.jlawyer.services.IntegrationServiceLocal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulAssistantConfigV8;
import org.jlawyer.io.rest.v8.pojo.RestfulAssistantPromptV8;
import org.jlawyer.io.rest.v8.pojo.RestfulAssistantReplacementV8;
import org.jlawyer.io.rest.v8.pojo.RestfulAssistantStatusV8;

/**
 * Configuration of the AI assistant ("Ingo") — the REST equivalent of the desktop "Assistent Ingo"
 * menu: the {@code Ingo-Server} connections, the custom {@code Prompts} and the automatic text
 * {@code Replacements}. Backed by {@code IntegrationService}. The role split mirrors the desktop:
 * server-connection CRUD requires {@code adminRole}; prompts and replacements can be created and
 * updated with {@code loginRole} but deleted only with {@code adminRole}; the live status lookup
 * needs {@code loginRole}. The server-connection password is write-only (never returned; applied
 * only when a non-empty value is sent) and is stored encrypted, exactly as the desktop stores it.
 *
 * <p>The interactive assistant runtime (chat, tool calls, transcription/vision/extraction flows)
 * is intentionally out of scope here — this endpoint only manages configuration.</p>
 *
 * @author jens
 */
@Stateless
@Path("/v8/assistant")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Assistant"})
public class AssistantEndpointV8 implements AssistantEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(AssistantEndpointV8.class.getName());
    private static final String LOOKUP_INTEGRATION = "java:global/j-lawyer-server/j-lawyer-server-ejb/IntegrationService!com.jdimension.jlawyer.services.IntegrationServiceLocal";

    // --- Ingo servers (AssistantConfig) ---

    /**
     * Returns all configured AI assistant servers. The password is never included.
     *
     * @response 200 The list of server connections
     */
    @Override
    @GET
    @Path("/configs")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns all AI assistant server connections", response = RestfulAssistantConfigV8.class, responseContainer = "List")
    public Response getConfigs() {
        try {
            IntegrationServiceLocal integration = lookup();
            List<RestfulAssistantConfigV8> result = new ArrayList<>();
            for (AssistantConfig ac : integration.getAllAssistantConfigs()) {
                result.add(RestfulAssistantConfigV8.fromEntity(ac));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not list assistant configs", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Creates a new AI assistant server connection.
     *
     * @param config the connection (name required; password stored encrypted when given)
     * @response 200 The created connection
     */
    @Override
    @PUT
    @Path("/configs")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Creates an AI assistant server connection", response = RestfulAssistantConfigV8.class)
    public Response createConfig(RestfulAssistantConfigV8 config) {
        try {
            if (config == null || isBlank(config.getName())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            IntegrationServiceLocal integration = lookup();
            AssistantConfig ac = new AssistantConfig();
            ac.setName(config.getName());
            ac.setUrl(config.getUrl());
            ac.setUserName(config.getUserName());
            ac.setConnectionTimeout(config.getConnectionTimeout());
            ac.setReadTimeout(config.getReadTimeout());
            ac.setConfiguration(config.getConfiguration());
            ac.setPassword(isBlank(config.getPassword()) ? "" : encrypt(config.getPassword()));
            AssistantConfig created = integration.addAssistantConfig(ac);
            return Response.ok(RestfulAssistantConfigV8.fromEntity(created)).build();
        } catch (Exception ex) {
            log.error("can not create assistant config", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Updates an AI assistant server connection. The password is changed only when a non-empty value
     * is sent; otherwise the stored password is kept.
     *
     * @param config the connection (id required)
     * @response 200 The updated connection
     * @response 404 No connection with that id
     */
    @Override
    @POST
    @Path("/configs")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Updates an AI assistant server connection", response = RestfulAssistantConfigV8.class)
    public Response updateConfig(RestfulAssistantConfigV8 config) {
        try {
            if (config == null || isBlank(config.getId())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            IntegrationServiceLocal integration = lookup();
            AssistantConfig existing = findConfig(integration, config.getId());
            if (existing == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            existing.setName(config.getName());
            existing.setUrl(config.getUrl());
            existing.setUserName(config.getUserName());
            existing.setConnectionTimeout(config.getConnectionTimeout());
            existing.setReadTimeout(config.getReadTimeout());
            existing.setConfiguration(config.getConfiguration());
            if (!isBlank(config.getPassword())) {
                existing.setPassword(encrypt(config.getPassword()));
            }
            AssistantConfig updated = integration.updateAssistantConfig(existing);
            return Response.ok(RestfulAssistantConfigV8.fromEntity(updated)).build();
        } catch (Exception ex) {
            log.error("can not update assistant config", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Deletes an AI assistant server connection.
     *
     * @param id the connection id
     * @response 200 Deleted
     * @response 404 No connection with that id
     */
    @Override
    @DELETE
    @Path("/configs/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Deletes an AI assistant server connection")
    public Response deleteConfig(@PathParam("id") String id) {
        try {
            IntegrationServiceLocal integration = lookup();
            AssistantConfig existing = findConfig(integration, id);
            if (existing == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            integration.removeAssistantConfig(existing);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete assistant config", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns the live status of the configured assistant servers: which are reachable, the
     * authenticated user label and the models they offer. Only reachable servers are listed; the
     * client joins the result to the configured servers by {@code configId}.
     *
     * @response 200 The per-server status
     */
    @Override
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the live status of the assistant servers", response = RestfulAssistantStatusV8.class, responseContainer = "List")
    public Response getStatus() {
        try {
            IntegrationServiceLocal integration = lookup();
            Map<AssistantConfig, List<AiModel>> models = integration.getAssistantModels();
            Map<AssistantConfig, AiUser> users = integration.getAssistantUserInformation();

            // union of reachable configs from both maps (AssistantConfig equals/hashCode is id-based)
            List<AssistantConfig> configs = new ArrayList<>(models.keySet());
            for (AssistantConfig c : users.keySet()) {
                if (!configs.contains(c)) {
                    configs.add(c);
                }
            }

            List<RestfulAssistantStatusV8> result = new ArrayList<>();
            for (AssistantConfig c : configs) {
                RestfulAssistantStatusV8 status = new RestfulAssistantStatusV8();
                status.setConfigId(c.getId());
                status.setName(c.getName());
                status.setReachable(true);
                AiUser user = users.get(c);
                if (user != null) {
                    status.setUserLabel(user.getUserName());
                    status.setTokens(user.getTokens());
                    status.setTokensPerDay(user.getTokensPerDay());
                    status.setTokensPerMonth(user.getTokensPerMonth());
                }
                List<AiModel> configModels = models.get(c);
                if (configModels != null) {
                    for (AiModel m : configModels) {
                        status.getModels().add(org.jlawyer.io.rest.v8.pojo.RestfulAssistantModelV8.fromModel(m));
                    }
                }
                result.add(status);
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not get assistant status", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns the request history of one assistant server (most recent requests: time, type, tokens).
     *
     * @param id the server-connection id
     * @response 200 The request log
     * @response 404 No connection with that id
     */
    @Override
    @GET
    @Path("/configs/{id}/log")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the request history of an assistant server", response = org.jlawyer.io.rest.v8.pojo.RestfulAiRequestLogV8.class, responseContainer = "List")
    public Response getConfigLog(@PathParam("id") String id) {
        try {
            if (isBlank(id)) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            IntegrationServiceLocal integration = lookup();
            AssistantConfig ref = new AssistantConfig(id);
            List<org.jlawyer.io.rest.v8.pojo.RestfulAiRequestLogV8> result = new ArrayList<>();
            for (com.jdimension.jlawyer.ai.AiRequestLog l : integration.getAssistantRequestLog(ref)) {
                result.add(org.jlawyer.io.rest.v8.pojo.RestfulAiRequestLogV8.fromLog(l));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not get assistant request log for " + id, ex);
            return Response.serverError().build();
        }
    }

    // --- custom prompts (AssistantPrompt) ---

    /**
     * Returns all custom assistant prompts.
     *
     * @response 200 The list of prompts
     */
    @Override
    @GET
    @Path("/prompts")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns all custom assistant prompts", response = RestfulAssistantPromptV8.class, responseContainer = "List")
    public Response getPrompts() {
        try {
            IntegrationServiceLocal integration = lookup();
            List<RestfulAssistantPromptV8> result = new ArrayList<>();
            for (AssistantPrompt ap : integration.getAllAssistantPrompts()) {
                result.add(RestfulAssistantPromptV8.fromEntity(ap));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not list assistant prompts", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Creates a custom assistant prompt.
     *
     * @param prompt the prompt (name required)
     * @response 200 The created prompt
     */
    @Override
    @PUT
    @Path("/prompts")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Creates a custom assistant prompt", response = RestfulAssistantPromptV8.class)
    public Response createPrompt(RestfulAssistantPromptV8 prompt) {
        try {
            if (prompt == null || isBlank(prompt.getName())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            IntegrationServiceLocal integration = lookup();
            AssistantPrompt ap = new AssistantPrompt();
            prompt.applyTo(ap);
            AssistantPrompt created = integration.addAssistantPrompt(ap);
            return Response.ok(RestfulAssistantPromptV8.fromEntity(created)).build();
        } catch (Exception ex) {
            log.error("can not create assistant prompt", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Updates a custom assistant prompt.
     *
     * @param prompt the prompt (id required)
     * @response 200 The updated prompt
     * @response 404 No prompt with that id
     */
    @Override
    @POST
    @Path("/prompts")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Updates a custom assistant prompt", response = RestfulAssistantPromptV8.class)
    public Response updatePrompt(RestfulAssistantPromptV8 prompt) {
        try {
            if (prompt == null || isBlank(prompt.getId())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            IntegrationServiceLocal integration = lookup();
            AssistantPrompt existing = null;
            for (AssistantPrompt ap : integration.getAllAssistantPrompts()) {
                if (prompt.getId().equals(ap.getId())) {
                    existing = ap;
                    break;
                }
            }
            if (existing == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            prompt.applyTo(existing);
            AssistantPrompt updated = integration.updateAssistantPrompt(existing);
            return Response.ok(RestfulAssistantPromptV8.fromEntity(updated)).build();
        } catch (Exception ex) {
            log.error("can not update assistant prompt", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Deletes a custom assistant prompt.
     *
     * @param id the prompt id
     * @response 200 Deleted
     * @response 404 No prompt with that id
     */
    @Override
    @DELETE
    @Path("/prompts/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Deletes a custom assistant prompt")
    public Response deletePrompt(@PathParam("id") String id) {
        try {
            IntegrationServiceLocal integration = lookup();
            AssistantPrompt existing = null;
            for (AssistantPrompt ap : integration.getAllAssistantPrompts()) {
                if (id.equals(ap.getId())) {
                    existing = ap;
                    break;
                }
            }
            if (existing == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            integration.removeAssistantPrompt(existing);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete assistant prompt", ex);
            return Response.serverError().build();
        }
    }

    // --- automatic replacements (AssistantReplacement) ---

    /**
     * Returns all automatic transcription replacements.
     *
     * @response 200 The list of replacements
     */
    @Override
    @GET
    @Path("/replacements")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns all automatic replacements", response = RestfulAssistantReplacementV8.class, responseContainer = "List")
    public Response getReplacements() {
        try {
            IntegrationServiceLocal integration = lookup();
            List<RestfulAssistantReplacementV8> result = new ArrayList<>();
            for (AssistantReplacement r : integration.getAllAssistantReplacements()) {
                result.add(RestfulAssistantReplacementV8.fromEntity(r));
            }
            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not list assistant replacements", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Creates an automatic replacement.
     *
     * @param replacement the replacement (search string required)
     * @response 200 The created replacement
     */
    @Override
    @PUT
    @Path("/replacements")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Creates an automatic replacement", response = RestfulAssistantReplacementV8.class)
    public Response createReplacement(RestfulAssistantReplacementV8 replacement) {
        try {
            if (replacement == null || isBlank(replacement.getSearchString())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            IntegrationServiceLocal integration = lookup();
            AssistantReplacement r = new AssistantReplacement();
            replacement.applyTo(r);
            AssistantReplacement created = integration.addAssistantReplacement(r);
            return Response.ok(RestfulAssistantReplacementV8.fromEntity(created)).build();
        } catch (Exception ex) {
            log.error("can not create assistant replacement", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Updates an automatic replacement.
     *
     * @param replacement the replacement (id required)
     * @response 200 The updated replacement
     * @response 404 No replacement with that id
     */
    @Override
    @POST
    @Path("/replacements")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Updates an automatic replacement", response = RestfulAssistantReplacementV8.class)
    public Response updateReplacement(RestfulAssistantReplacementV8 replacement) {
        try {
            if (replacement == null || isBlank(replacement.getId())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            IntegrationServiceLocal integration = lookup();
            AssistantReplacement existing = null;
            for (AssistantReplacement r : integration.getAllAssistantReplacements()) {
                if (replacement.getId().equals(r.getId())) {
                    existing = r;
                    break;
                }
            }
            if (existing == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            replacement.applyTo(existing);
            AssistantReplacement updated = integration.updateAssistantReplacement(existing);
            return Response.ok(RestfulAssistantReplacementV8.fromEntity(updated)).build();
        } catch (Exception ex) {
            log.error("can not update assistant replacement", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Deletes an automatic replacement.
     *
     * @param id the replacement id
     * @response 200 Deleted
     * @response 404 No replacement with that id
     */
    @Override
    @DELETE
    @Path("/replacements/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Deletes an automatic replacement")
    public Response deleteReplacement(@PathParam("id") String id) {
        try {
            IntegrationServiceLocal integration = lookup();
            AssistantReplacement existing = null;
            for (AssistantReplacement r : integration.getAllAssistantReplacements()) {
                if (id.equals(r.getId())) {
                    existing = r;
                    break;
                }
            }
            if (existing == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            integration.removeAssistantReplacement(existing);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete assistant replacement", ex);
            return Response.serverError().build();
        }
    }

    // --- helpers ---

    private IntegrationServiceLocal lookup() throws Exception {
        InitialContext ic = new InitialContext();
        return (IntegrationServiceLocal) ic.lookup(LOOKUP_INTEGRATION);
    }

    private AssistantConfig findConfig(IntegrationServiceLocal integration, String id) throws Exception {
        for (AssistantConfig ac : integration.getAllAssistantConfigs()) {
            if (id.equals(ac.getId())) {
                return ac;
            }
        }
        return null;
    }

    /** Encrypts a clear-text password exactly as the desktop stores it (same CryptoProvider). */
    private String encrypt(String clear) throws Exception {
        CachingCrypto crypto = CryptoProvider.newCrypto();
        return crypto.encrypt(clear);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
