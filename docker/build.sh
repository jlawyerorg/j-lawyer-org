#! /bin/bash

cp ../j-lawyer-server/dist/j-lawyer-server.ear ./wildfly/
# cp /home/travis/build/[secure]org/j-lawyer-org/j-lawyer-server/dist/j-lawyer-server.ear ./wildfly/


jlversion=`bash getversion.sh`

echo "j-lawyer docker version tag: $jlversion"

docker build -t="jlawyerorg/jlawyer-db:latest" -t="jlawyerorg/jlawyer-db:$jlversion" mysql/.
docker build -t="jlawyerorg/jlawyer-srv:latest" -t="jlawyerorg/jlawyer-srv:$jlversion" wildfly/.
# docker run --name mysql -v /var/docker_data/mysql/data/:/var/lib/mysql -d -p 3307:3306 j-dimension/jlawyerdb
# auf dem host: mysql --host=127.0.0.1 --port=3307 -u jlawyer -p

# docker stop mysql
# docker container rm mysql
