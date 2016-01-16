FROM debian:jessie

# Imixs-Microservice Version 1.2.0
MAINTAINER ralph.soika@imixs.com


####################################################################
# Setup User Imxis
####################################################################
USER root

# add jessie backports
RUN echo "#Backports" >> /etc/apt/sources.list && echo deb http://http.debian.net/debian jessie-backports main >> /etc/apt/sources.list


# Create a user and group used to launch processes
# The user ID 1000 is the default for the first "regular" user on Fedora/RHEL,
# so there is a high chance that this ID will be equal to the current user
# making it easier to use volumes (no permission issues)
RUN groupadd -r imixs -g 1000 && useradd -u 1000 -r -g imixs -m -d /opt/imixs -s /sbin/nologin -c "Imixs user" imixs && \
    chmod 755 /opt/imixs

# Set the working directory to imixs' user home directory
WORKDIR /opt/imixs

# Specify the user which should be used to execute all commands below
USER imixs

#####################################################################
# Install PostgreSQL, Java-8, WildFly 
#####################################################################

# User root user to install software
USER root

# Install necessary packages
RUN apt-get update && apt-get -q -y install postgresql wget openjdk-8-jre-headless openjdk-8-jdk

#####################################################################
# Setup postgreSQL Database 'imixs01'
#####################################################################
USER postgres
RUN /etc/init.d/postgresql start &&\
  createuser -s imixs &&\
  psql --command "ALTER USER imixs WITH PASSWORD 'imixs';" &&\
  psql postgres -c "CREATE DATABASE imixs01 WITH ENCODING 'UTF8' TEMPLATE template0" &&\
  psql postgres -c "ALTER DATABASE imixs01 OWNER TO imixs;" &&\
  /etc/init.d/postgresql stop



#####################################################################
# Install Wildfly 
#####################################################################
USER root
COPY src/docker/wildfly-install.sh ./
COPY src/docker/start.sh /opt/imixs/
RUN chmod a+x /opt/imixs/start.sh &&\
	chmod a+x ./wildfly-install.sh &&\
	./wildfly-install.sh &&\
	rm wildfly-install.sh
USER imixs


#####################################################################
# Setup Wildfly 
#####################################################################
 
# add admin account for wildfly console

USER root

RUN /opt/wildfly/bin/add-user.sh admin imixs --silent

# wildfly configuration 
COPY src/docker/eclipselink.jar /opt/wildfly/modules/system/layers/base/org/eclipse/persistence/main/
COPY src/docker/module.xml /opt/wildfly/modules/system/layers/base/org/eclipse/persistence/main/
COPY src/docker/standalone.xml /opt/wildfly/standalone/configuration/
COPY src/docker/imixsrealm.properties /opt/wildfly/standalone/configuration/

# deploy postgres jdbc
COPY src/docker/postgresql-9.3-1102.jdbc41.jar /opt/wildfly/standalone/deployments/

# deploy imixs-microservice.war
COPY target/imixs-microservice*.war /opt/wildfly/standalone/deployments/


EXPOSE 8080

# Set the default command to run on boot
# This will boot WildFly in the standalone mode and bind to all interface
#CMD ["/opt/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
CMD ["/opt/imixs/start.sh"]

########################################################
# NOTE: 
# run it with          : docker run -it -p 8080:8080 imixs-microservice
# run in admin mode use: docker run -it -p 8080:8080 -p 9990:9990 imixs-microservice /opt/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
########################################################
