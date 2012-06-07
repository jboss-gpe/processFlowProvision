#!/bin/sh

cd $HORNETQ_HOME/bin
./stop.sh ../config/stand-alone/clustered

sleep 2 

echo "Just Killed HornetQ.  List of active java processes on this operating system :"
ps -aef | grep java | grep -v 'grep java'

if test "x$1" = "xdeleteData"
then
    echo "about to delete $HORNETQ_HOME/data & log directories"
    rm -rf $HORNETQ_HOME/data
    rm -rf $HORNETQ_HOME/logs
else
    echo "will not remove $HORNETQ_HOME/data & log directories"
fi
