#!/bin/bash

# requires: sudo apt-get install openjfx

ant -buildfile j-lawyer-fax/build.xml clean
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server-entities/build.xml clean
ant -buildfile j-lawyer-server-common/build.xml clean
ant -buildfile j-lawyer-server-api/build.xml clean
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server/build.xml clean
ant -buildfile j-lawyer-io-common/build.xml clean
ant -buildfile j-lawyer-client/build.xml clean

# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
mvn -f j-lawyer-backupmgr/pom.xml clean
