#!/bin/bash

# requires: sudo apt-get install openjfx

# set tools
export ANTEXEC=../tools/apache-ant-1.10.14/bin/ant
export MVNEXEC=../tools/apache-maven-3.9.7/bin/mvn

export JDK17=../tools/jdk-17.0.11-full/
export JDK11=../tools/jdk-11.0.23-full/
export JDK8=../tools/jdk8u412-full/

# skips tests
function check_error {
	if [ $1 -gt 0 ]; then
		echo "Found error!"
		exit 1
	fi
}

# export JAVA_HOME=/home/jens/bin/jdk-17.0.9-full/
export JAVA_HOME=$JDK17

# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
$MVNEXEC -f j-lawyer-cloud/pom.xml install -DskipTests
check_error $?

$ANTEXEC -buildfile j-lawyer-fax/build.xml jar
check_error $?
$ANTEXEC -buildfile j-lawyer-server-common/build.xml jar
check_error $?
$ANTEXEC -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server-entities/build.xml dist
check_error $?
$ANTEXEC -buildfile j-lawyer-server-api/build.xml jar
check_error $?
export JAVA_HOME=$JDK8
$ANTEXEC -Dplatforms.default_platform.home=$JDK11 -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server/build.xml dist
check_error $?
$ANTEXEC -buildfile j-lawyer-io-common/build.xml jar
check_error $?
export JAVA_HOME=$JDK17
$ANTEXEC -Dplatforms.JDK_17.home=$JDK17 -buildfile j-lawyer-client/build.xml jar
check_error $?

# export JAVA_HOME=/home/jens/bin/jdk8u265-full/
# there is an OpenJDK bug on Ubuntu, causing Surefire tests to fail - skip tests for now
# $MVNEXEC -f j-lawyer-backupmgr/pom.xml package -DskipTests -Djava.home=/home/jens/bin/jdk8u265-full/jre

