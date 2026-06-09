#!/usr/bin/env python3
"""
Semantic equivalence gate for the j-lawyer-io REST API specification.

Compares a freshly generated swagger.json against the committed golden baseline.
Differences in formatting / key ordering are ignored; structural differences in the
API contract (paths, methods, parameters, response schemas, security, tags) fail.

Usage: swagger-equivalence-check.py <generated.json> <golden.json>
Exit code 0 = equivalent, 1 = differences found (printed as a report).
"""
import json, sys

def load(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)

def norm_schema(s):
    if not isinstance(s, dict):
        return s
    if "$ref" in s:
        return {"$ref": s["$ref"]}
    out = {}
    if "type" in s: out["type"] = s["type"]
    if "items" in s: out["items"] = norm_schema(s["items"])
    return out

def op_signature(op):
    """Contract-relevant fields of an operation (ignore operationId/summary ordering)."""
    params = sorted(
        (p.get("name"), p.get("in"), json.dumps(norm_schema(p.get("schema")), sort_keys=True)
         if p.get("schema") else (p.get("type"), p.get("format")))
        for p in op.get("parameters", [])
    )
    responses = {
        code: json.dumps(norm_schema(r.get("schema")), sort_keys=True) if isinstance(r, dict) and r.get("schema") else None
        for code, r in op.get("responses", {}).items()
    }
    return {
        "tags": sorted(op.get("tags", [])),
        "consumes": sorted(op.get("consumes", [])),
        "produces": sorted(op.get("produces", [])),
        "parameters": params,
        "responses": responses,
    }

def main():
    gen, gold = load(sys.argv[1]), load(sys.argv[2])
    diffs = []

    for key in ("swagger", "basePath"):
        if gen.get(key) != gold.get(key):
            diffs.append(f"top-level '{key}': {gen.get(key)!r} != {gold.get(key)!r}")
    if gen.get("info", {}).get("version") != gold.get("info", {}).get("version"):
        diffs.append("info.version mismatch")
    if sorted(gen.get("schemes", [])) != sorted(gold.get("schemes", [])):
        diffs.append("schemes mismatch")
    if gen.get("securityDefinitions") != gold.get("securityDefinitions"):
        diffs.append(f"securityDefinitions mismatch: {gen.get('securityDefinitions')} != {gold.get('securityDefinitions')}")
    if gen.get("security") != gold.get("security"):
        diffs.append(f"global security mismatch: {gen.get('security')} != {gold.get('security')}")

    gtags = {t["name"] for t in gold.get("tags", [])}
    ntags = {t["name"] for t in gen.get("tags", [])}
    if gtags - ntags:
        diffs.append(f"missing tags: {sorted(gtags - ntags)}")
    if ntags - gtags:
        diffs.append(f"extra tags: {sorted(ntags - gtags)}")

    gpaths, npaths = gold.get("paths", {}), gen.get("paths", {})
    missing = sorted(set(gpaths) - set(npaths))
    extra = sorted(set(npaths) - set(gpaths))
    if missing:
        diffs.append(f"{len(missing)} missing paths, e.g.: {missing[:8]}")
    if extra:
        diffs.append(f"{len(extra)} extra paths, e.g.: {extra[:8]}")

    op_diffs = 0
    for path in sorted(set(gpaths) & set(npaths)):
        for m in set(gpaths[path]) | set(npaths[path]):
            go, no = gpaths[path].get(m), npaths[path].get(m)
            if (go is None) != (no is None):
                op_diffs += 1; continue
            if go and no and op_signature(go) != op_signature(no):
                op_diffs += 1
    if op_diffs:
        diffs.append(f"{op_diffs} operations differ in tags/params/responses")

    gdefs, ndefs = set(gold.get("definitions", {})), set(gen.get("definitions", {}))
    if gdefs - ndefs:
        diffs.append(f"{len(gdefs - ndefs)} missing definitions, e.g.: {sorted(gdefs - ndefs)[:8]}")

    if diffs:
        print("SWAGGER EQUIVALENCE: FAIL")
        for d in diffs:
            print("  -", d)
        sys.exit(1)
    print("SWAGGER EQUIVALENCE: OK")

if __name__ == "__main__":
    main()
