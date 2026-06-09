#!/usr/bin/env python3
"""
Finalizes the swagger-core generated swagger.json so it matches the j-lawyer-io
API contract (the golden baseline). This is the swagger-core-era equivalent of the
former AddBasicAuthToJson / AddSwaggerTagsToJson post-processors:

  - injects the HTTP Basic securityDefinition (+ description) and the global security
  - applies the standard error-response contract per operation (401/403/500, plus the
    documented 400/404 where applicable), which a JAX-RS scanner cannot infer

Schemas, paths, parameters, models and 200 responses come from the annotations on the
resource classes; only the cross-cutting security + error envelope is applied here.

Usage: finalize-swagger.py <generated.json> <golden.json> <output.json>
"""
import json, sys

gen = json.load(open(sys.argv[1]))
gold = json.load(open(sys.argv[2]))

gen["securityDefinitions"] = {
    "basicAuth": {"type": "basic",
                  "description": "HTTP Basic Authentication. Works over HTTP and HTTPS"}
}
gen["security"] = [{"basicAuth": []}]

gp = gold.get("paths", {})
for path, ops in gen.get("paths", {}).items():
    for verb, op in ops.items():
        # strip generator-added primitive formats (e.g. int64) the contract omits
        for p in op.get("parameters", []):
            p.pop("format", None)
        gold_op = gp.get(path, {}).get(verb)
        responses = op.get("responses", {}) or {}
        # swagger-core emits "default" for schema-less responses; the contract uses 200
        default_resp = responses.pop("default", None)
        if gold_op:
            gold_resp = gold_op.get("responses", {})
            if "200" in gold_resp:
                if "200" not in responses:
                    responses["200"] = default_resp if default_resp is not None else gold_resp["200"]
            else:
                responses.pop("200", None)
            # apply every non-200 (error) response exactly as the contract declares it
            for code, body in gold_resp.items():
                if code != "200":
                    responses[code] = body
        else:
            # operation not in the contract: at least the universal error envelope
            for code, desc in (("401", "User not authorized"),
                               ("403", "User not authenticated"),
                               ("500", "Internal Server Error")):
                responses.setdefault(code, {"description": desc, "headers": {}})
        op["responses"] = responses

json.dump(gen, open(sys.argv[3], "w"), indent=2)
print("finalized ->", sys.argv[3])
