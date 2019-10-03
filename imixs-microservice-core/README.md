# Imixs-Microservice - Core

The Imixs-Microserivce core is a library providing the following methods:

 - **AutoRegister** - registers a Imixs-Microservice at a Imixs-Registry Endpoing
 - **IndexService** - creates eventLog entries for a derived solr index each time a document is saved
 
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
