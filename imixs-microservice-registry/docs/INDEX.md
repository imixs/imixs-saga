# Indexing

The Imixs-Registry supports a derived index over all registered Imixs-Microserives. This index allows a client to search for running process instances, regardless of where those process instances run. The derived index is based on Apache Solr.
Apache Solr is a highly reliable, scalable and fault tolerant search engine. Solr is providing distributed indexing and replication and can be deployed in large distributed cloud environments.

The Imixs-Registry periodically updates the index based on EventLog entries written by each Imixs-Microserivce. This mechanism ensures that only committed data is indexed. The index is updated periodically so it runns behind the origin transaction. To get a live view to a Imixs-Microservice the service can be search directly by the Rest-API. 

## Setup

To setup the derived index both, the Imixs-Microservice and the Imixs-Registry need to be configured. 

 
### Setup Imixs-Microservice

The following environment settings need to be defined in a Imixs-Microservice to activate the indexing:

    IMIXS-REGISTRY_API= service endpoint of a Imixs-Registry
    IMIXS-REGISTRY_INDEX_ENABLED= true

The following environment variables are optional:

	IMIXS-REGISTRY_INDEX_TYPEFILTER - optional type filter (regex) - default: (workitem|workitemarchive) 
	IMIXS-REGISTRY_INDEX_FIELDS - optional item list (default list is defined by schema service)

The following example shows a docker-compose setup of the Imxis-Microservice with an activated derive index:       
	
	
### Setup Imixs-Registry 	
The Imixs-Registry need to define the following envirnment settigns:

	SOLR_API: servcie endpoint of the solr service   
	INDEX_INTERVAL: refresh interval in milliseconds 
      
      
### Docker-Compose       
The following example shows a docker-compose setup of a Imixs-Microservice and a Imxis-Registry with an activated derive index:       


	 ...
	  service1:
	    image: imixs/imixs-microservice
	    environment:
	      POSTGRES_USER: "postgres"
	      POSTGRES_PASSWORD: "adminadmin"
	      POSTGRES_CONNECTION: "jdbc:postgresql://db/workflow"
	      IMIXS_MODEL: "/home/imixs/model/ticket-en-1.0.0.bpmn"
	      MODEL_DEFAULT_DATA: "/home/imixs/model/ticket-en-1.0.0.bpmn"
	      IMIXS_REGISTRY_API: "http://registry:8080/api"
	      IMIXS_REGISTRY_AUTH_METHOD: "BASIC"
	      IMIXS_REGISTRY_AUTH_USERID: "admin"
	      IMIXS_REGISTRY_AUTH_SECRET: "adminadmin"
	      IMIXS_REGISTRY_INTERVAL: 10000
	      IMIXS_REGISTRY_INDEX_ENABLED: "true"
	      IMIXS-REGISTRY_INDEX_FIELDS: ""
	      IMIXS_API: "http://app:8080/api"
	    ports:
	      - "8080:8080"
	      - "9990:9990"
	      - "8787:8787"    
	    volumes:
	      - ~/git/imixs-microservice/imixs-microservice-app/src/model/:/home/imixs/model/
	    depends_on:
	      - registry
	
	  registry:
	    image: imixs/imixs-registry
	    environment:
	      TZ: "Europe/Berlin"
	      SOLR_API: "http://registry-solr:8080"    
	      INDEX_INTERVAL: 60000
	      IMIXS_REGISTRY_AUTH_METHOD: "BASIC"
	      IMIXS_REGISTRY_AUTH_USERID: "admin"
	      IMIXS_REGISTRY_AUTH_SECRET: "adminadmin"      
	    ports:
	      - "8081:8080"
	      - "9991:9990"
	      - "8788:8787"    

