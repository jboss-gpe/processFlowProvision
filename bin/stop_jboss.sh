#!/bin/sh

SERVER_NAME=default-pfp
cd $JBOSS_HOME/bin
./shutdown.sh -u admin -p admin -s jnp://$HOSTNAME:1099
sleep 8

if test "x$1" = "xdeleteData"
then
    echo "about to delete work,tmp & data directories in $JBOSS_HOME/server/$SERVER_NAME"
    rm -rf $JBOSS_HOME/server/$SERVER_NAME/work
    rm -rf $JBOSS_HOME/server/$SERVER_NAME/tmp
    rm -rf $JBOSS_HOME/server/$SERVER_NAME/data
else
    echo "will not remove work,tmp & data directories in $JBOSS_HOME/server/$SERVER_NAME"
fi

echo "Just Killed JBoss.  List of active java processes Ids on this operating system :"
#ps -aef | grep java | grep -v 'grep java'
pgrep java

