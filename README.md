# imixs-microservice
The Imixs-Microservice project encapsulates the Imixs-Workflow Engine into a microservice architecture. This service can be bound to any business application, independent from the technology behind. This allows you to change the business logic without changing a single line of your code. You can control the state of your business process based on a workflow model. Imixs-Microservice sends notifications, logs business transactions and secures your business data.

Imixs-Microservice provides an easy to use REST API. Once deployed you can define your workflow Model within the Eclipse based Imixs-Workflow-Modelling Tool. Through the WebService interface you can create and access process instances through the JSON and XML based WebService inferface.


See also: http://martinfowler.com/articles/microservices.html


 
## Installation
Imixs-Microservice can be deployed on a JEE6 Web Server like GlassFish or JBoss/Wildfly.



# Docker
Imixs-Microservice also provides a docker image. This makes is easy to run Imixs-Microservice in a Docker container.
To build the docker file follow these steps:

1. Build imixs-microservice thrugh maven build tool
2. install the Docker container locally
3. start the microservice


## 1. Build imixs-microservice

First checkout the source from 

>github https://github.com/imixs/imixs-microservice

next run the maven to build the war file for JBoss/Wildfly: 

>mvn clean install -Pwildfly

## 2. Build the Docker container
Next you can create the Docker container proivded by the imixs-microservice project.
Therefor run the docker build with:

> docker build --tag=imixs-microservice .



## 3. Start the imixs-microservice

Finally start the docker container. You need to start a database and the JBoss/Wildfly Applicaiton server.
We use postgreSQL as a database and link this docker container with the imixs-microservice:

First run the Postgres container as:
 
>docker run --name imixs-postgres -e POSTGRES_PASSWORD=imixs -d postgres
 
Next run the imixs-microservice with linking to Postgres

>docker run --link imixs-postgres:imixs-database-host -it -p 8080:8080 -p 9990:9990 imixs-microservice /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0

Thats It!

#Background:

In the installation example before we are first starting a docker container providing a postgres database.
We are linking the docker container 'imixs-postgres'  with the imixs-microservice container.
So wildfly can access the postgres server by the hostame 'imixs-imixs-database-host'. This
hostname is used in the wildfly configuration to connect to the postgres database. 
 
To find the IP address of the imixs-postgres container:

>sudo docker inspect -f '{{ .NetworkSettings.IPAddress }}' imixs-postgres
 
###Run postgres container 
If you want to test the postgres database you can also run the imixs-posgres container with the following 
command which binds postgres to the default port 5432 of your host:

>docker run --name imixs-postgres -p 5432:5432 -e POSTGRES_PASSWORD=imixs -d postgres
 
### Run wildfly container 
To check out only the wildfly server without postgres start the imixs-microservice container with:

>docker run -it -p 8080:8080 imixs-microservice. 

This will start Wildfly without the deployed service. See the next section to run Wildfly togher
with a PostgreSQL Database.


###Stop imixs-postgres container

To stop the running imixs-postgres container use:

>docker stop imixs-postgres 

>docker rm imixs-postgres 