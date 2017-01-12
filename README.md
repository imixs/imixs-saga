# imixs-microservice

Imixs-Workflow is an open source workflow engine for human-centric business process management (BPM). Human-centric BPM means to support human skills and activities by a task orientated workflow-engine.

The Imixs-Microservice project encapsulates the [Imixs-Workflow Engine](http://www.imixs.org) into a RESTful web service interface. The Imixs-Microservice can be bound to any business application, independent from the technology behind. In this architectural style the business logic can be changed without changing a single line of code. 

A business process can be described by a BPMN Model using the Eclipse based [Imixs-Workflow-Modelling Tool](http://www.imixs.org/doc/modelling/index.html). Business data can be created, published and processed either in XML or JSON format.


See also [Marin Fowlers Blog)(http://martinfowler.com/articles/microservices.html) for further information about the concepts of microservices.

 
## Installation

Imixs-Workflow is based on the Java EE specification and can be deployed into any Java EE compatible application server like GlassFish or JBoss/Wildfly. See the [deployment guide](http://www.imixs.org/doc/deployment/index.html) for further information.



## Docker
Imixs-Microservice provides also a docker image. This makes is easy to run the Imixs-Microservice in a Docker container.
See the [Imixs Docker Project](https://hub.docker.com/r/imixs/workflow/) for further information.



## Initalize User DB
After the first deployment the database is initialized automatically with a default model (ticket.bpmn) and the default user 'admin' with the default password 'adminadmin'. 

You can initialize the internal UserDB manually by calling the setup URL

[http://[YOURSERVER]/imixs-microservice/setup](http://localhost/imixs-microservice/setup)

You can add additional accounts or change the default account later using the 'User-REST service' interface.

### How to Deploy a BPMN Model
After you have setup the Imixs-Workfow Microservice you can deploy a workflow Model. A workflow model can be created using the [Imixs-BPMN eclipse Plugin](http://www.imixs.org/doc/modelling/index.html). A workflow Model can be deployed into the Imixs-Microservice using the 'Model-REST service' interface. You can deploy the default 'Ticket Workflow' using the following curl command: 

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
     {"name":"$modelversion","value":{"@type":"xs:string","$":"my-model-definition-0.0.2"}},
     {"name":"$processid","value":{"@type":"xs:int","$":"2000"}}, 
     {"name":"$activityid","value":{"@type":"xs:int","$":"1"}}, 
     {"name":"_subject","value":{"@type":"xs:string","$":"Some usefull data.."}}
    ]}  
    
    
The example below shows how to post a new Workitem in JSON Format using the curl command. The request post a JSON structure for a new workitem with the $modelVerson 1.0.0 , ProcessID 10 and ActivityID 10. 

	curl --user admin:adminadmin -H "Content-Type: application/json" -d \
	       '{"item":[ \
	                 {"name":"type","value":{"@type":"xs:string","$":"workitem"}}, \
	                 {"name":"$modelversion","value":{"@type":"xs:string","$":"1.0.1"}}, \
	                 {"name":"txtworkflowgroup","value":{"@type":"xs:string","$":"Ticket"}}, \
	                 {"name":"$processid","value":{"@type":"xs:int","$":"1000"}}, \
	                 {"name":"$activityid","value":{"@type":"xs:int","$":"10"}}, \
	                 {"name":"txtname","value":{"@type":"xs:string","$":"test-json"}}\
	         ]}' \
	         http://localhost:8080/imixs-microservice/workflow/workitem.json


Once you created a new process instance based on a predefined model you got a set of data back form the workflow engine describing the state of your new business object which is now controlled by the workflow engine. This is called the workitem:

    {"item":[
	   {"name":"$uniqueid","value":{"@type":"xs:string","$":"141cb98aecc-18544f1b"}},
	   {"name":"$modelversion","value":{"@type":"xs:string","$":"my-model-definition-0.0.2"}},
	   {"name":"$processid","value":{"@type":"xs:int","$":"2000"}},
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


The Imixs Docker Container '[imixs/imixs-microservice](https://github.com/imixs-docker/imixs-archive)' can be used to run a Imixs-Microservice on a Docker host. The image is based on the docker image [imixs/wildfly](https://hub.docker.com/r/imixs/wildfly/).



## 1. Build the image

	docker build --tag=imixs/imixs-microservice .

## 2. Run with docker-compose
You can run Imixs-Microservice on docker-compose to simplify the startup. 
The following example shows a docker-compose.yml for imixs-office-workflow:

	postgres:
	  image: postgres
	  environment:
	    POSTGRES_PASSWORD: adminadmin
	    POSTGRES_DB: imixs01
	
	office:
	  image: imixs/imixs-microservice
	  environment:
	    WILDFLY_PASS: adminadmin
	  ports:
	    - "8080:8080"
	    - "9990:9990"
	  links: 
	    - postgres:postgres
 
Take care about the link to the postgres container. The host 'postgres' name need to be used in the standalone.xml configuration file in wildfly to access the postgres server.

Run 

	docker-compose up
	

## 2. Running and stopping a container


The container includes a start script which allows to start Wildfly with an admin password to grant access to the web admin console. You can start an instance of wildfly with the Docker run command:

    docker run --name="imixs-microservice" -d -p 8080:8080 -p 9990:9990 -e WILDFLY_PASS="admin_password" imixs/imixs-microservice
    
For further details see the [imixs/wildfly docker image](https://hub.docker.com/r/imixs/wildfly/).



## 3. Development

During development the imixs/imixs-archive docker container can be used with mounting an external deployments/ folder:

	docker run --name="imixs-archive" -d -p 8080:8080 -p 9990:9990 \
         -e WILDFLY_PASS="admin_password" \
         -v ~/git/imixs-microservice/deployments:/opt/wildfly/standalone/deployments/:rw \
         imixs/imixs-microservice

Logfiles can be monitored with 

	docker logs -f imixs-microservice





