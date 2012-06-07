#!/bin/sh

export HORNETQ_HOME=..
mkdir -p ../logs
# By default, the server is started in the non-clustered standalone configuration

if [ a"$1" = a ]; then CONFIG_DIR=$HORNETQ_HOME/config/stand-alone/non-clustered; else CONFIG_DIR="$1"; fi
if [ a"$2" = a ]; then FILENAME=hornetq-beans.xml; else FILENAME="$2"; fi

export CLASSPATH=$CONFIG_DIR:$HORNETQ_HOME/schemas/

#JA Bride :  UDP discovery and netty TCP configuration
SYS_PROPS="-Dhornetq.remoting.netty.host=$HOSTNAME"
SYS_PROPS="$SYS_PROPS -Dhornetq.remoting.netty.port=${hornetq.remoting.netty.port}"
SYS_PROPS="$SYS_PROPS -Dhornetq.remoting.netty.batch.port=5555"
SYS_PROPS="$SYS_PROPS -Dhornetq.config.dir=$CONFIG_DIR"

# JA Bride :  enable jmx properties for remote management
SYS_PROPS="$SYS_PROPS -Dcom.sun.management.jmxremote.port=1597"
SYS_PROPS="$SYS_PROPS -Dcom.sun.management.jmxremote.authenticate=false"
SYS_PROPS="$SYS_PROPS -Dcom.sun.management.jmxremote.ssl=false"

#JA Bride: hornetq jndi address/port configuration
SYS_PROPS="$SYS_PROPS -Djnp.host=$HOSTNAME"
SYS_PROPS="$SYS_PROPS -Djnp.rmiPort=1598"
SYS_PROPS="$SYS_PROPS -Djnp.port=1599"

SYS_PROPS="$SYS_PROPS -Djava.util.logging.config.file=$CONFIG_DIR/logging.properties"
SYS_PROPS="$SYS_PROPS -Djava.library.path=."

#JA Bride: allow remote debugging of stand-alone hornetq provider
#SYS_PROPS="$SYS_PROPS -Xrunjdwp:transport=dt_socket,address=8187,server=y,suspend=n"

export JVM_ARGS="$SYS_PROPS -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms128m -Xmx256m"



for i in `ls $HORNETQ_HOME/lib/*.jar`; do
	CLASSPATH=$i:$CLASSPATH
done

echo "***********************************************************************************"
echo "java $JVM_ARGS -classpath $CLASSPATH org.hornetq.integration.bootstrap.HornetQBootstrapServer $FILENAME"
echo "***********************************************************************************"
java $JVM_ARGS -classpath $CLASSPATH -Dcom.sun.management.jmxremote org.hornetq.integration.bootstrap.HornetQBootstrapServer $FILENAME
