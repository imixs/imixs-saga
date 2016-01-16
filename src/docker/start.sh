#!/bin/bash
#title           :start.sh
#description     :Start script for postgreSQL and Wildfly
#author	         :initial: Ralph Soika
#date            :20161116
#usage           :/bin/bash start.sh 

echo "starting server..."
/etc/init.d/postgresql start
/opt/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0

