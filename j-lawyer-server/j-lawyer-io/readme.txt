build.xml has a post-compile target that uses jaxrs-analyzer to create a swagger.json
swagger.json is then copied to the swagger-ui folder
swagger-ui is deployed as web app by default, available under http://localhost:8080/j-lawyer-io/swagger-ui
authentication required, using a user from jlawyerdb, table security_users
