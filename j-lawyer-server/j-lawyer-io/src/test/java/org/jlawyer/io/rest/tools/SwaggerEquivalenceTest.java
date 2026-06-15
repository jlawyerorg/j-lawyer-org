/*
 * Copyright (C) 2025, j-lawyer.org
 *
 * Licensed under the GNU Affero General Public License (see the project LICENSE file).
 */
package org.jlawyer.io.rest.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Semantic-equivalence gate for the j-lawyer-io REST API specification.
 *
 * <p>Asserts that the served {@code swagger-ui/swagger.json} is semantically equivalent to the
 * committed golden baseline ({@code src/test/resources/swagger.golden.json}). The golden is a
 * snapshot of the API contract; it must be re-baselined (via {@code mvn -Pswagger-regen}) whenever
 * the API is changed intentionally. Differences in formatting or key/array ordering are ignored;
 * any structural change to paths, methods, parameters, response schemas, security or tags fails.
 *
 * <p>This is the Java replacement for the former {@code scripts/swagger-equivalence-check.py}.
 */
public class SwaggerEquivalenceTest {

    @Test
    public void servedSpecMatchesGolden() throws Exception {
        File served = new File("src/main/webapp/swagger-ui/swagger.json");
        File golden = new File("src/test/resources/swagger.golden.json");
        if (!served.exists() || !golden.exists()) {
            fail("missing swagger spec(s): served=" + served.exists() + " golden=" + golden.exists());
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode gen = mapper.readTree(served);
        JsonNode gold = mapper.readTree(golden);
        List<String> diffs = new ArrayList<>();

        if (!gold.path("swagger").equals(gen.path("swagger"))) {
            diffs.add("swagger version mismatch");
        }
        if (!gold.path("basePath").equals(gen.path("basePath"))) {
            diffs.add("basePath mismatch");
        }
        if (!gold.path("info").path("version").equals(gen.path("info").path("version"))) {
            diffs.add("info.version mismatch");
        }
        if (!set(gold.get("schemes")).equals(set(gen.get("schemes")))) {
            diffs.add("schemes mismatch");
        }
        if (!gold.path("securityDefinitions").equals(gen.path("securityDefinitions"))) {
            diffs.add("securityDefinitions mismatch");
        }
        if (!gold.path("security").equals(gen.path("security"))) {
            diffs.add("global security mismatch");
        }
        if (!tagNames(gold).equals(tagNames(gen))) {
            diffs.add("tags mismatch: golden=" + tagNames(gold) + " served=" + tagNames(gen));
        }

        JsonNode gp = gold.path("paths");
        JsonNode np = gen.path("paths");
        TreeSet<String> goldPaths = fields(gp);
        TreeSet<String> genPaths = fields(np);
        TreeSet<String> missing = new TreeSet<>(goldPaths);
        missing.removeAll(genPaths);
        TreeSet<String> extra = new TreeSet<>(genPaths);
        extra.removeAll(goldPaths);
        if (!missing.isEmpty()) {
            diffs.add(missing.size() + " missing paths, e.g.: " + first(missing, 8));
        }
        if (!extra.isEmpty()) {
            diffs.add(extra.size() + " extra paths, e.g.: " + first(extra, 8));
        }

        int opDiffs = 0;
        for (String path : goldPaths) {
            if (!genPaths.contains(path)) {
                continue;
            }
            TreeSet<String> verbs = fields(gp.get(path));
            verbs.retainAll(fields(np.get(path)));
            for (String verb : verbs) {
                if (!opSignature(gp.get(path).get(verb)).equals(opSignature(np.get(path).get(verb)))) {
                    opDiffs++;
                }
            }
        }
        if (opDiffs > 0) {
            diffs.add(opDiffs + " operations differ in tags/params/responses");
        }

        TreeSet<String> goldDefs = fields(gold.path("definitions"));
        TreeSet<String> genDefs = fields(gen.path("definitions"));
        TreeSet<String> missingDefs = new TreeSet<>(goldDefs);
        missingDefs.removeAll(genDefs);
        if (!missingDefs.isEmpty()) {
            diffs.add(missingDefs.size() + " missing definitions, e.g.: " + first(missingDefs, 8));
        }

        assertTrue("Served swagger.json is not equivalent to the golden baseline.\n"
                + "Re-baseline with `mvn -Pswagger-regen` after intentional API changes.\n  "
                + String.join("\n  ", diffs), diffs.isEmpty());
    }

    /** Contract-relevant, order-independent signature of an operation. */
    private static String opSignature(JsonNode op) {
        TreeMap<String, String> sig = new TreeMap<>();
        sig.put("tags", set(op.get("tags")).toString());
        sig.put("consumes", set(op.get("consumes")).toString());
        sig.put("produces", set(op.get("produces")).toString());
        TreeSet<String> params = new TreeSet<>();
        if (op.has("parameters")) {
            for (JsonNode p : op.get("parameters")) {
                params.add(p.path("name").asText() + "|" + p.path("in").asText() + "|"
                        + normSchema(p.get("schema"))
                        + "|" + p.path("type").asText("") + "|" + p.path("format").asText(""));
            }
        }
        sig.put("parameters", params.toString());
        TreeMap<String, String> responses = new TreeMap<>();
        if (op.has("responses")) {
            Iterator<Map.Entry<String, JsonNode>> it = op.get("responses").fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                responses.put(e.getKey(), normSchema(e.getValue().get("schema")));
            }
        }
        sig.put("responses", responses.toString());
        return sig.toString();
    }

    /** Normalize a schema to its contract-relevant shape ($ref name, type, item type). */
    private static String normSchema(JsonNode schema) {
        if (schema == null || schema.isNull()) {
            return "null";
        }
        if (schema.has("$ref")) {
            return "ref:" + schema.get("$ref").asText();
        }
        StringBuilder sb = new StringBuilder();
        if (schema.has("type")) {
            sb.append("type=").append(schema.get("type").asText());
        }
        if (schema.has("items")) {
            sb.append(",items=").append(normSchema(schema.get("items")));
        }
        return sb.toString();
    }

    private static TreeSet<String> tagNames(JsonNode root) {
        TreeSet<String> names = new TreeSet<>();
        if (root.has("tags")) {
            for (JsonNode t : root.get("tags")) {
                names.add(t.path("name").asText());
            }
        }
        return names;
    }

    private static TreeSet<String> set(JsonNode array) {
        TreeSet<String> s = new TreeSet<>();
        if (array != null && array.isArray()) {
            for (JsonNode n : array) {
                s.add(n.asText());
            }
        }
        return s;
    }

    private static TreeSet<String> fields(JsonNode obj) {
        TreeSet<String> s = new TreeSet<>();
        if (obj != null && obj.isObject()) {
            Iterator<String> it = obj.fieldNames();
            while (it.hasNext()) {
                s.add(it.next());
            }
        }
        return s;
    }

    private static List<String> first(TreeSet<String> set, int n) {
        List<String> out = new ArrayList<>();
        for (String s : set) {
            if (out.size() >= n) {
                break;
            }
            out.add(s);
        }
        return out;
    }
}
