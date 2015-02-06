FROM jboss/wildfly

# Install Maven and postgres
USER root
RUN yum -y install postgresql maven && yum clean all
USER jboss

# add admin account for wildfly console
RUN /opt/jboss/wildfly/bin/add-user.sh admin admin --silent

# deploy postgres jdbc
ADD src/docker/postgresql-9.3-1102.jdbc41.jar /opt/jboss/wildfly/standalone/deployments/

# deploy imixs-microservice.war
ADD target/imixs-microservice-1.0.0.war /opt/jboss/wildfly/standalone/deployments/



# run it with: docker run -it -p 9990:9990 imixs-microservice