#! /bin/bash
set -e

#HOST="$MYSQL_PORT_3306_TCP_ADDR"
#PORT="$MYSQL_PORT_3306_TCP_PORT"
#USER="root"
#PASSWORD="$MYSQL_ENV_MYSQL_ROOT_PASSWORD"
#DATABASE="$MYSQL_ENV_MYSQL_DATABASE"
#SQL_PATH="${SQL_PATH:-/var/www/Config/sql}"

HOST="db"
PORT="3306"
USER="jlawyer"
PASSWORD="jlawyer"
DATABASE="jlawyerdb"
# SQL_PATH="${SQL_PATH:-/var/www/Config/sql}"

# echo $SQL_FILE;
# SQL_FILE="${SQL_FILE:-database.sql}"
# echo $SQL_FILE;

until echo '\q' | mysql -h"$HOST" -P"$PORT" -u"$USER" -p"$PASSWORD" $DATABASE; do
    >&2 echo "MySQL is unavailable - sleeping"
    sleep 5
done

# while [ ! -d $SQL_PATH ]; do
#     >&2 echo "Data is unavailable - sleeping"
#     sleep 1;
# done;

>&2 echo "MySQL and Data are up - executing command"

# cat $SQL_PATH/$SQL_FILE | mysql -h"$HOST" -P"$PORT" -u"$USER" -p"$PASSWORD" $DATABASE

# mkdir -p /opt/jboss/j-lawyer-data/templates
# mkdir -p /opt/jboss/j-lawyer-data/emailtemplates
# mkdir -p /opt/jboss/j-lawyer-data/mastertemplates
# mkdir -p /opt/jboss/j-lawyer-data/archivefiles
# mkdir -p /opt/jboss/j-lawyer-data/searchindex
# mkdir -p /opt/jboss/j-lawyer-data/faxqueue
# chmod -R 777 /opt/jboss/j-lawyer-data

/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0