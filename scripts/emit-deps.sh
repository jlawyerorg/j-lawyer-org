#!/bin/bash
# Emits <dependency> stanzas for all jars under lib paths matching the given
# substring(s), using the GAV map produced by seed-maven-repo.sh.
#
# Usage: ./scripts/emit-deps.sh <path-substring> [scope] [exclude-regex]
#   path-substring : e.g. "j-lawyer-fax/lib/"  (matched against the source path)
#   scope          : optional maven scope (compile|provided|test|runtime)
#   exclude-regex  : optional egrep pattern of source paths to skip
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MAP="$ROOT/scripts/lib-gav-map.txt"
SUB="$1"
SCOPE="${2:-}"
EXCL="${3:-}"

grep -F "$SUB" "$MAP" | while IFS= read -r line; do
  src="${line%% => *}"
  gav="${line##* => }"
  if [[ -n "$EXCL" ]] && echo "$src" | grep -qE "$EXCL"; then continue; fi
  IFS=':' read -r g a v <<< "$gav"
  printf '        <dependency>\n            <groupId>%s</groupId>\n            <artifactId>%s</artifactId>\n            <version>%s</version>\n' "$g" "$a" "$v"
  [[ -n "$SCOPE" ]] && printf '            <scope>%s</scope>\n' "$SCOPE"
  printf '        </dependency>\n'
done
