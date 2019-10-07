#!/bin/bash

# requires: sudo apt-get install openjfx

# skips tests

ant -buildfile j-lawyer-fax/build.xml jar
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server-entities/build.xml dist
ant -buildfile j-lawyer-server-common/build.xml jar
ant -buildfile j-lawyer-server-api/build.xml jar
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server/build.xml dist
ant -buildfile j-lawyer-io-common/build.xml jar
ant -buildfile j-lawyer-client/build.xml jar

# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
mvn -f j-lawyer-backupmgr/pom.xml package -DskipTests
