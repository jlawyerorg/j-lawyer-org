#!/usr/bin/env python3
"""
Injects swagger-core annotations into the j-lawyer-io REST resource classes,
driven by the golden swagger.json (source of truth for response types/bodies).

Method-anchored: for each `public ...(` resource method, scan UPWARD over its
contiguous annotation block (annotation order is irrelevant), determine the HTTP
verb + method @Path, match to the golden, and inject:
  - @ApiOperation(value="", response=<FQN>.class[, responseContainer="List"])
  - @ApiParam on the body parameter
Models are generated automatically by swagger-core via reflection.
Idempotent: methods already carrying @ApiOperation are skipped.

Usage: inject-swagger-annotations.py <io-src-root> <golden.json> [--apply]
"""
import json, sys, glob, os, re

IO_ROOT, GOLD = sys.argv[1], sys.argv[2]
APPLY = "--apply" in sys.argv
gold = json.load(open(GOLD))

FQN = {}
for root in [IO_ROOT, "j-lawyer-server-api/src/main/java", "j-lawyer-server-entities/src/main/java"]:
    for jf in glob.glob(root + "/**/*.java", recursive=True):
        s = open(jf, encoding="utf-8", errors="replace").read()
        pkg = re.search(r'package\s+([\w.]+);', s)
        if pkg:
            FQN.setdefault(os.path.basename(jf)[:-5], pkg.group(1) + "." + os.path.basename(jf)[:-5])

PRIM = {"string": "String", "boolean": "Boolean", "integer": "Integer", "number": "Double"}

def resp_for(op):
    sch = (op.get("responses", {}).get("200") or {}).get("schema")
    if not sch: return None, None
    if "$ref" in sch: return FQN.get(sch["$ref"].split("/")[-1]), None
    if sch.get("type") == "array":
        it = sch.get("items", {})
        if "$ref" in it: return FQN.get(it["$ref"].split("/")[-1]), "List"
        return PRIM.get(it.get("type")), "List"
    if sch.get("type") == "object":
        return "Object", None
    return PRIM.get(sch.get("type")), None

HTTP = {"@GET": "get", "@POST": "post", "@PUT": "put", "@DELETE": "delete", "@HEAD": "head", "@OPTIONS": "options"}
BIND = ("@PathParam", "@QueryParam", "@HeaderParam", "@FormParam", "@Context", "@CookieParam", "@MatrixParam")
stats = {"matched": 0, "op": 0, "body": 0, "skipped": 0, "unmatched": 0, "nobody_sig": 0}

for jf in glob.glob(IO_ROOT + "/**/*.java", recursive=True):
    lines = open(jf, encoding="utf-8").read().split("\n")
    head = "\n".join(lines).split("public class")[0]
    cm = re.findall(r'@Path\("([^"]*)"\)', head)
    class_path = cm[-1] if cm else ""
    edits = []
    for idx, line in enumerate(lines):
        m = re.match(r'\s*public\s+\w[\w<>\[\].,\s]*\s+\w+\s*\(', line)
        if not m:
            continue
        # scan upward for the contiguous annotation block
        b = idx - 1
        block = []
        while b >= 0 and (lines[b].strip().startswith("@") or lines[b].strip() == "" or lines[b].strip().startswith("*") or lines[b].strip().startswith("/")):
            block.append(lines[b]); b -= 1
        block_txt = "\n".join(block)
        verb = next((v for a, v in HTTP.items() if re.search(re.escape(a) + r'\b', block_txt)), None)
        if not verb:
            continue
        mp = re.search(r'@Path\("([^"]*)"\)', block_txt)
        method_path = mp.group(1) if mp else ""
        full = "/" + "/".join(p for p in (class_path.strip("/"), method_path.strip("/")) if p)
        op = gold.get("paths", {}).get(full, {}).get(verb)
        if "ApiOperation" in block_txt:
            stats["skipped"] += 1; continue
        if op is None:
            stats["unmatched"] += 1
            print("  UNMATCHED", verb.upper(), full, "::", line.strip()[:50])
            continue
        stats["matched"] += 1
        indent = re.match(r'\s*', line).group(0)
        resp, cont = resp_for(op)
        cont_s = f', responseContainer="{cont}"' if cont else ""
        ann = (f'{indent}@io.swagger.annotations.ApiOperation(value="", response={resp}.class{cont_s})'
               if resp else f'{indent}@io.swagger.annotations.ApiOperation(value="")')
        edits.append((idx, "insert", ann)); stats["op"] += 1
        if any(p.get("in") == "body" for p in op.get("parameters", [])):
            # collect signature (may span lines) until the params' matching ')'
            sig = line; e = idx
            while sig.count("(") > sig.count(")") and e + 1 < len(lines):
                e += 1; sig += "\n" + lines[e]
            # extract the parameter list via balanced parentheses from the first '('
            start = sig.index("("); depth = 0; end = -1
            for ci in range(start, len(sig)):
                if sig[ci] == "(": depth += 1
                elif sig[ci] == ")":
                    depth -= 1
                    if depth == 0: end = ci; break
            params = sig[start + 1:end] if end > start else ""
            if params.strip():
                # split on top-level commas only (respect <>, (), [])
                parts = []; depth = 0; cur = ""
                for ch in params:
                    if ch in "<([": depth += 1
                    elif ch in ">)]": depth -= 1
                    if ch == "," and depth == 0:
                        parts.append(cur.strip()); cur = ""
                    else:
                        cur += ch
                if cur.strip(): parts.append(cur.strip())
                done = False
                for k, p in enumerate(parts):
                    if p and not any(bd in p for bd in BIND) and "ApiParam" not in p:
                        parts[k] = "@io.swagger.annotations.ApiParam " + p; done = True; break
                if done:
                    newsig = sig.replace(params, ", ".join(parts), 1)
                    # replace spanned lines: put newsig on first line, blank the rest
                    nl = newsig.split("\n")
                    edits.append((idx, "replace", nl[0]))
                    for off in range(1, e - idx + 1):
                        edits.append((idx + off, "replace", nl[off] if off < len(nl) else ""))
                    stats["body"] += 1
            else:
                stats["nobody_sig"] += 1
    for i2, kind, payload in sorted(edits, key=lambda z: (-z[0], 0 if z[1] == "replace" else 1)):
        if kind == "insert": lines.insert(i2, payload)
        else: lines[i2] = payload
    if APPLY and edits:
        open(jf, "w", encoding="utf-8").write("\n".join(lines))

print("stats:", stats)
print("(dry run)" if not APPLY else "APPLIED")
