FROM jboss/wildfly

# Version 1.1.0

MAINTAINER ralph.soika@imixs.com

# add admin account for wildfly console

USER jboss

RUN /opt/jboss/wildfly/bin/add-user.sh admin imixs --silent

# wildfly configuration 
COPY src/docker/eclipselink.jar /opt/jboss/wildfly/modules/system/layers/base/org/eclipse/persistence/main/
COPY src/docker/module.xml /opt/jboss/wildfly/modules/system/layers/base/org/eclipse/persistence/main/
COPY src/docker/standalone.xml /opt/jboss/wildfly/standalone/configuration/
COPY src/docker/imixsrealm.properties /opt/jboss/wildfly/standalone/configuration/

# deploy postgres jdbc
COPY src/docker/postgresql-9.3-1102.jdbc41.jar /opt/jboss/wildfly/standalone/deployments/

# deploy imixs-microservice.war
COPY target/imixs-microservice-*.war /opt/jboss/wildfly/standalone/deployments/

#  use to start bash
#CMD ["bash"]

########################################################
# NOTE: 
# run it with          : docker run -it -p 8080:8080 imixs-microservice
# run in admin mode use: docker run -it -p 8080:8080 -p 9990:9990 imixs-microservice /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
########################################################
