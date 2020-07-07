#!/bin/bash

# requires: sudo apt-get install openjfx

# skips tests
function check_error {
	if [ $1 -gt 0 ]; then
		echo "Found error!"
		exit 1
	fi
}

# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
mvn -f j-lawyer-cloud/pom.xml install -DskipTests
check_error $?

ant -buildfile j-lawyer-fax/build.xml jar
check_error $?
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server-entities/build.xml dist
check_error $?
ant -buildfile j-lawyer-server-common/build.xml jar
check_error $?
ant -buildfile j-lawyer-server-api/build.xml jar
check_error $?
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server/build.xml dist
check_error $?
ant -buildfile j-lawyer-io-common/build.xml jar
check_error $?
ant -buildfile j-lawyer-client/build.xml jar
check_error $?

# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
mvn -f j-lawyer-backupmgr/pom.xml package -DskipTests

