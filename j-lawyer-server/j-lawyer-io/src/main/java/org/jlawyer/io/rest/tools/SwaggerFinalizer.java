/*
 * Copyright (C) 2025, j-lawyer.org
 *
 * Licensed under the GNU Affero General Public License (see the project LICENSE file).
 */
package org.jlawyer.io.rest.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;

/**
 * Applies the cross-cutting envelope that a JAX-RS scanner cannot infer to the
 * swagger-core generated specification:
 * <ul>
 *   <li>the HTTP Basic securityDefinition (with description) and the global security requirement</li>
 *   <li>the uniform 401/403/500 responses every secured endpoint may return, each documenting the
 *       {@code RestError} body ({@code status}/{@code error}/{@code message}) that the REST layer
 *       actually returns on failure (see {@code RestErrorResponses})</li>
 * </ul>
 * Everything else (paths, parameters, request bodies, models, 200 responses, operationIds and the
 * endpoint-specific 400/404 via {@code @ApiResponses}) comes from the annotations on the resource
 * classes. This is the Java replacement for the former {@code scripts/finalize-swagger.py}.
 *
 * <p>Usage: {@code SwaggerFinalizer <generated.json> <output.json>}</p>
 */
public final class SwaggerFinalizer {

    /** Name of the shared error-body model added to {@code definitions}. */
    private static final String ERROR_MODEL = "RestError";

    private SwaggerFinalizer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: SwaggerFinalizer <generated.json> <output.json>");
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = (ObjectNode) mapper.readTree(new File(args[0]));

        ObjectNode basic = mapper.createObjectNode();
        basic.put("type", "basic");
        basic.put("description", "HTTP Basic Authentication. Works over HTTP and HTTPS");
        ObjectNode securityDefinitions = mapper.createObjectNode();
        securityDefinitions.set("basicAuth", basic);
        root.set("securityDefinitions", securityDefinitions);

        ObjectNode securityRequirement = mapper.createObjectNode();
        securityRequirement.set("basicAuth", mapper.createArrayNode());
        ArrayNode security = mapper.createArrayNode();
        security.add(securityRequirement);
        root.set("security", security);

        // the uniform error body returned on failure (see RestErrorResponses)
        ObjectNode definitions = root.has("definitions")
                ? (ObjectNode) root.get("definitions") : mapper.createObjectNode();
        if (!definitions.has(ERROR_MODEL)) {
            ObjectNode error = mapper.createObjectNode();
            error.put("type", "object");
            ObjectNode props = mapper.createObjectNode();
            ObjectNode status = mapper.createObjectNode();
            status.put("type", "integer");
            status.put("format", "int32");
            props.set("status", status);
            props.set("error", stringProp(mapper));
            props.set("message", stringProp(mapper));
            error.set("properties", props);
            definitions.set(ERROR_MODEL, error);
        }
        root.set("definitions", definitions);

        JsonNode paths = root.get("paths");
        if (paths != null) {
            for (JsonNode pathItem : paths) {
                for (JsonNode operation : pathItem) {
                    ObjectNode op = (ObjectNode) operation;
                    ObjectNode responses = op.has("responses")
                            ? (ObjectNode) op.get("responses") : mapper.createObjectNode();
                    addEnvelope(mapper, responses, "401", "User not authorized");
                    addEnvelope(mapper, responses, "403", "User not authenticated");
                    addEnvelope(mapper, responses, "500", "Internal Server Error");
                    op.set("responses", responses);
                }
            }
        }

        File output = new File(args[1]);
        File outputDir = output.getParentFile();
        if (outputDir != null) {
            outputDir.mkdirs();
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, root);
        System.out.println("finalized -> " + args[1]);
    }

    private static void addEnvelope(ObjectMapper mapper, ObjectNode responses, String code, String description) {
        if (!responses.has(code)) {
            ObjectNode response = mapper.createObjectNode();
            response.put("description", description);
            response.set("headers", mapper.createObjectNode());
            response.set("schema", errorRef(mapper));
            responses.set(code, response);
        } else {
            // document the error body on a response that was declared (e.g. via @ApiResponses) but
            // carries no schema yet — without overwriting an existing one
            JsonNode existing = responses.get(code);
            if (existing != null && existing.isObject() && !existing.has("schema")) {
                ((ObjectNode) existing).set("schema", errorRef(mapper));
            }
        }
    }

    /** A Swagger-2.0 {@code $ref} to the shared {@link #ERROR_MODEL} definition. */
    private static ObjectNode errorRef(ObjectMapper mapper) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("$ref", "#/definitions/" + ERROR_MODEL);
        return schema;
    }

    private static ObjectNode stringProp(ObjectMapper mapper) {
        ObjectNode p = mapper.createObjectNode();
        p.put("type", "string");
        return p;
    }
}
