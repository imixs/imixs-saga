#!/bin/bash
#title           :wildfly-install.sh
#description     :The script to install and run Wildfly 9.x in a Docker container
#more            :http://sukharevd.net/wildfly-8-installation.html
#author	         :initial: Dmitriy Sukharev, changes:Ralph Soika
#date            :20161116
#usage           :/bin/bash wildfly-install.sh [INSTALLDIR] [PORTOFFSET]

function usage
{
    echo "usage: wildfly-install [[[-v version ] [-d installdir] [-p portbase]]]"
}


WILDFLY_VERSION=9.0.2.Final
INSTALL_DIR=/opt
PORT_BASE=0

# read options...
while [ "$1" != "" ]; do
    case $1 in
        -v | --version )        shift
                                WILDFLY_VERSION=$1
                                ;;
        -d | --installdir )     shift
			        INSTALL_DIR=$1
                                ;;
        -p | --portbase )     shift
			        PORT_BASE=$1
                                ;;                                
        * )                     usage
                                exit 1
    esac
    shift
done

WILDFLY_FILENAME=wildfly-$WILDFLY_VERSION
WILDFLY_ARCHIVE_NAME=$WILDFLY_FILENAME.tar.gz
WILDFLY_DOWNLOAD_ADDRESS=http://download.jboss.org/wildfly/$WILDFLY_VERSION/$WILDFLY_ARCHIVE_NAME
WILDFLY_FULL_DIR=$INSTALL_DIR/$WILDFLY_FILENAME
WILDFLY_DIR=$INSTALL_DIR/wildfly

WILDFLY_USER="wildfly"
WILDFLY_SERVICE="wildfly"

WILDFLY_STARTUP_TIMEOUT=240
WILDFLY_SHUTDOWN_TIMEOUT=30

echo WILDFLY_VERSION=$WILDFLY_VERSION
echo PORT_BASE=$PORT_BASE
echo INSTALL_DIR=$INSTALL_DIR



SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ $EUID -ne 0 ]]; then
   echo "This script must be run as root."
   exit 1
fi

echo "Downloading: $WILDFLY_DOWNLOAD_ADDRESS..."
[ -e "$WILDFLY_ARCHIVE_NAME" ] && echo 'Wildfly archive already exists.'
if [ ! -e "$WILDFLY_ARCHIVE_NAME" ]; then
  wget -q $WILDFLY_DOWNLOAD_ADDRESS
  if [ $? -ne 0 ]; then
    echo "Not possible to download Wildfly."
    exit 1
  fi
fi

echo "Cleaning up..."
rm -f "$WILDFLY_DIR"
rm -rf "$WILDFLY_FULL_DIR"
rm -rf "/var/run/$WILDFLY_SERVICE/"
rm -f "/etc/init.d/$WILDFLY_SERVICE"

echo "Installation..."
mkdir $WILDFLY_FULL_DIR
tar -xzf $WILDFLY_ARCHIVE_NAME -C $INSTALL_DIR
rm $WILDFLY_ARCHIVE_NAME
ln -s $WILDFLY_FULL_DIR/ $WILDFLY_DIR
useradd -s /sbin/nologin $WILDFLY_USER
chown -R $WILDFLY_USER:$WILDFLY_USER $WILDFLY_DIR
chown -R $WILDFLY_USER:$WILDFLY_USER $WILDFLY_DIR/

echo "...Installation done."