#!/bin/bash

APP_HOME=/apps/BB-Image
INSTALL_PATH=$APP_HOME/current
LOG_DIR=$INSTALL_PATH/logs

SERVICE_NAME=application
JMX_PORT=14002

#purge temp directory
rm -rf $APP_HOME/temp/*

#save existing logs
if [ ! -d "$LOG_DIR" ]; then
    mkdir $LOG_DIR
fi
if [ -d "$LOG_DIR" ]; then
    echo "Archiving old logs"

    cd $LOG_DIR
    if [ ! -d "archive" ]; then
        mkdir archive
    fi

    if [ -e  $SERVICE_NAME.log ]; then
        nowdate=`date +%Y%m%d_%H%M`
        tar cfz archive/${SERVICE_NAME}_$nowdate.tar.gz  $SERVICE_NAME.log* jvm.log
        rm $SERVICE_NAME.log*
    fi
fi

#Start up Server
echo "Starting play"
echo cd $INSTALL_PATH
echo ""
echo bin/bb-server2 -J-Xloggc:$LOG_DIR/jvm.log -J-XX:+HeapDumpOnOutOfMemoryError -J-XX:+DisableExplicitGC -J-XX:+PrintGCDetails -J-XX:+PrintGCDateStamps -J-Xmx768m -J-XX:MaxPermSize=128m -Dhttp.port=9002 -Dconfig.file=/opt/conf/bb_image_prod.conf
echo ""

cd $INSTALL_PATH
nohup bin/bb-server2 -J-Xloggc:$LOG_DIR/jvm.log -J-XX:+HeapDumpOnOutOfMemoryError -J-XX:+DisableExplicitGC -J-XX:+PrintGCDetails -J-XX:+PrintGCDateStamps -J-Xmx768m -J-XX:MaxPermSize=128m -Dhttp.port=9002 -Dconfig.file=/opt/conf/bb_image_prod.conf &