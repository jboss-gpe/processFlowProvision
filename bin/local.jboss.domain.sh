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
        -jbossModulePath=*)
            jbossModulePath=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossCliXmx=*)
            jbossCliXmx=`echo $var | cut -f2 -d\=` 
            ;;
        -cliFile=*)
            cliFile=`echo $var | cut -f2 -d\=` 
            ;;
        -hostName=*)
            hostName=`echo $var | cut -f2 -d\=` 
            ;;
        -cliCommand=*)
            cliCommand=`echo $var | cut -f2 -d\=` 
            ;;
        -sleepSec=*)
            sleepSec=`echo $var | cut -f2 -d\=`
            ;;
    esac
done

checkHostName() {
    if ping -c 1 $HOSTNAME > /dev/null 2>&1
    then
        echo "we are online!"
    else
        echo -en "\n unable to ping $HOSTNAME.  check your network settings"
        exit 1
    fi
}

start() {
    checkHostName

    if [ "x$jbossDomainBaseDir" = "x" ]; then
        jbossDomainBaseDir=domain
    fi
    if [ "x$domainConfig" = "x" ]; then
        domainConfig=domain.xml
    fi
    if [ "x$jbossModulePath" != "x" ]; then
        export JBOSS_MODULEPATH=$jbossModulePath
    fi
    echo -en $"Starting jboss daemon w/ following command line args: \n\tjboss.bind.address = $HOSTNAME\n\t-bmanagement = $HOSTNAME\n\tjboss.domain.base.dir= $jbossDomainBaseDir\n\tdomainConfig=$domainConfig\n\tsleepSec=$sleepSec\n"
    sleep 1 
    cd $JBOSS_HOME
    chmod 755 $JBOSS_HOME/bin/*.sh
    rm nohup.out
    nohup ./bin/domain.sh -b=$HOSTNAME -bmanagement=$HOSTNAME -Djboss.domain.base.dir=$jbossDomainBaseDir -Ddomain-config=$domainConfig &
    if [ "x$sleepSec" !=  "x" ]; then
        sleep $sleepSec
    else
        sleep 3
    fi

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
    sleep 3
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

executeCli() {
    echo -en "executeCli() cliCommand = $cliCommand"
    chmod 755 $jbossHome/bin/*.sh

    export JAVA_OPTS=-Xmx$jbossCliXmx

    if [ "x$cliCommand" !=  "x" ]; then
        $jbossHome/bin/jboss-cli.sh --connect --controller=$hostName:$cliPort --command=$cliCommand
    else
        $jbossHome/bin/jboss-cli.sh --connect --controller=$hostName:$cliPort -c --file=$cliFile
    fi
}

case "$1" in
    start|stop|restart|executeCli)
        $1
        ;;
    *)
    echo 1>&2 $"Usage: $0 {start|stop|restart|executeAddUser|executeCli}"
    exit 1
esac
