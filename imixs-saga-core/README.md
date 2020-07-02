# Imixs-SAGA - Core

The Imixs-SAGA core is a library providing the core functionallity for a Imixs-SAGA:

 - **AutoRegister** - registers a Imixs-Microservice at a Imixs-Registry Endpoint
 - **IndexService** - creates eventLog entries for a derived solr index each time a document is saved

 
The core library can be bundled with a Imixs-Microserice by the following Maven dependency:

	<dependency>
		<groupId>org.imixs.workflow</groupId>
		<artifactId>imixs-saga-core</artifactId>
		<version>${imixs.saga.version}</version>
	</dependency> 


## AutoRegister

The Imixs-Registry provides an API for the registration of a Imixs-Microservice. This allows the Imixs-Registry to automatically update the mapping table when a new workflow service is stared or becomes unavailable.

A new Imixs-Microservice instance can be automatically registered with the Imixs-Registry during startup. This prevents the need for to edit the mapping table manually at runtime. A workflow service instance periodically invokes the registration during runtime in order to prevent its registration from expiring.

Find more details in the module [Imixs-Registry](https://github.com/imixs/imixs-sgaa/tree/master/imixs-saga-registry).



## IndexService

The Imixs registry can maintain a derived index across all registered Imixs microservices. This index is a fast access layer to search for running process instances distributed across different Imixs microservices.   

The following environment settings need to be defined to activate the index service:


    IMIXS-REGISTRY_API= service endpoint of a Imixs-Registry
    IMIXS-REGISTRY_INDEX_ENABLED= true

The following environment variables are optional:

	IMIXS-REGISTRY_INDEX_TYPEFILTER - optional type filter (regex) - default: (workitem|workitemarchive) 
	IMIXS-REGISTRY_INDEX_FIELDS - optional item list (default list is defined by schema service)
	


	