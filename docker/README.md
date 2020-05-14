# j-lawyer.org server - dockerized

For testing and "trying out" purposes there is a docker build for the j-lawyer.org server, consisting of both the database and the application.

It can be used like this:

```
wget https://raw.githubusercontent.com/jlawyerorg/j-lawyer-org/master/docker/docker-compose.yaml
docker-compose up
```

The container(s) expose port 8000.

## Health Check

To check whether the container is running, try to launch the status / welcome page: http://localhost:8000/

## Swagger UI

The applications REST API can be browsed and used directly from within the browser: http://localhost:8000/j-lawyer-io/swagger-ui/

## Using the Client

Use the official installers for Windows, Linux or macOS on https://www.j-lawyer.org
Then connect to the docker container using "localhost" and port 8000.