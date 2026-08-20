#!/bin/bash
# Emits deduplicated <dependency> stanzas (by G:A:V) for all jars whose source
# path matches ANY of the given path substrings (comma-separated arg 1).
# arg2 = scope (optional), arg3 = exclude egrep regex (optional, matched on src path AND on G:A:V)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MAP="$ROOT/scripts/lib-gav-map.txt"
IFS=',' read -ra SUBS <<< "$1"
SCOPE="${2:-}"
EXCL="${3:-}"

{
for sub in "${SUBS[@]}"; do grep -F "$sub" "$MAP"; done
} | while IFS= read -r line; do
  src="${line%% => *}"; gav="${line##* => }"
  if [[ -n "$EXCL" ]] && { echo "$src" | grep -qE "$EXCL" || echo "$gav" | grep -qE "$EXCL"; }; then continue; fi
  echo "$gav"
done | sort -u | while IFS=':' read -r g a v; do
  printf '        <dependency>\n            <groupId>%s</groupId>\n            <artifactId>%s</artifactId>\n            <version>%s</version>\n' "$g" "$a" "$v"
  [[ -n "$SCOPE" ]] && printf '            <scope>%s</scope>\n' "$SCOPE"
  printf '        </dependency>\n'
done
