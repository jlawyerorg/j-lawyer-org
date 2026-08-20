#!/bin/bash
# Builds the whole project with Maven on Java 17 (tests skipped), in reactor order.
set -e
export JAVA_HOME=/home/jens/bin/jdk-17.0.9-full
export PATH="$JAVA_HOME/bin:$PATH"

# First run only: seed the in-project file repository from the committed lib/ jars.
if [ ! -d maven-repo ]; then
    ./scripts/seed-maven-repo.sh
fi

mvn -Dmaven.test.skip=true clean install
