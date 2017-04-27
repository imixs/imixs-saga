FROM imixs/wildfly:latest

# Imixs-Microservice Version 1.0.2
MAINTAINER ralph.soika@imixs.com

# add configuration files
COPY ./src/docker/imixsrealm.properties ${WILDFLY_CONFIG}/
COPY ./src/docker/standalone.xml ${WILDFLY_CONFIG}/

# Install Imixs-Admin Client
RUN wget https://github.com/imixs/imixs-admin/releases/download/4.2.1/imixs-admin-4.2.1.war \
 && mv imixs-admin-4.2.1.war $WILDFLY_DEPLOYMENT  

# Install Imixs-Microservice
RUN wget https://github.com/imixs/imixs-microservice/releases/download/imixs-workflow-microservice-1.5.2/imixs-microservice.war \
 && mv imixs-microservice.war $WILDFLY_DEPLOYMENT  

