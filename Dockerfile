FROM imixs/wildfly:latest

COPY ./src/docker/imixsrealm.properties ${WILDFLY_CONFIG}/
COPY ./src/docker/standalone.xml ${WILDFLY_CONFIG}/
COPY ./target/imixs-microservice.war ${WILDFLY_DEPLOYMENT}/

