Docker + MariaDB 11.4 LTS

#### Environment Variables

* `MARIADB_ROOT_PASSWORD` : root Password (default: "mysql")
* `MARIADB_USER`          : application User (default: "jlawyer")
* `MARIADB_PASSWORD`      : application User Password (default: "jlawyer")
* `MARIADB_DATABASE`      : Database name (default: "jlawyerdb")

#### Build Image

```
$ docker build -t="jlawyerorg/jlawyer-db" .
```

#### Run Container

```
$ docker run --name mariadb -v /var/docker_data/j-lawyer-db/:/var/lib/mysql -d -p 3306:3306 jlawyerorg/jlawyer-db
```

#### Run with Custom Credentials

```
$ docker run --name mariadb -v /var/docker_data/j-lawyer-db/:/var/lib/mysql -d -p 3306:3306 \
  -e MARIADB_ROOT_PASSWORD=secret \
  -e MARIADB_USER=myuser \
  -e MARIADB_PASSWORD=mypassword \
  -e MARIADB_DATABASE=mydb \
  jlawyerorg/jlawyer-db
```

#### Connect to Database

```
$ mariadb -h 127.0.0.1 -P 3306 -u jlawyer -p
```
