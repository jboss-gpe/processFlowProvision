#!/bin/sh

export jbossDomainBaseDir=$2
export domainConfig=$3

export cliPort=$2
export node=$3

start() {
    if [ "x$jbossDomainBaseDir" = "x" ]; then
        jbossDomainBaseDir=domain
    fi
    if [ "x$domainConfig" = "x" ]; then
        domainConfig=domain.xml
    fi
    echo -en $"Starting jboss daemon w/ following command line args: \n\tjboss.bind.address = $HOSTNAME\n\t-bmanagement = $HOSTNAME\n\tjboss.domain.base.dir= $jbossDomainBaseDir\n\tdomainConfig=$domainConfig\n"
    sleep 1 
    cd $JBOSS_HOME
    rm nohup.out
    nohup ./bin/domain.sh -b=$HOSTNAME -bmanagement=$HOSTNAME -Djboss.domain.base.dir=$jbossDomainBaseDir -Ddomain-config=$domainConfig &
    sleep 10 
}

stop() {
    if [ "x$cliPort" = "x" ]; then
        cliPort=9999
    fi
    if [ "x$node" = "x" ]; then
        node=master
    fi

    echo -en $"stopping the following jboss node: $node\n"
    cd $JBOSS_HOME
    ./bin/jboss-cli.sh --connect --controller=$HOSTNAME:$cliPort --command=/host=$node:shutdown
    echo
    sleep 2
}

restart() {
    stop
    start
}

case "$1" in
    start|stop|restart)
        $1
        ;;
    *)
    echo 1>&2 $"Usage: $0 {start|stop|restart}"
    exit 1
esac
