# imixs-microservice
The Imixs-Microservice project encapsulates the Imixs-Workflow Engine into a microservice architecture. This service can be bound to any business application, independent from the technology behind. This allows you to change the business logic without changing a single line of your code. You can control the state of your business process based on a workflow model. Imixs-Microservice sends notifications, logs business transactions and secures your business data.

Imixs-Microservice provides an easy to use REST API. Once deployed you can define your workflow Model within the Eclipse based Imixs-Workflow-Modelling Tool. Through the WebService interface you can create and access process instances through the JSON and XML based WebService inferface.

 
## Installation
Imixs-Microservice can be deployed on a JEE6 Web Server like GlassFish or JBoss/Wildfly.

See: http://martinfowler.com/articles/microservices.html



## Docker
Imixs-Microservice also provides a docker image. This makes is easy to run Imixs-Microservice in a Docker container.
To build the docker file follow these steps:

  1) checkout the source from github https://github.com/imixs/imixs-microservice

  2) run the maven build

>mvn clean install

3) run the docker build with:

> docker build --tag=imixs-microservice .

4) finally start the docker container with :

>docker run -it -p 8080:8080 imixs-microservice. 

Application will be deployed on the container boot.




### Linking Containers

1) Run the Postgres container as:
 
>docker run --name imixs-postgres -e POSTGRES_PASSWORD=imixs -d postgres

or use the following to access postgress throug port from your host system:

>docker run --name imixs-postgres -p 5432:5432 -e POSTGRES_PASSWORD=imixs -d postgres
 
2) Run the WildFly container, with linking Postgres

>docker run --name mywildfly --link mysqldb:db -p 8080:8080 -d arungupta/wildfly-mysql-javaee7

>docker run --link imixs-postgres:db -it -p 8080:8080 -p 9990:9990 imixs-microservice /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0

 
3) Find the IP address of the WildFly container:

>sudo docker inspect -f '{{ .NetworkSettings.IPAddress }}' mywildfly
 
