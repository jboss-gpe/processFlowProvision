#!/bin/sh

cd $HORNETQ_HOME/bin
rm nohup.out
chmod 755 run.sh
nohup ./run.sh ../config/stand-alone/clustered &

if test "x$1" = "xlog"
then
    sleep 4
    tail -f $HORNETQ_HOME/logs/hornetq.log
else
    echo "started but won't tail $HORNETQ_HOME/logs/hornetq.log"
fi

