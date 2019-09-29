# The Imixs-Microservice

[Imixs-Workflow](http://www.imixs.org) is an open source workflow engine for human-centric business process management (BPM). Human-centric BPM means to support human skills and activities by a task orientated workflow-engine. Read more about Imixs-Workflow [here](http://www.imixs.org).

## The Rest API
The Imixs-Microservice project encapsulates the [Imixs-Workflow Engine](https://www.imixs.org/doc/index.html) into a microservice to be bound to any business application, independent from the technology behind. In this architectural style the business logic can be changed without changing a single line of code. 
Imixs-Microservice provides a Rest API to interact with the Imixs-Workflow Engine. See the [Imixs-Workflow Rest API](http://www.imixs.org/doc/restapi/index.html) for more information.

## BPMN 2.0

The 'Business Process Model and Notation' - BPMN 2.0 is the common standard to describe a business process. BPMN was initially designed to describe a business process without all the technical details of a software system. As a result, a BPMN diagram is easy to understand and a good starting point to talk about a business process with technician as also with management people.

Imixs-Workflow supports the BPMN 2.0 standard and provides the free modeling tool - [Imixs-BPMN](https://www.imixs.org/doc/modelling/index.html). Imixs-BPMN takes the full advantage of all the capabilities of BPMN 2.0 and complements them with the features of a powerful workflow engine.

<center><img src="https://github.com/imixs/imixs-workflow/raw/master/screen_001.png" width="700" /></center>

 
# How to Install

Imixs-Microservice is based on the Java EE specification and can be deployed into any Java EE application server like [JBoss/Wildfly](http://wildfly.org/), GlassFish or [Payara](http://www.payara.fish/). See the [Imixs-Workflow deployment guide](http://www.imixs.org/doc/deployment/index.html) for further information. 

### Run with Docker

Imixs-Microservice provides a docker image, making it easy to run the Imixs-Microservice out of the box in a Docker container. This container image can be used for development as also for productive purpose. 
To start the Imixs-Microservice you need to define a container stack with Docker Compose. A docker-compose.yml file is already part of the project. 

To start the service run:

	$ docker-compose up

The Imixs Rest API is available under the following URL

	http://localhost:8080/api

### Authentication and Authorization

Imixs-Workflow is a Human-Centric workflow engine. This means that each actor is authenticated against the service. The Workflow Engine is based on the Java EE and so the authentication is also standardized and supports different authentication solutions like LDAP, Database, SSO and more.  

The default Docker installation provides a set of predefined user accounts which can be used for testing purpose:


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


You can add or change accounts by updating the property files:

 * _imixs-roles.properties_ 
 * _imixs-users.properties_
 
And of course  you can configure a different custom security realm (e.g. LDAP). 



# How to Work With the Imixs-Microservice

After you have started the Imixs-Microservce a full featured Imixs-Workflow instance is ready to be used within your business application project.  The following section includes some short examples how to create, process and verify a Imixs-Workflow instance.

**NOTE:** You need to authenticate against the rest service API. In this examples we will use the default user 'admin' with the default password 'adminadmin'.


### How to Deploy a BPMN Model

Imixs-Microservice automatically loads a default BPMN model from the location defined by the environment variable 'IMIXS_MODEL' during the startup. You can also add or update a model created with [Imixs-BPMN](http://www.imixs.org/doc/modelling/index.html) during runtime via the Imixs Rest API. See the following curl command: 

    curl --user admin:adminadmin --request POST -Tticket-en-1.0.0.bpmn http://localhost:8080/api/model/bpmn

An example model is included in the Imixs-Microservice project located at: /src/model/ticket-en-1.0.0.bpmn

**NOTE:** cURL isn't installed in Windows by default. See the [Use Curl on Windows](https://stackoverflow.com/questions/9507353/how-do-i-install-and-use-curl-on-windows) discussion on stackoverflow.
 
 
### Verify Model

To verify if the model was deployed successfully you can check the model repository form your web browser:

    http://localhost:8080/api/model
    


### Create a new Process Instance

To create a new process instance you can POST a JSON Object to the Imixs-Microservice Rest API: 

    POST = http://localhost:8080/api/workflow/workitem
				
To create a valid workitem the following attributes are mandatory:

* $modelversion = the version of your model
* $taskid = the start task in your model
* $eventid = the initial event to be processed by the Imixs-Worklfow engine

See the following Example:

    {"item":[
     {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0"}},
     {"name":"$taskid","value":{"@type":"xs:int","$":"1000"}}, 
     {"name":"$eventid","value":{"@type":"xs:int","$":"10"}}, 
     {"name":"_subject","value":{"@type":"xs:string","$":"...some business data..."}}
    ]}  
    

The example below shows how to post a new Workitem in the JSON format using the curl command. The request creates a new process instance with the $modelVerson 1.0, TaskID 10 and the inital EventID 10. 

	$ curl --user admin:adminadmin -H "Content-Type: application/json" -H 'Accept: application/json' -d \
	       '{"item":[ \
	                 {"name":"type","value":{"@type":"xs:string","$":"workitem"}}, \
	                 {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0"}}, \
	                 {"name":"$taskid","value":{"@type":"xs:int","$":"1000"}}, \
	                 {"name":"$eventid","value":{"@type":"xs:int","$":"10"}}, \
	                 {"name":"txtname","value":{"@type":"xs:string","$":"test-json"}}\
	         ]}' \
	         http://localhost:8080/api/workflow/workitem.json


Once you created a new process instance based on a predefined model you got a set of data back form the workflow engine describing the state of your new business object which is now controlled by the workflow engine. This is called the workitem:

	{"document":[
	  {"item":[
	    {"value":[1556380031038],"name":"$created"},
	    {"value":["admin"],"name":"$creator"},
	    {"value":["admin"],"name":"$editor"},
	    {"value":[0],"name":"$eventid"},
	    {"value":[true],"name":"$isauthor"},
	    {"value":["1.0"],"name":"$modelversion"},
	    {"value":[1556380031038],"name":"$modified"},
	    {"value":[1100],"name":"$taskid"},
	    {"value":["51f3c349-06f5-4bda-ba10-d50bcb9ec0bf"],"name":"$uniqueid"},
	    {"value":["Ticket"],"name":"$workflowgroup"},
	    {"value":["Open"],"name":"$workflowstatus"},
	    {"value":["test-json"],"name":"txtname"},
	    {"value":["workitem"],"name":"type"}
	  ]}
	]}



The workitem includes the attribute '$uniqueid' which is used to identify the process instance later. Also workflow information like the current status or the editor is returned by the service.

There are several Resource URIs to request the state of a process instance. Using the $uniqueid returned by the POST method you can request the current status of a single process instance:

    GET = http://localhost:8080/api/workflow/workitem/[UNIQUEID]

curl command: 

	curl --user admin:adminadmin \
	      -H "Accept: application/json"  \
	       http://localhost:8080/api/workflow/workitem/[UNIQUEID]



To change the status of a process instance you simply need to post an updated version of your process instance together with the event to be processed:

    POST = http://localhost:8080/api/workflow/workitem/[UNIQUEID]
 
	 {"item":[
	     {"name":"$uniqueid","value":{"@type":"xs:string","$":"141cb98aecc-18544f1b"}},
	     {"name":"$eventid","value":{"@type":"xs:int","$":"1"}}, 
	     {"name":"_subject","value":{"@type":"xs:string","$":"...some other business data..."}}
	     {"name":"_customdata","value":{"@type":"xs:string","$":"...some additional data..."}}
	   ]}  

You can define any kind of business data to be stored together with the process instance.


### Open the Task List
Each user involved in a business process has a personal task list (called the 'worklist'). You can request the task list for the current user by the following Rest API URL: 

    http://[YOURSERVER]/api/workflow/worklist

The result will include all workitems for the current user.


### Task Lists

Imixs-Workflow provides several additional resources to request the task list for specific users or object types. 

To request the Worklist for the current user 'admin' user you can call:

    curl --user admin:adminadmin -H \
         "Accept: application/json" \
         http://localhost:8080/api/workflow/tasklist/creator/admin

Find more details about the Imixs-Rest API [here](http://www.imixs.org/doc/restapi/workflowservice.html). 



# <img src="https://github.com/imixs/imixs-microservice/raw/master/small_h-trans.png">

Imixs-Microservice provides a docker image, making it easy to run the Imixs-Microservice out of the box in a Docker container. This container image can be used for development as also for productive purpose. To start the Imixs-Microservice in a docker container you can define a container stack with Docker Compose. Docker Compose is a tool for defining and running a stack of multiple Docker containers.

The following docker-compose.yml file defines the application stack consisting of a PostgreSQL database and a Imixs-Microservice service:


	version: '3.3'	
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
	      POSTGRES_USER: "postgres"
	      POSTGRES_PASSWORD: "adminadmin"
	      POSTGRES_CONNECTION: "jdbc:postgresql://db/workflow"
	      IMIXS_MODEL: "ticket-en-1.0.0.bpmn"
	    ports:
	      - "8080:8080"



To start the service run:

	$ docker-compose up


The application can be accessed from a web browser at the following URL:

	http://localhost:8080/

The image _imixs/imixs-microservice_ is based on the Docker image _imixs/wildfly_. You can find details about the base image [here](https://hub.docker.com/r/imixs/wildfly/).

### Provide a Model

You can define a model file to be updloaded during startup of the service. See the following configuration which defines a custom model file to be loaded:

	...
	  app:
	    image: imixs/imixs-microservice
	    environment:
	      WILDFLY_PASS: adminadmin
	      DEBUG: "true"
	      POSTGRES_USER: "postgres"
	      POSTGRES_PASSWORD: "adminadmin"
	      POSTGRES_CONNECTION: "jdbc:postgresql://db/workflow"
	      IMIXS_MODEL: "/home/imixs/model/my-model-1.0.0.bpmn"
	    ports:
	      - "8080:8080"
	      - "9990:9990"
	      - "8787:8787"
	    
	    volumes:
	      - ~/git/imixs-microservice/src/model/:/home/imixs/model/
 

In this example the model project directory is mapped as a volume to the service instance /home/imixs/model/

You can initialize provided models by calling the Rest API URL:

	 http://localhost:8080/api/setup

This will automatically reload the models from the defined location. 

## How to Build a Local Docker Image

To build the Docker image locally from sources run:
	
	$ mvn clean install -Pdocker

	
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

Imixs-Microservice is also available on [Docker-Hub](https://hub.docker.com/r/imixs/imixs-microservice/). The public docker images can be used for development and production. If you need technical support please contact [imixs.com](https://www.imixs.com/contact/) 


# Development

The Imixs-Microservice project also includes a docker-compose-dev profile with additional settings and services. To run the Imixs-Microservice in developer mode run:

	$ docker-compose -f docker-compose-dev.yml up

   
### The Imixs-Admin Client

In the developer mode the service provides an additional admin service. The [Imixs-Admin Client](http://www.imixs.org/doc/administration.html) is a web tool to administrate a running instance of the Imixs-Workflow Engine. The Imixs-Admin tool is automatically deployed when running Imixs-Microservice based on the official Docker Container. 

<img src="https://github.com/imixs/imixs-microservice/raw/master/screen_imixs-admin-client-01.png" width="800" /> 

You can open the Imixs-Admin Client from your browser with at the following location:

	http://localhost:8888/

The connect URL to connect the Imixs-Admin Tool with your microservice is _http://app:8080/api_

To learn more about all the Imixs-Admin client, see the [official documentation](http://www.imixs.org/doc/administration.html). 


### Build from Sources

You can use Imixs-Microservice also as a template for a custom workflow project. To build the project from the source code run the maven command:

    $ mvn clean install
 		
To build the image run:

	$ mvn clean install -Pdocker 		

You can also map a deployment directory for hot-deployment:

    ...
    volumes:
        - ~/git/imixs-microservice/src/docker/deployments:/opt/wildfly/standalone/deployments/
    ...



### Extend the Imixs-Micorsevice

If you plan to extend the Imixs-Microservice you just need to setup a Maven Overlay Project:


	
	...
	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-war-plugin</artifactId>
				<version>2.4</version>
				<configuration>
					<webResources>
						<resource>
							<filtering>true</filtering>
							<!-- this is relative to the pom.xml directory -->
							<directory>${custom.webResources}</directory>
							<includes>
								<include>**/WEB-INF/*</include>
								<include>/src/main/resources/*</include>
								<!-- include any other file types you want to filter -->
							</includes>
						</resource>
					</webResources>
					<overlays>
						<overlay>
							<groupId>org.imixs.workflow</groupId>
							<artifactId>imixs-microservice</artifactId>
						</overlay>
					</overlays>
				</configuration>
			</plugin>			
		</plugins>
		....
	</build>
	.....


This will result in a new Microservice Project including the Imxixs-Microservice Core API. You can customize this project or add additional functionallity like Plug-Ins or Adapter Classes.

	

### Debug

During development the wildfly runs in a debug mode listening on port 8787.


### Initialize a Database Connection

The Imixs-Microservice expects a JPA database pool with the JNDI name 'imixs-microservice'. You can use any SQL database vendor. Just configure the JDBC database pool in your application server before your start the deployment. 

The _docker-compose.yml_ provided by this project already defines such a stack configuration located in _/src/docker/conf/standalone.xml/_


### The Imixs Rest Client

To access the Imixs-Microservice form a Java application you can use the Imixs-Workflow RestClient provided by the [Melman Project](https://github.com/imixs/imixs-melman).

### JUnit Tests

The Imixs-Microservice project provide a set of JUnit Tests. These tests can be used also as a starting point to learn how the RestService API works.


# Registry

The Imixs-Microservice project provides a registry service for registration and discovery of Imixs-Microservicess within a microservice architecture. This allows a client to start a new process instance within a microservice architecture without knowning the details about a concrete workflow instance. 

Find details in the [Imixs-Registry](./tree/master/imixs-microservice-registry) sub project.


# Monitoring

You can use a monitoring tool chain to monitor and analyze the behavior of your microservice. The tool chain in Imixs-Microservice is build up on [Prometheus](https://prometheus.io/) and [Grafana](https://grafana.com/). 

To start the tool chain run:

	$ docker-compose -f docker-compose-prometheus.yml up

This Docker Stack includes an additonal prometheus service and Grafana Service which allows you to monitor and analyze your business case. 
You can access the Grafana from your web browser:

	http://localhost:3000

To configure the prometheus data source just add the Prometheus service endpoint in Grafana:

	http://prometheus:9090/
	
<img src="https://github.com/imixs/imixs-microservice/raw/master/screen_monitoring_grafana.png" width="800" /> 	


# Contribute

General information about Imixs-Workflow can be found on the [project site](http://www.imixs.org). The docker image is available on [Docker Hub](https://hub.docker.com/u/imixs).

If you have any questions concerning the Imixs-Microservice please see the [Issue Tracker on Github](https://github.com/imixs/imixs-microservice/issues)




