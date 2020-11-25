#!/bin/bash

echo "usage: deploy-demo.sh <username> <password>"
deployuser=$1
deploypassword=$2


echo "stopping j-lawyer-server"
sshpass -p $deploypassword ssh -o "StrictHostKeyChecking=no" $deployuser@demo.j-lawyer.com 'service j-lawyer-server stop'
echo "copying j-lawyer-server.ear"
sshpass -p "$deploypassword" scp j-lawyer-server/dist/j-lawyer-server.ear $deployuser@demo.j-lawyer.com:/usr/local/j-lawyer-server/wildfly/standalone/deployments/j-lawyer-server.ear
echo "restarting j-lawyer-server"
sshpass -p $deploypassword ssh -o "StrictHostKeyChecking=no" $deployuser@demo.j-lawyer.com 'service j-lawyer-server restart'

echo "copying client"
sshpass -p $deploypassword rsync -vrt j-lawyer-client/dist/ $deployuser@demo.j-lawyer.com:/home/jlawyer/j-lawyer-client
echo "changing file privileges"
sshpass -p $deploypassword ssh -o "StrictHostKeyChecking=no" $deployuser@demo.j-lawyer.com 'chown -R jlawyer:jlawyer /home/jlawyer/j-lawyer-client'