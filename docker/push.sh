#! /bin/bash

# docker login --username=yourhubusername --email=youremail@company.com

jlversion=`bash getversion.sh`

docker push jlawyerorg/jlawyer-db:latest
docker push jlawyerorg/jlawyer-db:$jlversion
docker push jlawyerorg/jlawyer-srv:latest
docker push jlawyerorg/jlawyer-srv:$jlversion

