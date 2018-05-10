# imixs-microservice

[Imixs-Workflow](http://www.imixs.org) is an open source workflow engine for human-centric business process management (BPM). Human-centric BPM means to support human skills and activities by a task orientated workflow-engine. The Imixs-Microservice project encapsulates the [Imixs-Workflow Engine](https://www.imixs.org/doc/index.html) into a RESTful web service interface. This Microservice can be bound to any business application, independent from the technology behind. In this architectural style the business logic can be changed without changing a single line of code. 

## BPMN 2.0

The 'Business Process Model and Notation' - BPMN 2.0 is the common standard to describe a business process. BPMN was initially designed to describe a business process without all the technical details of a software system. As an result, a BPMN diagram is easy to understand and a good starting point to talk about a business process with technician as also with management people.

Imixs-Workflow supports BPMN 2.0 and provides a free modelling tool [Imixs-BPMN](https://www.imixs.org/doc/modelling/index.html). Imixs-BPMN takes the full advantage of all the capabilities of BPMN 2.0 and complements them with the features of a powerful workflow engine.


 
## Installation

Imixs-Microservice is based on the Java EE specification and can be deployed into any Java EE application server like [JBoss/Wildfly](http://wildfly.org/), GlassFish or [Payara](http://www.payara.fish/). See the [Imixs-Workflow deployment guide](http://www.imixs.org/doc/deployment/index.html) for further information about deployment and configuration.

### Docker
Imixs-Microservice provides a docker image making it easy to run the Imixs-Microservice out of the box in a Docker container.

To start the service copy the docker-compose.yml file into your workspace and start the containers:

	docker-compose up
	

The docker-compose.yml file defines the application stack consisting of a postgres database and a wildfly container:

	version: '3'
	
	services:
	
	  db:
	    image: postgres:9.6.1
	    environment:
	      POSTGRES_PASSWORD: adminadmin
	      POSTGRES_DB: workflow
	
	  app:
	    image: imixs/imixs-microservice
	    environment:
	      WILDFLY_PASS: adminadmin
	      DEBUG: "true"
	      POSTGRES_USER: "postgres"
	      POSTGRES_PASSWORD: "adminadmin"
	      POSTGRES_CONNECTION: "jdbc:postgresql://db/workflow"
	    ports:
	      - "8080:8080"
	      - "9990:9990" 



### Imixs-Admin Client

The [Imixs-Admin Client](http://www.imixs.org/doc/administration.html) is a web tool to administrate a running instance of the Imixs-Workflow engine. The Imixs-Admin tool can be deployed in addition to the Imixs-Microservice. 

<img src="imixs-admin-client-01.png" width="800" /> 

The Docker Image already contains the latest version of the [Imixs-Admin Client](http://www.imixs.org/doc/administration.html). 

### Initalize Database Connection
The Imixs-Microservice expects a database pool with the JNDI name 'imixs-microservice'. You can use any database vendor, but you need to configure the JDBC database pool in your application server before your start the deployment. 

### Authentication and Authorization

Imixs-Workflow is a human-centry workflow engine which means that each actor need to authenticate against the service to interact. The Worklfow Engine is based on the Java EE and so the authentication is also standardized by the JAAS Specification which supports different authentication solutions like LDAP, Database, SSO and more.  

The default docker installation provides a set of test users which can be used for testing purpose. The test users are stored in a separate user and roles properties file provided by the container. 


| User    | Role                   | Password |
|---------|------------------------|----------|
| admin   | IMIXS-WORKFLOW-Manager | adminadmin |
| alex    | IMIXS-WORKFLOW-Manager | password |
| marty   | IMIXS-WORKFLOW-Author  | password |
| melman  | IMIXS-WORKFLOW-Author  | password |
| gloria  | IMIXS-WORKFLOW-Author  | password |
| skipper | IMIXS-WORKFLOW-Author  | password |
| kowalski| IMIXS-WORKFLOW-Author  | password |
| private | IMIXS-WORKFLOW-Author  | password |
| rico    | IMIXS-WORKFLOW-Author  | password |


You can add additional accounts or change the default account later, by updated the files "imixs-roles.properties" and "imixs-users.properties"..

### How to Deploy a BPMN Model
After the Imixs-Microservice is up and running, you can deploy your own BPMN Model. A workflow model can be created using the [Imixs-BPMN eclipse Plugin](http://www.imixs.org/doc/modelling/index.html). The Model can be deployed into the Imixs-Microservice using the 'Model-REST service' interface. You can deploy the default 'Ticket Workflow' using the following curl command: 

    curl --user admin:adminadmin --request POST -Tticket.bpmn http://localhost:8080/imixs-microservice/model/bpmn

The example model is included in the Imixs-Microservice project located at: /src/model/ticket.bpmn

### Verify Model

To verify if the model was deployed successfully you can check the deployed model version with the Rest API:

    http://localhost:8080/imixs-microservice/model


## How to Manage a Process Instance

The following section includes some short examples how to create, process and verify a workflow instance managed by the Imxis-Workflow Microservice. See the [Imixs-Workflow Rest API](http://www.imixs.org/doc/restapi/index.html) for more information.

**NOTE:** you need to authenticate against the rest service. Use the default user 'admin' and
the default password 'adminadmin' for testing.

### Open the Task List
Each user involved in a business process has a personal task list (called the 'worklist'). You can request the task list for the current user by the following Rest API URL: 

    http://[YOURSERVER]/imixs-microservice/workflow/worklist

The result will include all workitems for the current user.

### Create a new Process Instance
To create a new process instance you can POST a JSON Object to the Imixs-Microservice in the following way

    POST = http://localhost/imixs-microservice/workflow/workitem
				
To create a valid workitem the following attributes are mandatory:

* $modelversion = the version of your model
* $processid = the start process
* $activityid = the activity to be processed

See the following Example:

    {"item":[
     {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0.1"}},
     {"name":"$processid","value":{"@type":"xs:int","$":"1000"}}, 
     {"name":"$activityid","value":{"@type":"xs:int","$":"10"}}, 
     {"name":"_subject","value":{"@type":"xs:string","$":"Some usefull data.."}}
    ]}  
    
    
The example below shows how to post a new Workitem in JSON Format using the curl command. The request post a JSON structure for a new workitem with the $modelVerson 1.0.0 , ProcessID 10 and ActivityID 10. 

	curl --user admin:adminadmin -H "Content-Type: application/json" -d \
	       '{"item":[ \
	                 {"name":"type","value":{"@type":"xs:string","$":"workitem"}}, \
	                 {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0.1"}}, \
	                 {"name":"$processid","value":{"@type":"xs:int","$":"1000"}}, \
	                 {"name":"$activityid","value":{"@type":"xs:int","$":"10"}}, \
	                 {"name":"txtname","value":{"@type":"xs:string","$":"test-json"}}\
	         ]}' \
	         http://localhost:8080/imixs-microservice/workflow/workitem.json


Once you created a new process instance based on a predefined model you got a set of data back form the workflow engine describing the state of your new business object which is now controlled by the workflow engine. This is called the workitem:

    {"item":[
	   {"name":"$uniqueid","value":{"@type":"xs:string","$":"141cb98aecc-18544f1b"}},
	   {"name":"$modelversion","value":{"@type":"xs:string","$":"my-model-definition-0.0.2"}},
	   {"name":"$processid","value":{"@type":"xs:int","$":"1000"}},
	   {"name":"namcreator","value":{"@type":"xs:string","$":"admin"}}, 
	   {"name":"namcurrenteditor","value":{"@type":"xs:string","$":"admin"}}, 
	   {"name":"namowner","value":{"@type":"xs:string","$":"admin"}}, 
	   {"name":"$isauthor","value":{"@type":"xs:boolean","$":"true"}},
	   {"name":"_subject","value":{"@type":"xs:string","$":"JUnit Test-6476"}}, 
	   {"name":"txtworkflowstatus","value":{"@type":"xs:string","$":"Vorlauf"}}, 
	   {"name":"txtworkflowresultmessage","value":{"@type":"xs:string","$":""}}
	  ]}

The workitem includes the attribute '$uniqueid' wich is used ot identify the process instance later. Also workflow information like the current status or the owner is returend by the service.

There are serveral Resouce URIs to request the state of a process instance. Using the $uniqueid returned by the POST method you can request the current status of a single process instance:

    GET = http://localhost/workflow/rest-service/workflow/workitem/[UNIQUEID]

curl command: 

	curl --user admin:adminadmin \
	      -H "Accept: application/json"  \
	       http://localhost:8080/imixs-microservice/workflow/workitem/[UNIQUEID]



To change the status of a process instance you simply need to post uniqueid together with the next workflow activity defined by your workflow model

    POST = http://localhost/workflow/rest-service/workflow/workitem/[UNIQUEID]
 
	 {"item":[
	     {"name":"$uniqueid","value":{"@type":"xs:string","$":"141cb98aecc-18544f1b"}},
	     {"name":"$activityid","value":{"@type":"xs:int","$":"1"}}, 
	     {"name":"_subject","value":{"@type":"xs:string","$":"Some usefull data.."}}
	     {"name":"_customdata","value":{"@type":"xs:string","$":"Some more data.."}}
	   ]}  

Within the object you can define any kind of data to be stored together with the process instance.

### Task Lists

Imixs-Workflow provides several resources to request the task list for specific users or object types. 

To request the Worklist for the current user 'admin' user you can call:

    curl --user admin:adminadmin -H \
         "Accept: application/json" \
         http://localhost:8080/imixs-microservice/workflow/tasklist/creator/admin

Find more details about the Imixs-Rest API [here](http://www.imixs.org/doc/restapi/workflowservice.html)

### JUnit Tests

The Imixs-Microservice project provide a set of JUnit Tests. These tests can be used also as a starting point to learn how the RestService API works.



<br><br><img src="small_h-trans.png">


Imixs-Microservice provides a Docker Container to be used to run the service on a Docker host. 
The docker image is based on the docker image [imixs/wildfly](https://hub.docker.com/r/imixs/wildfly/).

To run Imixs-Microservice in a Docker container, the container need to be linked to a postgreSQL database container. The database connection is configured in the Wildfly standalone.xml file and can be customized to any other database system. 


## 1. Build a local Docker Image
To run the build of a local Docker image:
	
	mvn clean install -DskipTests -Pdocker

 
## 2. Starting the Imixs-Microservice

After the local docker container is build Imixs-Micorservice can be started with a PostgreSQL storage container:

	docker-compose up


The application can be accessed from a web browser at the following url:

http://localhost:8080/imixs-microservice

  
More details about the imixs/wildfly image, which is the base image for Imixs-Workflow, can be found [here](https://hub.docker.com/r/imixs/wildfly/).

	
## Docker for Production

To run the Imixs-Microservice in a Docker production environment the project provides several additional maven profiles:


### docker-build

With the profile '_docker-build_' a docker container based on the current version of Imixs-Microservice is created locally
 
	mvn clean install -Pdocker-build


### docker-push

With the '_docker-push_' profile the current version of Imixs-Microservice can be pushed to a private remote repository:

	mvn clean install -Pdocker-push -Dorg.imixs.docker.registry=localhost:5000

where 'localhost:5000' need to be replaced with the host of a private registry. See the [docker-push command](https://docs.docker.com/docker-cloud/builds/push-images/) for more details.

### docker-hub

Imixs-Microservice is also available on [Docker-Hub](https://hub.docker.com/r/imixs/imixs-microservice/). The public docker images can be used for development and production. If you need technical support please contact [imixs.com](http://www.imixs.com) 




# Contribute
General information about Imixs-Workflow can be found the the [project home](http://www.imixs.org). The sources for this docker image are available on [Github](https://github.com/imixs-docker/imixs-workflow). Please report any issues.


If you have any questions concerning the Imixs-Microservice please see the [Imixs-Microservice Project on GitHub](https://github.com/imixs/imixs-microservice)




# Development

You can use Imixs-Microservice also as a template for custom applications. To build the project code run the maven command:

    mvn clean install -DskipTests
    

During development the docker container environment variable 'DEBUG=true' can be used to run wildfly in a debug mode listening on port 8787.
		

