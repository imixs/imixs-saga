FROM imixs/wildfly:1.2.6

# Imixs-Microservice Version 1.5.4
MAINTAINER ralph.soika@imixs.com

# add configuration files
COPY ./src/docker/conf/* ${WILDFLY_CONFIG}/

# add application artifacts
COPY ./src/docker/apps/* ${WILDFLY_DEPLOYMENT}/   
