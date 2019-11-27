# Imixs-Microservice - Core

The Imixs-Microserivce core is a library providing the core functionallity for a Imixs-Microservice:

 - **AutoRegister** - registers a Imixs-Microservice at a Imixs-Registry Endpoing
 - **IndexService** - creates eventLog entries for a derived solr index each time a document is saved
 - **Batch-Processing** - allows to process in an asynchronous way
 
The core library can be bundled with a Imixs-Microserice by the following Maven dependency:

	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-microservice-core</artifactId>
		<version>${imixs.microservice.version}</version>
	</dependency> 


## AutoRegister

The Imixs-Registry provides an API for the registration of a Imixs-Microservice. This allows the Imixs-Registry to automatically update the mapping table when a new workflow service is stared or becomes unavailable.

A new Imixs-Microservice instance can be automatically registered with the Imixs-Registry during startup. This prevents the need for to edit the mapping table manually at runtime. A workflow service instance periodically invokes the registration during runtime in order to prevent its registration from expiring.

Find more details in the module [Imixs-Registry](https://github.com/imixs/imixs-microservice/tree/master/imixs-microservice-registry).



## IndexService

The Imixs registry can maintain a derived index across all registered Imixs microservices. This index is a fast access layer to search for running process instances distributed across different Imixs microservices.   

The following environment settings need to be defined to activate the index service:


    IMIXS-REGISTRY_API= service endpoint of a Imixs-Registry
    IMIXS-REGISTRY_INDEX_ENABLED= true

The following environment variables are optional:

	IMIXS-REGISTRY_INDEX_TYPEFILTER - optional type filter (regex) - default: (workitem|workitemarchive) 
	IMIXS-REGISTRY_INDEX_FIELDS - optional item list (default list is defined by schema service)
	


## Asynchronous Batch-Processor

The BatchEventService can be used to process workflow events in an asynchronous batch process. 

A batch event can be defined by the model  workflow result of an event:

	<item name="batch.event.id">[EVENT_ID]</item>

This will create a new eventLog entry with the topic "batch.event". Those eventLog entries are process Asynchronous by the BatchEventService.

 
### Configuration

The BatchEventService runs on a scheduled base defined by the following environment settings:

 - BATCH\_PROCESSOR\_INTERVAL - timeout period in milliseconds
 - BATCH\_PROCESSOR\_INITIALDELAY - To enable the batchPorcessor
 - BATCH\_PROCESSOR\_ENABLED - must be set to true (default=false).
 - BATCH\_PROCESSOR\_DEADLOCK - deadlock in milliseconds (default 1 minute)

To prevent concurrent processes to handle the same workitems the batch process uses a Optimistic lock strategy. The 
expiration time on the lock can be set by the environment variable BATCH\_PROCESSOR\_DEADLOCK.
 	
	
	