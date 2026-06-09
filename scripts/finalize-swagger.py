#!/usr/bin/env python3
"""
Applies the cross-cutting envelope that a JAX-RS scanner cannot infer to the
swagger-core generated spec:
  - HTTP Basic securityDefinition (+ description) and the global security requirement
  - the uniform 401/403/500 responses every secured endpoint may return

Everything else (paths, methods, parameters, request bodies, models, 200 responses,
operationIds, and the endpoint-specific 400/404 via @ApiResponses) comes from the
annotations on the resource classes. No golden coupling, no generator-quirk rewriting.

Usage: finalize-swagger.py <generated.json> <output.json>
"""
import json, sys

gen = json.load(open(sys.argv[1]))

gen["securityDefinitions"] = {
    "basicAuth": {"type": "basic",
                  "description": "HTTP Basic Authentication. Works over HTTP and HTTPS"}
}
gen["security"] = [{"basicAuth": []}]

ENVELOPE = {
    "401": {"description": "User not authorized", "headers": {}},
    "403": {"description": "User not authenticated", "headers": {}},
    "500": {"description": "Internal Server Error", "headers": {}},
}

for ops in gen.get("paths", {}).values():
    for op in ops.values():
        responses = op.setdefault("responses", {})
        for code, body in ENVELOPE.items():
            responses.setdefault(code, body)

json.dump(gen, open(sys.argv[2], "w"), indent=2)
print("finalized ->", sys.argv[2])
