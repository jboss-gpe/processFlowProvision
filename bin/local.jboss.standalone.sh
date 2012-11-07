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
        -cliPort=*)
            cliPort=`echo $var | cut -f2 -d\=` 
            ;;
        -cliFile=*)
            cliFile=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossNodeName=*)
            jbossNodeName=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossHome=*)
            jbossHome=`echo $var | cut -f2 -d\=` 
            ;;
        -hostName=*)
            hostName=`echo $var | cut -f2 -d\=` 
            ;;
        -serverConfig=*)
            serverConfig=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossServerBaseDir=*)
            jbossServerBaseDir=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossSocketBindingPortOffset=*)
            jbossSocketBindingPortOffset=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossCliXmx=*)
            jbossCliXmx=`echo $var | cut -f2 -d\=` 
            ;;
        -cliCommand=*)
            cliCommand=`echo $var | cut -f2 -d\=` 
            ;;
        -sleepSec=*)
            sleepSec=`echo $var | cut -f2 -d\=` 
            ;;
        -jbossModulePath=*)
            jbossModulePath=`echo $var | cut -f2 -d\=`
            ;;
    esac
done

# better solution is to set and export JAVA_OPTS environment variable
start() {
    echo -en $"Starting jboss daemon w/ following command line args: \n\thostName = $hostName\n\tserver-config = $serverConfig\n\tjboss.server.base.dir = $jbossServerBaseDir \n\tjboss.socket.binding.port-offset = $jbossSocketBindingPortOffset \n\t jbossNodeName= $jbossNodeName\n\tsleepSec=$sleepSec"
    cd $jbossHome
    chmod 755 bin/*.sh

    if [ "x$jbossModulePath" != "x" ]; then
        export JBOSS_MODULEPATH=$jbossModulePath
    fi

    if [ "$jbossServerBaseDir" = "standalone" ]; then
        #  defining jboss.server.base.dir causes problems when deploying SOAP service on AS7.1.1    :   http://pastebin.com/qyX1crrT 
        #  "standalone" server will be reserved for core functionality that needs SOAP
        echo -en $"\nwill start standalone server base dir\n"
        nohup ./bin/standalone.sh -b=$hostName -bmanagement=$hostName --server-config=$serverConfig -Djboss.socket.binding.port-offset=$jbossSocketBindingPortOffset -Djboss.node.name=$jbossNodeName $JAVA_OPTS &
    else
        nohup ./bin/standalone.sh -b=$hostName -bmanagement=$hostName --server-config=$serverConfig -Djboss.server.base.dir=$jbossServerBaseDir -Djboss.socket.binding.port-offset=$jbossSocketBindingPortOffset -Djboss.node.name=$jbossNodeName $JAVA_OPTS &
    fi

    if [ "x$sleepSec" !=  "x" ]; then
        sleep $sleepSec 
    else
        sleep 3
    fi
}

stop() {
    (echo >/dev/tcp/$hostName/$cliPort) &>/dev/null
    if [ $? -eq 0 ]; then
        echo -en $"stopping jboss daemon: \n"
        cd $jbossHome
        chmod 755 bin/*.sh
        ./bin/jboss-cli.sh --connect --controller=$hostName:$cliPort --command=:shutdown
        echo
        rm nohup.out
        sleep 2
    else
        echo -en "\n$hostName:$cliPort is closed."
    fi
}

restart() {
    stop
    start
}

executeAddUser() {
    echo -en "executeAddUser() : isAdmin = $isAdmin : userId = $userId \n"
    cd $jbossHome
    chmod 755 bin/*.sh
    ./bin/add-user.sh $userId $password  --silent=false
    #if [$isAdmin -eq "true"]; then
    #    ./bin/add-user.sh $userId $password 
    #else
    #    ./bin/add-user.sh $userId $password -a
    #fi
}

executeCli() {
    echo -en "executeCliScript() "
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
