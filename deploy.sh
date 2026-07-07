#!/bin/bash
DEPLOY_DIR=~/bin/wildfly-26.1.3.Final/standalone/deployments
cp j-lawyer-server/j-lawyer-server-ear/target/j-lawyer-server.ear "$DEPLOY_DIR/j-lawyer-server.ear"

# Web UI (standalone WAR, opt-in module -Pweb). Deployed alongside the EAR only when
# it has been built (see j-lawyer-web/README.md). Served at /j-lawyer-web.
WEB_WAR=j-lawyer-web/target/j-lawyer-web.war
if [ -f "$WEB_WAR" ]; then
    cp "$WEB_WAR" "$DEPLOY_DIR/j-lawyer-web.war"
fi
