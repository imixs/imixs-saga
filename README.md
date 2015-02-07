# imixs-microservice
The Imixs-Microservice project encapsulates the Imixs-Workflow Engine into a microservice architecture. This service can be bound to any business application, independent from the technology behind. This allows you to change the business logic without changing a single line of your code. You can control the state of your business process based on a workflow model. Imixs-Microservice sends notifications, logs business transactions and secures your business data.

Imixs-Microservice provides an easy to use REST API. Once deployed you can define your workflow Model within the Eclipse based Imixs-Workflow-Modelling Tool. Through the WebService interface you can create and access process instances through the JSON and XML based WebService inferface.


See also: http://martinfowler.com/articles/microservices.html


 
## Installation
Imixs-Microservice can be deployed on a JEE6 Web Server like GlassFish or JBoss/Wildfly.



# Docker
Imixs-Microservice also provides a docker image. This makes is easy to run Imixs-Microservice in a Docker container.
To build the docker file follow these steps:

1. Build imixs-microservice with maven 
2. Build the Docker container 
3. Start the microservice


### 1. Build imixs-microservice

First checkout the source from 

>github https://github.com/imixs/imixs-microservice

next switch into your local git workspace and run the following maven command
to build the war file for JBoss/Wildfly: 

>mvn clean install -Pwildfly -DskipTests

### 2. Build the Docker container
Next you can create the Docker container provided by the imixs-microservice project.
There for run the docker build command again from your git workspace with:

> docker build --tag=imixs-microservice .

(be carefull about the ending '.')

### 3. Start the imixs-microservice

Finally start the docker container. 
Imixs-Microservice runs with PostgreSQL. So you need to start two docker containers.
One for the database and one for the JBoss/Wildfly Applicaiton server.
We link the postgres docker container with the imixs-microservice:

... run the Postgres container:
 
>docker run --name imixs-postgres -e POSTGRES_PASSWORD=imixs -d postgres
 
... and now run the imixs-microservice Container with linking to Postgres

>docker run --link imixs-postgres:imixs-database-host -it -p 8080:8080 -p 9990:9990 imixs-microservice /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0

*Thats It!*



#Background:

In the installation example before we are first starting a docker container providing a postgres database.
We are linking the docker container 'imixs-postgres'  with the imixs-microservice container.
So wildfly can access the postgres server by the hostame 'imixs-imixs-database-host'. This
hostname is used in the wildfly configuration to connect to the postgres database. 
 
To find the IP address of the imixs-postgres container run:

>sudo docker inspect -f '{{ .NetworkSettings.IPAddress }}' imixs-postgres
 
###Run postgres container 
If you want to test the postgres database you can also run the imixs-posgres container alone. 

>docker run --name imixs-postgres -p 5432:5432 -e POSTGRES_PASSWORD=imixs -d postgres
 
The command binds postgres to the default port 5432 of your host. So 
you can connect with your prefered sql-tool:
 
### Run wildfly container 
To run only the wildfly server without postgres start the imixs-microservice container with:

>docker run -it -p 8080:8080 imixs-microservice. 

This will start Wildfly without the deployed service. 

###Stop imixs-postgres container

To stop the running imixs-postgres container use:

>docker stop imixs-postgres 

>docker rm imixs-postgres 