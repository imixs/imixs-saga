# Imixs-Microservice Util

The imixs-microservice-util.jar contains shared utility classes used by the Imixs-Microservice and the Imixs-Registry. For example the SchemaService provides the same index schema information for the RegistryIndexService as also for the SolrUpdateService used by the imixs-registry. 

You add the following maven dependency to use the util library in your project:

		</dependency>
			<dependency>
			<groupId>org.imixs.workflow</groupId>
			<artifactId>imixs-microservice-util</artifactId>
			<version>${version}</version>
		</dependency>