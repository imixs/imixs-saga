# imixs-microservice
The Imixs-Microservice project encapsulates the Imixs-Workflow Engine into a microservice architecture. This service can be bound to any business application, independent from the technology behind. This allows you to change the business logic without changing a single line of your code. You can control the state of your business process based on a workflow model. Imixs-Microservice sends notifications, logs business transactions and secures your business data.

Imixs-Microservice provides an easy to use REST API. Once deployed you can define your workflow Model within the Eclipse based Imixs-Workflow-Modelling Tool. Through the WebService interface you can create and access process instances through the JSON and XML based WebService inferface.


See also: http://martinfowler.com/articles/microservices.html


 
## Installation
Imixs-Microservice is a Java Enterprise Web Module which can be deployed on any JEE6 Web Server like GlassFish or JBoss/Wildfly.
The Web Modul includes the Imxis-Workflow Rest API. You can run the web module as a single application, or you can bundle the module which your own JEE application in a EAR file. 
To find detailed information about how to deploy Imixs-Workflow in a JEE Environment see: http://www.imixs.org


# Docker
Imixs-Microservice provides a docker image. This makes is easy to run Imixs-Microservice in a Docker container.
To build the docker file follow these steps:

1. Build imixs-microservice with maven 
2. Build the Docker container 
3. Start the microservice


### 1. Build imixs-microservice

First checkout the source from the git repository

>git@github.com:imixs/imixs-microservice.git

next switch into your local git workspace and run the following maven command
to build the war file for JBoss/Wildfly: 

>mvn clean install -Pwildfly -DskipTests

### 2. Build the Docker container
Next you can create the Docker container provided by the imixs-microservice project.
There for run the docker build command again from your git workspace with:

> docker build --tag=imixs-microservice .

(be carefull about the ending '.')

### 3. Start the imixs-microservice

Finally you can start the docker container. 
The Docker container provided with Imixs-Microservice runs with WildFly 9.0.2 and PostgreSQL. To start the container use the Docker run command: 


>docker run --rm -ti -p 8080:8080 -p 9990:9990 imixs-microservice
 
After the server was started, you can run Imixs-Microservice from your web browser: 

>http://localhost:8080/imixs-microservice

*Thats It!*

Find more about Docker in the wiki: https://github.com/imixs/imixs-microservice/wiki/docker

#Using 'curl' for testing:

You can use the commandline tool curl to test the Imixs-Microservice.
Here are some examples. 

NOTE: you need to authenticate against the rest service. Use the default user 'admin' and
the default password 'adminadmin' to test:

Deploy a new BPMN model 

>curl --user admin:adminadmin --request POST -Tticket.bpmn http://localhost:8080/imixs-microservice/model/bpmn

Request the deployed Model Version

>curl --user admin:adminadmin -H "Accept: application/xml" http://localhost:8080/imixs-microservice/model/

To request the current Worklist for the user 'admin' use:

>curl --user admin:adminadmin  http://localhost:8080/imixs-microservice/workflow/worklist

to get the same result in JSON format:

>curl --user admin:adminadmin -H "Accept: application/json"  http://localhost:8080/imixs-microservice/workflow/worklist


The next example shows how to post a new Workitem in JSON Format. The request post a JSON structure for a new workitem with the $modelVerson 1.0.0 , ProcessID 10 and ActivityID 10. 
The result is the new process instance controlled by Imixs-Workflow

>curl --user admin:adminadmin -H "Content-Type: application/json" -d '{"item":[ {"name":"type","value":{"@type":"xs:string","$":"workitem"}}, {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0.0"}}, {"name":"$processid","value":{"@type":"xs:int","$":"10"}}, {"name":"$activityid","value":{"@type":"xs:int","$":"10"}}, {"name":"txtname","value":{"@type":"xs:string","$":"test-json"}}]}' http://localhost:8080/imixs-microservice/workflow/workitem.json




Find more examples in the wiki: https://github.com/imixs/imixs-microservice/wiki/curl
