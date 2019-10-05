# The Service Discovery

A client can start a new process instance based on a specific business event. The service discovery process automatically selects a matching workflow service based on the data provided with the business event and starts a new process instance.

The discovery process supports different modes of a service discovery, with each mode having its pros and cons. The following section describes the modes in detail.

## Service Discover by Modelversion and Start Event

In the first mode, a client calls the Imixs-Registry with a business event containing a modelversion, a taskID and an eventID. The Imixs-Registry selects the corresponding Imixs-Microservice by the given modelversion and forwards the request immediately. 

	{"item":[
	 {"name":"$modelversion","value":{"@type":"xs:string","$":"order-en-1.0"}},
	 {"name":"$taskid","value":{"@type":"xs:int","$":"1000"}}, 
	 {"name":"$eventid","value":{"@type":"xs:int","$":"10"}}, 
	 {"name":"_subject","value":{"@type":"xs:string","$":"...some business data..."}}
	]} 



### Benefits
The discovery mode  by model is  very fast, as the Imixs-Registry can select the corresponding Imixs-Microservice by the provided modelversion. The business event can be forwarded to the Imxis-Microservice immediately. 

### Drawbacks
The drawback of this mode is, that a client has to know the modelversion and its start task. This means that the client has some hard coded information about the available workflow and model versions. A Imixs-Microservice instance can not change or update the model without a redeployment or a reconfiguration of the client code. The client is tightly coupled to a specific Imixs microservice. 


## Service Discover by Workflow Group and Start Event

In this mode, a client calls the Imixs-Registry with a business event containing the name of a workflow group together with a taskID and eventID. The Imixs-Registry selects the corresponding Imixs-Microservice by comparing the registered model versions with there workflow groups, task and event IDs. The request is forwarded to the first matching service instance:

	{"item":[
	 {"name":"$workflowgroup","value":{"@type":"xs:string","$":"Ordering"}},
	 {"name":"$taskid","value":{"@type":"xs:int","$":"1000"}}, 
	 {"name":"$eventid","value":{"@type":"xs:int","$":"10"}}, 
	 {"name":"_subject","value":{"@type":"xs:string","$":"...some business data..."}}
	]} 

### Benefits
The discovery mode  by model is also fast, as the Imixs-Registry can select the corresponding Imixs-Microservice by comparing the registered model informations and automatically selects the latest modelversion. A Imixs-Microservice instance can change or update the modelversions without the need of a redeployment or a reconfiguraiton of the client.

### Drawbacks
A client still has to know the start task and eventID to initialize a new process instance. This is typically a technical detail about the model and need to be hard coded in some kind of configuration or code. This is not a big drawback because a team can agree on unified start tasks and eventIDs in all models. 


## Service Discover by Workflow Group only

In this mode, a client calls the Imixs-Registry with a business event providing only the workflow group without a taskID or eventID. The Imixs-Registry selects the corresponding Imixs-Microservice by comparing the registered model information. The model version, the start task and the start event are evaluated by analyzing the selected models:

	{"item":[
	 {"name":"$workflowgroup","value":{"@type":"xs:string","$":"Ordering"}},
	 {"name":"_subject","value":{"@type":"xs:string","$":"...some business data..."}}
	]} 

### Benefits
The discovery mode selects the corresponding Imixs-Microservice by comparing the registered model information and automatically adds the startTask and startEvent by analyzing the selected model. A client does not know the model details and just need to provide a valid workflow group.

### Drawbacks
A client has no control about the instantiation of a new process instance. The workflow groups must be unique over all model versions. 



## Service Discover by Business Rule

In this mode, a client calls the Imixs-Registry with a business event containing only business information and provides no information about the business process. The Imixs-Registry evaluates the workflow version by applying the provided business data to each registered model. If the evaluated business rule returns a valid task (not an EndTask) the corresponding model information will be added and the request will be forwarded to the corrsponding Imixs-Microservice :

	{"item":[
	 {"name":"_customer","value":{"@type":"xs:string","$":"M. Gloria"}},
	 {"name":"_orderID","value":{"@type":"xs:string","$":"42"}},
	 {"name":"_subject","value":{"@type":"xs:string","$":"...some business data..."}}
	]} 

### Benefits
The discovery mode is fully based on business rules. A client need not knowledge about technical model details and just sends it business data. 
The process designer can set up fine grained business rules describing how new process instances will be initialized. Also different versions of a model can be applied based on individual rules. 

### Drawbacks
A discovery process by business rules is more time-consuming because many models and there business rules may need to be evaluated. 
The client has no control which service and model will be eventually applied to a new process instance. 












