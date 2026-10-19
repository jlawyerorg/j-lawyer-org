#!/bin/sh
# Builds the skill archives for upload to claude.ai / Claude Desktop (Settings -> Capabilities -> Skills).
# Regenerates the placeholder catalog from the server sources first, so the skill stays in sync.
#
#   skills/build-skills.sh   ->  skills/dist/j-lawyer-vorlagen.zip
set -e
cd "$(dirname "$0")"

python3 tools/generate_catalog.py
python3 -m unittest discover -s j-lawyer-vorlagen/tests

mkdir -p dist
for skill in j-lawyer-vorlagen; do
    rm -f "dist/$skill.zip"
    python3 - "$skill" <<'EOF'
import os, sys, zipfile
skill = sys.argv[1]
with zipfile.ZipFile(f"dist/{skill}.zip", "w", zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(skill):
        dirs[:] = sorted(d for d in dirs if d not in ("tests", "__pycache__"))
        for f in sorted(files):
            if not f.endswith(".pyc"):
                z.write(os.path.join(root, f))
EOF
    echo "dist/$skill.zip"
done
