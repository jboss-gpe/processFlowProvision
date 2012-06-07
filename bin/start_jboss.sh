#!/bin/sh

SERVER_NAME=default-pfp
cd $JBOSS_HOME/bin
rm nohup.out
nohup ./run.sh -b $HOSTNAME -c $SERVER_NAME &

if test "x$1" = "xlog"
then
    sleep 15 
    tail -f $JBOSS_HOME/server/$SERVER_NAME/log/server.log
else
    echo "started but won't tail $SERVER_NAME/log/server.log"
fi
