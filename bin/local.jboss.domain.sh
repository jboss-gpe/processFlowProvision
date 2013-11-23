#!/bin/bash

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
        -serverIpAddr=*)
            serverIpAddr=`echo $var | cut -f2 -d\=` 
            ;;
        -port=*)
            port=`echo $var | cut -f2 -d\=` 
            ;;
        -orgName=*)
            orgName=`echo $var | cut -f2 -d\=` 
            ;;
        -webContext=*)
            webContext=`echo $var | cut -f2 -d\=` 
            ;;
        -taskId=*)
            taskId=`echo $var | cut -f2 -d\=` 
            ;;
        -deployId=*)
            deployId=`echo $var | cut -f2 -d\=` 
            ;;
        -pathToFSWInstaller=*)
            pathToFSWInstaller=`echo $var | cut -f2 -d\=` 
            ;;
    esac
done

checkHostName() {
    if [ "x$hostName" = "x" ]; then
        hostName=$HOSTNAME
    fi
    if ping -c 1 $hostName > /dev/null 2>&1
    then
        echo "we are online!"
    else
        echo -en "\n unable to ping $hostName.  check your network settings"
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
    if [ "x$hostName" = "x" ]; then
        hostName=$HOSTNAME
    fi
    if [ "x$jbossHome" = "x" ]; then
        jbossHome=$JBOSS_HOME
    fi
    echo -en $"Starting jboss daemon at $jbossHome w/ following command line:\n  nohup ./bin/domain.sh -b=$hostName -bmanagement=$hostName -Djboss.domain.base.dir=$jbossDomainBaseDir -Ddomain-config=$domainConfig & \n"
    sleep 1 
    cd $jbossHome
    chmod 755 $jbossHome/bin/*.sh
    rm nohup.out
    nohup ./bin/domain.sh -b=$hostName -bmanagement=$hostName -Djboss.domain.base.dir=$jbossDomainBaseDir -Ddomain-config=$domainConfig &
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

    echo -en $"stopping following jboss node: $node\t: at $hostName:$cliPort\t : jbossHome = $jbossHome\n"
    cd $jbossHome
    chmod 755 $jbossHome/bin/*.sh
    ./bin/jboss-cli.sh --connect --controller=$hostName:$cliPort --command=/host=$node:shutdown
    echo
    sleep 3
}

restart() {
    stop
    start
}

executeAddUser() {
    echo -en "executeAddUser() : isAdmin = $isAdmin : userId = $userId \n"
    cd $jbossHome
        ./bin/add-user.sh $userId $password  --silent=false
    #if [$isAdmin -eq "true"]; then
    #    ./bin/add-user.sh $userId $password 
    #else
    #    ./bin/add-user.sh $userId $password -a
    #fi
}

executeCli() {
    echo -en "executeCli() cliCommand = $cliCommand\n"
    chmod 755 $jbossHome/bin/*.sh

    export JAVA_OPTS=-Xmx$jbossCliXmx

    if [ "x$cliCommand" !=  "x" ]; then
        $jbossHome/bin/jboss-cli.sh --connect --controller=$hostName:$cliPort --command=$cliCommand
    else
        $jbossHome/bin/jboss-cli.sh --connect --controller=$hostName:$cliPort -c --file=$cliFile
    fi
}

# ./bin/local.jboss.domain.sh refreshSlaveHosts -serverIpAddr=eap6cluster1 -orgName=gpe
refreshSlaveHosts() {
    echo -en "refreshSlaveHosts() serverIpAddr = $serverIpAddr : orgName=$orgName\n"
    #  0)  verify network connectivity to remote host
    port=22;
    checkRemotePort;
    if [ $socketIsOpen -ne 0 ]; then
        exit 1
    fi

    # 1)  kill any existing java processes on remote node  
    ssh jboss@$serverIpAddr 'for jProc in `ps -C java -o pid=`;
        do
            echo -en "about to kill java process id = $jProc\n"
            kill -9 $jProc
        done
    '
    # 2) scp .bashrc with module path env variable 
    scp conf/shell/slavebashrc jboss@$serverIpAddr:/home/jboss/.bashrc

    # 3)  blow away existing eap
    ssh jboss@$serverIpAddr 'mkdir -p ~/jbossProjects/downloads; rm -rf $JBOSS_HOME'

    # 4)  scp and  unzip jboss in remote host
    remoteZip=$(ssh jboss@$serverIpAddr "ls ~/jbossProjects/downloads/jboss-eap-6.0.1.zip")
    if [ "x$remoteZip" == "x" ]; then
        scp target/lib/jboss-eap-6.0.1.zip jboss@$serverIpAddr:/home/jboss/jbossProjects/downloads
    fi

    # 5)  clone domain root and rename to domain-$orgName
    ssh jboss@$serverIpAddr "unzip ~/jbossProjects/downloads/jboss-eap-6.0.1.zip -d /opt; cp -r /opt/jboss-eap-6.0/domain /opt/jboss-eap-6.0/domain-$orgName"

    # 6) rsync modules, slave host.xml, application-roles.properties and application-users.properties
    rsync -avz target/jboss/* jboss@$serverIpAddr:/opt/jboss-eap-6.0

        #                            - attempt to use generic userId for communication back to parent process controller (rather than userId equivalent to hostname)
        #                            - may need to generate management user for each host in master node (depending on step #2)

    # 7)  start remote host
    ssh jboss@$serverIpAddr "cd /opt/jboss-eap-6.0; nohup ./bin/domain.sh -b=$serverIpAddr -Djboss.domain.base.dir=domain-$orgName -Djboss.domain.master.address=$HOSTNAME -Djboss.domain.master.port=9999 > eap.log 2>&1 &"
}

# Test remote host:port availability (TCP-only as UDP does not reply)
function checkRemotePort() {
    echo "checkRemotePort"
    (echo >/dev/tcp/$serverIpAddr/$port) &>/dev/null
    if [ $? -eq 0 ]; then
        echo -en "\n$serverIpAddr:$port is open."
        socketIsOpen=0
    else
        echo -en "\n$serverIpAddr:$port is closed."
        socketIsOpen=1
    fi
}


function killJbossProcesses() {
    sleep 2;
    for jProc in `ps -C java -o pid=`;
    do
        pInfo=$(ps -p $jProc -f)
        if [[ $pInfo =~ .*jboss.modules.system.pkgs.* ]];
        then
            if [[ $pInfo =~ .*org.jboss.as.cli.* ]];
            then
                echo -en "\nkillJavaProcesses() will not kill jboss cli = $jProc\n"
            else
                echo -en "killJavaProcesses() about to kill jboss process id = $jProc\n"
                kill -9 $jProc
            fi
        else
            echo -en "\nkillJavaProcesses() will not kill java process = $jProc\n"
        fi
    done
}

# example:   ./bin/local.jboss.domain.sh smokeTest -deployId=org.acme.insurance:policyquote:1.0.0
function smokeTest() {
    if [ "x$userId" = "x" ]; then
        userId=jboss
    fi
    if [ "x$password" = "x" ]; then
        password=brms
    fi
    if [ "x$port" = "x" ]; then
        port=8330
    fi
    if [ "x$webContext" = "x" ]; then
        webContext=services-remote
    fi
    if [ "x$deployId" = "x" ]; then
        deployId=org.acme.insurance:policyquote:1.0.0
    fi

    # list all process definitions for designated deployment
    curl -v -u $userId:$password -X GET http://$HOSTNAME:$port/$webContext/rest/additional/runtime/$deployId/processes

    # start an instance of policyQuoteTask
    curl -v -u $userId:$password -X POST -d 'map_bonusAmount=1500' -d 'map_selectedEmployee=Alex' http://$HOSTNAME:$port/$webContext/rest/runtime/$deployId/process/policyQuoteTask/start

    #  NOTE:  as per org.kie.services.remote.rest.TaskResource, query paramter logic as follows:
    #    1)  specify value for one of the following:   businessAdministrator, potentialOwner or taskOwner
    #                    or
    #            a value for:  processInstanceId 
    #
    #         NOTE:  "status" and "language" are optional query paramters
    #
    #                OR
    #
    #     2)  specify value for taskId
    #                OR
    #     3)  specify value for workItemId

    # query for all tasks with status=Ready and any potential owner (which includes task that don't currently have an owner)
    curl -v -u $userId:$password -X GET http://$HOSTNAME:$port/$webContext/rest/task/query?potentialOwner= > /tmp/humanTasks.txt
    eval taskId=\"`xmlstarlet sel -t -n -m '//task-summary-list/task-summary[1]' -v 'id' -n /tmp/humanTasks.txt`\"

    #ensure no whitespace in taskId
    taskId=`echo $taskId | tr -d ' '`
    if [ "x$taskId" = "x" ]; then
        echo -en "\n unable to locate any tasks in any Ready state\n"
        exit 1;
    fi

    curl -u $userId:$password -X POST http://$HOSTNAME:$port/$webContext/rest/task/$taskId/claim
    curl -v -u $userId:$password -X POST http://$HOSTNAME:$port/$webContext/rest/task/$taskId/start
    curl -v -u $userId:$password -X GET http://$HOSTNAME:$port/$webContext/rest/task/$taskId/content
    curl -v -u $userId:$password -X POST -d 'map_bonusAmount=1501' -d 'map_selectedEmployee=Azra' http://$HOSTNAME:$port/$webContext/rest/task/$taskId/complete

    # claim next available task
    # study org.kie.services.client.serialization.jaxb.JaxbCommandsRequest to understand http request payload
    #curl -v -u $userId:$password -H "Content-Type:application/xml" -d '<command-request><deployment-id>$deployId</deployment-id><process-instance-id>1</process-instance-id><claim-next-available-task/></command-request>' -X POST http://$HOSTNAME:$port/$webContext/rest/task/execute
    #curl -v -u $userId:$password -H "Content-Type:application/xml" -d '<command-request><deployment-id>$deployId</deployment-id><process-instance-id>1</process-instance-id><claim-task id="1" /></command-request>' -X POST http://$HOSTNAME:$port/$webContext/rest/task/execute
}

runFSWInstaller() {
    export JBOSS_HOME=
    java -jar $pathToFSWInstaller target/tmp/fsw/installer/fsw.install.xml
}


case "$1" in
    start|stop|restart|executeCli|refreshSlaveHosts|killJbossProcesses|smokeTest|runFSWInstaller)
        $1
        ;;
    *)
    echo 1>&2 $"Usage: $0 {start|stop|restart|executeAddUser|executeCli|refreshSlaveHosts|killJbossProcesses|smokeTest|runFSWInstaller}"
    exit 1
esac
