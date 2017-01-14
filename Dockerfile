FROM imixs/wildfly:latest

COPY ./src/docker/imixsrealm.properties ${WILDFLY_CONFIG}/
COPY ./src/docker/standalone.xml ${WILDFLY_CONFIG}/

# Install Imixs-Admin Client
RUN wget https://github.com/imixs/imixs-admin/releases/download/4.2.1/imixs-admin-4.2.1.war \
 && mv imixs-admin-4.2.1.war $WILDFLY_DEPLOYMENT  


COPY ./target/imixs-microservice.war ${WILDFLY_DEPLOYMENT}/

