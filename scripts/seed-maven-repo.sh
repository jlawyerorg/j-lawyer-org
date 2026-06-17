#!/bin/bash
#
# Seeds the in-project file-based Maven repository (./maven-repo) from the jars
# currently committed under the various module lib/ folders plus the proprietary
# beA wrapper. Existing versions are preserved verbatim (no upgrades).
#
# After the migrate-dependencies-to-central change, only the RESIDUAL artifacts are
# still committed and seeded here: the proprietary beA wrapper + its lib/bea jars,
# WildFly's jboss-client, and the repackaged / not-on-Central / no-version jars that
# the SHA-1 verification confirmed differ from any Maven Central release. Everything
# else now resolves from Maven Central via real groupId:artifactId:version coordinates.
#
# GAV derivation from a jar file name:
#   groupId    = jlawyer.thirdparty           (single flat namespace)
#   artifactId = everything before the first "-<digit>" token
#   version    = that token and the remainder (or 0.0.0 when no version present)
#
# The same jar that appears in multiple lib/ folders is installed once (idempotent).
#
# Usage: JAVA_HOME=/home/jens/bin/jdk-17.0.9-full ./scripts/seed-maven-repo.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$ROOT/maven-repo"
GROUP="jlawyer.thirdparty"
MAP="$ROOT/scripts/lib-gav-map.txt"

mkdir -p "$REPO"
: > "$MAP"

# Directories whose jars are NOT runtime/compile dependencies and must be skipped.
SKIP_REGEX='/(bea\.bak|CopyLibs|CopyLibs-2)/'

derive_gav() {
  # $1 = base file name (without path), no .jar
  local base="$1"
  local artifact version
  # find first hyphen-delimited token that starts with a digit
  if [[ "$base" =~ ^(.*)-([0-9][^-]*(-.*)?)$ ]]; then
    artifact="${BASH_REMATCH[1]}"
    version="${BASH_REMATCH[2]}"
  else
    artifact="$base"
    version="0.0.0"
  fi
  echo "$artifact|$version"
}

install_jar() {
  local jar="$1"
  local base; base="$(basename "$jar" .jar)"
  local gav; gav="$(derive_gav "$base")"
  local artifact="${gav%%|*}"
  local version="${gav##*|}"
  # record mapping: <source-path> -> G:A:V
  echo "$jar => $GROUP:$artifact:$version" >> "$MAP"
  # skip if already installed
  local target="$REPO/${GROUP//./\/}/$artifact/$version/$artifact-$version.jar"
  if [[ -f "$target" ]]; then
    return 0
  fi
  mvn -q org.apache.maven.plugins:maven-install-plugin:3.1.1:install-file \
      -Dfile="$jar" \
      -DgroupId="$GROUP" \
      -DartifactId="$artifact" \
      -Dversion="$version" \
      -Dpackaging=jar \
      -DlocalRepositoryPath="$REPO" \
      -DcreateChecksum=true
}

echo "Seeding $REPO ..."
while IFS= read -r -d '' jar; do
  if [[ "$jar" =~ $SKIP_REGEX ]]; then
    continue
  fi
  install_jar "$jar"
done < <(find "$ROOT" \
            -path "$ROOT/maven-repo" -prune -o \
            -name '*.jar' \
            \( -path '*/lib/*' -o -path '*/libs/*' \) \
            ! -path '*/build/*' ! -path '*/dist/*' ! -path '*/target/*' \
            -print0)

# maven-install-plugin reuses any pom.xml embedded in a jar (wrong coordinates,
# parents, transitive deps). Overwrite every generated pom with a minimal stub so
# the file repo only provides the jar with our flat coordinates.
python3 - "$REPO" <<'PY'
import os,glob,sys
repo=sys.argv[1]
for pom in glob.glob(repo+'/**/*.pom',recursive=True):
    parts=os.path.relpath(pom,repo).split(os.sep)
    version=parts[-2]; artifact=parts[-3]; group='.'.join(parts[:-3])
    open(pom,'w',encoding='utf-8').write(
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<project xmlns="http://maven.apache.org/POM/4.0.0">\n'
        '  <modelVersion>4.0.0</modelVersion>\n'
        f'  <groupId>{group}</groupId>\n  <artifactId>{artifact}</artifactId>\n'
        f'  <version>{version}</version>\n  <packaging>jar</packaging>\n</project>\n')
PY

echo "Done. GAV map written to $MAP"
echo "Unique artifacts installed:"
awk -F' => ' '{print $2}' "$MAP" | sort -u | wc -l
