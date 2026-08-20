#!/bin/bash
#
# Seeds the in-project npm cache (./.npm-cache) from the vetted package tarballs
# committed under ./vendor-npm/*.tgz — the npm analogue of scripts/seed-maven-repo.sh
# (which seeds ./maven-repo from the committed lib/ jars).
#
# Two-phase supply-chain workflow (see SUPPLY-CHAIN.md):
#   Phase 1 (once, on a trusted machine, reviewed):  npm ci --ignore-scripts against the
#           public registry, then vendor each resolved tarball into ./vendor-npm/.
#   Phase 2 (every reproducible build):  this script populates ./.npm-cache, then
#           `npm ci --offline --ignore-scripts` resolves fully offline from that cache.
#
# SKELETON: activates once vendor-npm/ is populated in Phase 1. Runs nothing destructive.
#
# Usage: ./scripts/seed-npm-cache.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VENDOR="$ROOT/vendor-npm"
CACHE="$ROOT/.npm-cache"

if [[ ! -d "$VENDOR" ]] || ! ls "$VENDOR"/*.tgz >/dev/null 2>&1; then
  echo "seed-npm-cache: no vendored tarballs found under $VENDOR" >&2
  echo "Run Phase 1 first (see SUPPLY-CHAIN.md) to produce vendor-npm/*.tgz." >&2
  exit 1
fi

mkdir -p "$CACHE"
echo "Seeding npm cache at $CACHE from $VENDOR ..."
for tgz in "$VENDOR"/*.tgz; do
  npm cache add "$tgz" --cache "$CACHE"
  echo "  + $(basename "$tgz")"
done
echo "Done. Install reproducibly with:"
echo "  npm ci --offline --ignore-scripts --cache \"$CACHE\""
