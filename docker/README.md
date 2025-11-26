# j-lawyer.org server - dockerized

For testing and "trying out" purposes there is a docker build for the j-lawyer.org server, consisting of both the database and the application.

It can be used like this:

```
wget https://raw.githubusercontent.com/jlawyerorg/j-lawyer-org/master/docker/docker-compose.yaml
wget https://raw.githubusercontent.com/jlawyerorg/j-lawyer-org/master/docker/run.sh
sh run.sh
```

The container(s) expose port 8000.
The database schema is named "jlawyerdb" and can be accessed by a user jlawyer:jlawyer.

There are two volumes that are kept on the host:

* /var/docker_data/j-lawyer-data/ contains documents, templates etc.
* /var/docker_data/j-lawyer-db/ contains the MariaDB database

## Health Check

To check whether the container is running, try to launch the status / welcome page: http://localhost:8000/

## Swagger UI

The applications REST API can be browsed and used directly from within the browser: http://localhost:8000/j-lawyer-io/swagger-ui/
For authentication, use user "admin" with password "a".

## Using the Client

Use the official installers for Windows, Linux or macOS on https://www.j-lawyer.org
Then connect to the docker container using "localhost" and port 8000.

For authentication, use user "admin" with password "a".

## Restoring a backup into a Docker environment

Launch the Backup Manager application. Java 8 or later is required.

    sudo java -jar j-lawyer-backupmgr-1.10.0.jar -console

Provide required parameters (see j-lawyer.org documentation for details).

Once the restore has finished, update file system permissions.

    sudo docker exec -u root -it docker_server_1 /bin/bash
    chown -R jboss:jboss /opt/jboss/j-lawyer-data/

## Docker trouble shooting

### Viewing logs

Retrieve the containers ID:

    sudo docker container ls
    sudo docker logs -f docker_server_1

### Opening a shell for a container:

    sudo docker exec -it <container_name_or_id> /bin/bash

or as root

    sudo docker exec -u root -it <container_name_or_id> /bin/bash

### Updating

    sudo docker-compose pull
    sudo docker-compose down
    sudo docker-compose up -d

### Connecting to MariaDB running inside a container

Find its IP:

    sudo docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' docker_db_1

Then connect as follows:

    mariadb -h <IP> -P 3306 -u jlawyer -p

