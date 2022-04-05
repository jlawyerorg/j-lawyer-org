#!/bin/bash

# requires: sudo apt-get install openjfx

# set this if you want to execute the Sipgate API tests
export sipuser=""
export sippassword=""

# set this if you want to execute the virtual file system tests, required for FTP and SFTP implementations
export ftpuser=""
export ftppassword=""
export ftphome=""
export ftphost=""


# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
mvn -f j-lawyer-cloud/pom.xml install -DskipTests

if [ "$sipuser" = "" ] || [ "$sippassword" = "" ]
then
   echo "Sipgate credentials not set, skipping j-lawyer-fax tests"
   ant -buildfile j-lawyer-fax/build.xml jar
else
   ant -buildfile j-lawyer-fax/build.xml default
fi

if [ "$ftpuser" = "" ] || [ "$ftppassword" = "" ] || [ "$ftphome" = "" ] || [ "$ftphost" = "" ]
then
   echo "VFS ftp / sftp credentials not set, skipping j-lawyer-server-common tests"
   ant -buildfile j-lawyer-server-common/build.xml jar
else
   ant -buildfile j-lawyer-server-common/build.xml default
fi

ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server-entities/build.xml default

ant -buildfile j-lawyer-server-api/build.xml default

ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server/build.xml default test

ant -buildfile j-lawyer-io-common/build.xml default

ant -buildfile j-lawyer-client/build.xml default

# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
mvn -f j-lawyer-backupmgr/pom.xml clean package test -DskipTests
