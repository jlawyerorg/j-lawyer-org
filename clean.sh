#!/bin/bash
# Cleans all Maven build outputs (target/). Keeps the in-project maven-repo.
set -e
export JAVA_HOME=/home/jens/bin/jdk-17.0.9-full
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean
