# Payara-Micro

This folder holds a custom configuration to build a docker image with Imixs-Microservice based on the official payara/micro image. 

There are several ways to customize the image and add additional launch options. See also [here](https://docs.payara.fish/documentation/payara-micro/configuring/config-sys-props.html)

## Custom domain.xml

With the option --domainConfig, it is possible to define a custom domain.xml file to be used to launch payara-micro.
 This option overrides the default Payara Micro configuration completely. The provided file must conform to the same structure as the domain.xml file in a Payara Server domain.

## Command Options

We use the following additional command options to launch Imixs-Microservice:

 - "--addLibs","/opt/payara/config/postgresql-42.2.5.jar"
   add a postgresql JDBC driver
 - "--domainConfig", "/opt/payara/config/domain.xml"
   specify the custom domain.xml file
 - "--deploymentDir", "/opt/payara/deployments"
   add the default deployment folder location
   


## Build and Launch 

To build the payara-micro Docker image for Imixs-Microservice run:

	$ mvn clean install -Pdocker-payara-micro

you can also buidl a cusotm Docker image with a different tag name:

	$ docker build -f Dockerfile-payara-micro --tag=imixs/imixs-microservice .

to and start the docker container use the docker-compose-payara-micro.yml file

	$ docker-compose -f docker-compose-payara-micro.yml up
	
