rm -R /home/jens/dev/projects/j-lawyer-data
cp -R /home/jens/dev/projects/j-lawyer-data.bak /home/jens/dev/projects/j-lawyer-data
~/bin/jdk1.8.0_66/bin/java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8787 -jar target/j-lawyer-backupmgr-1.0-SNAPSHOT.jar