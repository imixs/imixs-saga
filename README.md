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



#Using 'curl' for testing:

You can use the commandline tool curl to test the Imixs-Microservice.
Here are some examples. 

NOTE: you need to authenticate against the rest service. Use the default user 'admin' and
the default password 'adminadmin' to test:

Request the deployed Model Version

>curl --user admin:adminadmin http://localhost:8080/imixs-microservice/model

To request the current Worklist for the user 'admin' use:

>curl --user admin:adminadmin http://localhost:8080/imixs-microservice/workflow/worklist

to get the same result in JSON format:


>curl --user admin:adminadmin -H "Accept: application/json"  http://localhost:8080/imixs-microservice/workflow/worklist


The next example shows how to post a new Workitem in JSON Format. The request post a JSON structure for a new workitem with the $modelVerson 1.0.0 , ProcessID 10 and ActivityID 10. 
The result is the new process instance controlled by Imixs-Workflow

>curl --user admin:adminadmin -H "Content-Type: application/json" -d '{"item":[ {"name":"type","value":{"@type":"xs:string","$":"workitem"}}, {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0.0"}}, {"name":"$processid","value":{"@type":"xs:int","$":"10"}}, {"name":"$activityid","value":{"@type":"xs:int","$":"10"}}, {"name":"txtname","value":{"@type":"xs:string","$":"test-json"}}]}' http://localhost:8080/imixs-microservice/workflow/workitem.json




Find more examples in the wiki: https://github.com/imixs/imixs-microservice/wiki/curl
