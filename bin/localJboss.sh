#!/bin/sh

for var in $@
do
    case $var in
        -userId=*)
            userId=`echo $var | cut -f2 -d\=` 
            ;;
        -password=*)
            password=`echo $var | cut -f2 -d\=` 
            ;;
        -isAdmin=*)
            isAdmin=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossDomainBaseDir=*)
            jbossDomainBaseDir=`echo $var | cut -f2 -d\=` 
            ;;
        -domainConfig=*)
            domainConfig=`echo $var | cut -f2 -d\=` 
            ;;
        -cliPort=*)
            cliPort=`echo $var | cut -f2 -d\=` 
            ;;
        -node=*)
            node=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossHome=*)
            jbossHome=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossDomainBaseDir=*)
            jbossDomainBaseDir=`echo $var | cut -f2 -d\=` 
            ;;
    esac
done

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
    sleep 15 
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

executeAddUser() {
    echo -en "executeAddUser() : isAdmin = $isAdmin : userId = $userId \n"
    cd $JBOSS_HOME
        ./bin/add-user.sh $userId $password  --silent=false
    #if [$isAdmin -eq "true"]; then
    #    ./bin/add-user.sh $userId $password 
    #else
    #    ./bin/add-user.sh $userId $password -a
    #fi
}


case "$1" in
    start|stop|restart|executeAddUser)
        $1
        ;;
    *)
    echo 1>&2 $"Usage: $0 {start|stop|restart|executeAddUser}"
    exit 1
esac
