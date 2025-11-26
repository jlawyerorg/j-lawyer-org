FROM mariadb:11.4

LABEL maintainer="j-dimension <info@j-lawyer.org>"

# UTF-8 Konfiguration
ENV MARIADB_ROOT_PASSWORD=mysql
ENV MARIADB_DATABASE=jlawyerdb
ENV MARIADB_USER=jlawyer
ENV MARIADB_PASSWORD=jlawyer
ENV LANG=C.UTF-8

# Custom MariaDB Konfiguration
COPY ./my.cnf /etc/mysql/conf.d/jlawyer.cnf
COPY ./create_database.sql /docker-entrypoint-initdb.d/

EXPOSE 3306
