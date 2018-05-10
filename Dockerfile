FROM imixs/wildfly:1.2.1

# Imixs-Microservice Version 1.5.4
MAINTAINER ralph.soika@imixs.com

# Install Imixs-Admin Client
RUN wget https://github.com/imixs/imixs-admin/releases/download/4.2.9/imixs-admin-4.2.9.war \
 && mv imixs-admin-4.2.9.war $WILDFLY_DEPLOYMENT  

# add configuration files
COPY ./src/docker/* ${WILDFLY_CONFIG}/

# Install Imixs-Microservice
COPY ./target/imixs-microservice.war $WILDFLY_DEPLOYMENT   

