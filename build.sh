#!/bin/bash
# Builds the whole project with Maven on Java 17 (with tests), in reactor order.
# Optional integration-test credentials (Sipgate / FTP-SFTP) as before:
export sipuser="${sipuser:-}"
export sippassword="${sippassword:-}"
export ftpuser="${ftpuser:-}"
export ftppassword="${ftppassword:-}"
export ftphome="${ftphome:-}"
export ftphost="${ftphost:-}"
set -e
export JAVA_HOME=/home/jens/bin/jdk-17.0.9-full
export PATH="$JAVA_HOME/bin:$PATH"

if [ ! -d maven-repo ]; then
    ./scripts/seed-maven-repo.sh
fi

mvn clean install
