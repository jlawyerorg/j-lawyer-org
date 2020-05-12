#! /bin/bash
set -e

# MYSQL_ROOT_PWD=${MYSQL_ROOT_PWD:-"mysql"}
# MYSQL_USER=${MYSQL_USER:-"jlawyer"}
# MYSQL_USER_PWD=${MYSQL_USER_PWD:-"jlawyer"}
# MYSQL_USER_DB=${MYSQL_USER_DB:-"jlawyer"}

#! /bin/bash
set -e

if [ ! -f /var/lib/mysql/ibdata1 ];then
	MYSQL_ROOT_PWD="mysql"
        MYSQL_USER="jlawyer"
        MYSQL_USER_PWD="jlawyer"
        MYSQL_USER_DB="jlawyerdb"
	mysqld --initialize-insecure --user=mysql
	service mysql start $ sleep 10

	echo "[i] Setting root new password."
	mysql --user=root -e "UPDATE mysql.user set authentication_string=password('$MYSQL_ROOT_PWD') where user='root'; FLUSH PRIVILEGES;"
	

	echo "[i] Setting root remote password."
	mysql --user=root --password=$MYSQL_ROOT_PWD -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '$MYSQL_ROOT_PWD' WITH GRANT OPTION; FLUSH PRIVILEGES;"
	
	echo "[i] Setting up new power user credentials."

	if [ -n "$MYSQL_USER_DB" ]; then
		echo "[i] Creating database: $MYSQL_USER_DB"
		mysql --user=root --password=$MYSQL_ROOT_PWD -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_USER_DB\` CHARACTER SET utf8 COLLATE utf8_general_ci; FLUSH PRIVILEGES;"
                mysql --user=root --password=$MYSQL_ROOT_PWD < /root/create_database.sql
		if [ -n "$MYSQL_USER" ] && [ -n "$MYSQL_USER_PWD" ]; then
			echo "[i] Create new User: $MYSQL_USER with password $MYSQL_USER_PWD for new database $MYSQL_USER_DB."
			mysql --user=root --password=$MYSQL_ROOT_PWD -e "GRANT ALL PRIVILEGES ON \`$MYSQL_USER_DB\`.* TO '$MYSQL_USER'@'%' IDENTIFIED BY '$MYSQL_USER_PWD' WITH GRANT OPTION; FLUSH PRIVILEGES;"
		else
			echo "[i] Don\`t need to create new User."
		fi
	else
		if [ -n "$MYSQL_USER" ] && [ -n "$MYSQL_USER_PWD" ]; then
			echo "[i] Create new User: $MYSQL_USER with password $MYSQL_USER_PWD for all database."
			mysql --user=root --password=$MYSQL_ROOT_PWD -e "GRANT ALL PRIVILEGES ON *.* TO '$MYSQL_USER'@'%' IDENTIFIED BY '$MYSQL_USER_PWD' WITH GRANT OPTION; FLUSH PRIVILEGES;"
		else
			echo "[i] Don\`t need to create new User."
		fi
	fi
	killall mysqld
	sleep 5

fi

echo "[i] Setting end,have fun."
exec "$@"
