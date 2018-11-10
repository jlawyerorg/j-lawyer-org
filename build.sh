#!/bin/bash

# requires: sudo apt-get install openjfx

ant -buildfile j-lawyer-fax/build.xml default
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server-entities/build.xml default
ant -buildfile j-lawyer-server-common/build.xml default
ant -buildfile j-lawyer-server-api/build.xml default
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server/build.xml default test
ant -buildfile j-lawyer-io-common/build.xml default
ant -buildfile j-lawyer-client/build.xml default

# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
mvn -f j-lawyer-backupmgr/pom.xml clean package test -DskipTests
