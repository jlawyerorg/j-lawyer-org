#!/usr/bin/env python3
"""
Adds @ApiResponses for the endpoint-specific 400/404 responses (the uniform
401/403/500 envelope is applied by finalize-swagger.py). Driven by the golden as
the record of which operations declare those codes. Insert-only (safe).
Idempotent: methods already carrying @ApiResponses are skipped.

Usage: inject-response-codes.py <io-src-root> <golden.json> [--apply]
"""
import json, sys, glob, re

IO_ROOT, GOLD = sys.argv[1], sys.argv[2]
APPLY = "--apply" in sys.argv
gold = json.load(open(GOLD))
MSG = {"400": "Bad Request", "404": "Not Found"}
HTTP = {"@GET": "get", "@POST": "post", "@PUT": "put", "@DELETE": "delete", "@HEAD": "head", "@OPTIONS": "options"}
stats = {"added": 0, "skipped": 0}

for jf in glob.glob(IO_ROOT + "/**/*.java", recursive=True):
    lines = open(jf, encoding="utf-8").read().split("\n")
    cm = re.findall(r'@Path\("([^"]*)"\)', "\n".join(lines).split("public class")[0])
    class_path = cm[-1] if cm else ""
    edits = []
    for idx, line in enumerate(lines):
        if not re.match(r'\s*public\s+\w[\w<>\[\].,\s]*\s+\w+\s*\(', line):
            continue
        b = idx - 1; block = []
        while b >= 0 and (lines[b].strip().startswith("@") or lines[b].strip() == "" or lines[b].strip().startswith(("*", "/"))):
            block.append(lines[b]); b -= 1
        bt = "\n".join(block)
        verb = next((v for a, v in HTTP.items() if re.search(re.escape(a) + r'\b', bt)), None)
        if not verb:
            continue
        mp = re.search(r'@Path\("([^"]*)"\)', bt)
        full = "/" + "/".join(p for p in (class_path.strip("/"), (mp.group(1) if mp else "").strip("/")) if p)
        op = gold.get("paths", {}).get(full, {}).get(verb)
        if not op:
            continue
        codes = [c for c in ("400", "404") if c in op.get("responses", {})]
        if not codes:
            continue
        if "ApiResponses" in bt:
            stats["skipped"] += 1; continue
        indent = re.match(r'\s*', line).group(0)
        items = ", ".join(f'@io.swagger.annotations.ApiResponse(code={c}, message="{MSG[c]}")' for c in codes)
        edits.append((idx, f'{indent}@io.swagger.annotations.ApiResponses({{{items}}})'))
        stats["added"] += 1
    for i2, payload in sorted(edits, key=lambda z: -z[0]):
        lines.insert(i2, payload)
    if APPLY and edits:
        open(jf, "w", encoding="utf-8").write("\n".join(lines))

print("stats:", stats, "(dry run)" if not APPLY else "APPLIED")
