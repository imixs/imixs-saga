# imixs-microservice

Imixs-Workflow is an open source workflow engine for human-centric business process management (BPM). Human-centric BPM means to support human skills and activities by a task orientated workflow-engine.

The Imixs-Microservice project encapsulates the [Imixs-Workflow Engine](http://www.imixs.org) into a RESTful web service interface. The Imixs-Microservice can be bound to any business application, independent from the technology behind. In this architectural style the business logic can be changed without changing a single line of code. 

A business process can be described by a BPMN Model using the Eclipse based [Imixs-Workflow-Modelling Tool](http://www.imixs.org/doc/modelling/index.html). Business data can be created, published and processed either in XML or JSON format.


See also [Marin Fowlers Blog)(http://martinfowler.com/articles/microservices.html) for further information about the concepts of microservices.


 
## Installation

Imixs-Microservice is based on the Java EE specification and can be deployed into any Java EE compatible application server like [JBoss/Wildfly](http://wildfly.org/), GlassFish or [Payara](http://www.payara.fish/). See the [Imixs-Workflow deployment guide](http://www.imixs.org/doc/deployment/index.html) for further information.

### Docker
Imixs-Microservice provides also a docker image. This makes is easy to run the Imixs-Microservice in a Docker container.
If you do not want to install the Imixs-Microservice by yourself you can skip this chapter and jump directly to the Docker section below.


### Imixs-Admin Client

The [Imixs-Admin Client](http://www.imixs.org/doc/administration.html) is a web tool to administrate a running instance of the Imixs-Worklfow engine. The Imixs-Admin tool can be deployed in addition to the Imixs-Microservce. 
The Docker Image already contains the latest version of the [Imixs-Admin Client](http://www.imixs.org/doc/administration.html). 

<img src="imixs-admin-client-01.png" width="800" /> 


### Initalize User DB
Imixs-Microservice expects a database pool with the JNDI name 'imixs-microservice'. You can use any database vendor but you need to configure the JDBC database pool in your application server befor your start the deployment. 
After the first deployment the database is initialized automatically with a default model (ticket.bpmn) and the default user 'admin' with the default password 'adminadmin'. 

You can initialize the internal UserDB manually by calling the setup URL

[http://[YOURSERVER]/imixs-microservice/setup](http://localhost:8080/imixs-microservice/setup)

You can add additional accounts or change the default account later using the 'User-REST service' interface.

### How to Deploy a BPMN Model
After you Imixs-Workfow Microservice is up and running you can deploy your own BPMN Model. A workflow model can be created using the [Imixs-BPMN eclipse Plugin](http://www.imixs.org/doc/modelling/index.html). A workflow Model can be deployed into the Imixs-Microservice using the 'Model-REST service' interface. You can deploy the default 'Ticket Workflow' using the following curl command: 

    curl --user admin:adminadmin --request POST -Tticket.bpmn http://localhost:8080/imixs-microservice/model/bpmn

The example model is included in the Imixs-Microservice project located at: /src/main/resources/ticket.bpmn

### Verify Model

To verify if the model was deployed successfully you can check the deployed model version with the Rest API:

    http://[YOURSERVER]/imixs-microservice/model

If no model version is yet deployed, you can create and upload a new BPMN Model using the Imixs-BPMN-Modeller.

## How to Manage a Process Instance

The following section includes some short examples how to create, process and verify a workflow instance managed by the Imxis-Workflow Microservicve. See the [Imixs-Workflow Project](http://www.imixs.org/doc/restapi/index.html) for more information.

**NOTE:** you need to authenticate against the rest service. Use the default user 'admin' and
the default password 'adminadmin' to test

###Open the Task List
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

The Imixs-Microservice project provide a set of JUnit Tests. These tests can be used also as a starting point to see how the RestService API works.


## User Management

The Imixs-Microservice provides a user managemetn service to store user credentials into the existing database connection (jdbc/imixs-microservice). This service is recommended for test and development only. 

The userManagement service provides a rest API to create a new user or update an existing account. 
The service expects a JSON structure with the user id, groups and password:

	{"item":[    
		  		{"name":"type","value":{"@type":"xs:string","$":"profile"}},     
		  		{"name":"txtname","value":{"@type":"xs:string","$":"USER_NAME"}},     
		  		{"name":"txtpassword","value":{"@type":"xs:string","$":"USER_PASSWORD"}},     
		  		{"name":"txtgroups","value":{"@type":"xs:string","$":"IMIXS-WORKFLOW-Author"}}
		  ]}

Example curl command:

	curl --user admin:adminadmin -H "Content-Type: application/json" -d \
	       '{"item":[  \
	       	    {"name":"type","value":{"@type":"xs:string","$":"profile"}}, \
	       	    {"name":"txtname","value":{"@type":"xs:string","$":"USER_NAME"}}, \
	       	    {"name":"txtpassword","value":{"@type":"xs:string","$":"USER_PASSWORD"}}, \
	       	    {"name":"txtgroups","value":{"@type":"xs:string","$":"IMIXS-WORKFLOW-Author"}} \
	       	 ]}' \
	       	 http://localhost:8080/imixs-microservice/user






<br /><br /><img src="small_h-trans.png" />


Imixs-Microservice provides a Docker Container to be used to run the service on a Docker host. 
The docker image is based on the docker image [imixs/wildfly](https://hub.docker.com/r/imixs/wildfly/).

To run Imixs-Microservice in a Docker container, the container need to be linked to a postgreSQL database container. The database connection is configured in the Wildfly standalone.xml file and can be customized to any other database system. 

### 1. Starting a Postgress Container
To start a postgreSQL container run the following command:
	
	docker run --name imixs-workflow-postgres -d \
	       -e POSTGRES_DB=imixs-microservice \
	       -e POSTGRES_PASSWORD=adminadmin postgres:9.6.1
 
This command will start a [Postgres container](https://hub.docker.com/_/postgres/) with a database named 'imixs-microservice'. This container can be liked to the Imixs-Microservice Container.

 
### 2. Starting Imixs-Workflow

After the postgres database container started, you can run the imixs/imixs-microservice container with a link to the postgres container using the following command:    

	docker run --name="imixs-workflow" \
			-p 8080:8080 -p 9990:9990 \
           -e WILDFLY_PASS="adminadmin" \
           --link imixs-workflow-postgres:postgres \
           imixs/imixs-microservice

The link to the postgres container allows the wildfly server to access the postgress database via the host name 'postgres' which is mapped by the --link parameter.  This host name is used for the data-pool configuration in the standalone.xml file of wildfly.  

You can access the Imixs-Microservice from you web browser at the following url:

http://localhost:8080/imixs-microservice

  
More details about the imixs/wildfly image, which is the base image for Imixs-Workflow, can be found [here](https://hub.docker.com/r/imixs/wildfly/).



# docker-compose
You can simplify the start process of Imixs-Workflow by using 'docker-compose'. 
The following example shows a docker-compose.yml file for imixs-workflow:

	postgres:
	  image: postgres:9.6.1
	  environment:
	    POSTGRES_PASSWORD: adminadmin
	    POSTGRES_DB: imixs-microservice
	
	imixsworkflow:
	  image: imixs/imixs-microservice
	  environment:
	    WILDFLY_PASS: adminadmin
	  ports:
	    - "8080:8080"
	    - "9990:9990"
	  links: 
	    - postgres:postgres


Run start imixs-wokflow with docker-compose run:

	docker-compose up

Take care about the link to the postgres container. The host name 'postgres' is needed to be used in the standalone.xml configuration file in wildfly to access the postgres server for the database pool configuration.

# Contribute
General information about Imixs-Workflow can be found the the [project home](http://www.imixs.org). The sources for this docker image are available on [Github](https://github.com/imixs-docker/imixs-workflow). Please report any issues.


If you have any questions concerning the Imixs-Microservice please see the [Imixs-Microservice Project on GitHub](https://github.com/imixs/imixs-microservice)




# Development

## 1. Build the image

To build the artifact run the maven command:

    mvn clean install -DskipTests
    
To build the docker image run:
    
	docker build --tag=imixs/imixs-microservice .
	

## 3. Development

During development the docker container can be used with mounting an external deployments/ folder:

	docker run --name="imixs-microservice" -d -p 8080:8080 -p 9990:9990 \
         -e WILDFLY_PASS="admin_password" \
         -v ~/git/imixs-microservice/deployments:/opt/wildfly/standalone/deployments/:rw \
         imixs/imixs-microservice
