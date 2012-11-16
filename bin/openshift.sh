#!/bin/sh

socketIsOpen=2
fileSize=0

for var in $@
do
    case $var in
        -command=*)
            command=`echo $var | cut -f2 -d\=`
            ;;
        -domainName=*)
            domainName=`echo $var | cut -f2 -d\=`
            ;;
        -localAppLocation=*)
            localAppLocation=`echo $var | cut -f2 -d\=`
            ;;
        -remoteJbossHome=*)
            remoteJbossHome=`echo $var | cut -f2 -d\=`
            ;;
        -localJbossHome=*)
            localJbossHome=`echo $var | cut -f2 -d\=`
            ;;
        -serverIpAddr=*)
            serverIpAddr=`echo $var | cut -f2 -d\=`
            ;;
        -user=*)
            user=`echo $var | cut -f2 -d\=`
            ;;
        -password=*)
            password=`echo $var | cut -f2 -d\=`
            ;;
        -port=*)
            port=`echo $var | cut -f2 -d\=`
            ;;
        -sshUrl=*)
            sshUrl=`echo $var | cut -f2 -d\=`
            ;;
        -file=*)
            file=`echo $var | cut -f2 -d\=`
            ;;
        -appName=*)
            apName=`echo $var | cut -f2 -d\=`
            ;;
        -localDir=*)
            localDir=`echo $var | cut -f2 -d\=`
            ;;
        -remoteDir=*)
            remoteDir=`echo $var | cut -f2 -d\=`
            ;;
        -osAccountDetailsFileLocation=*)
            osAccountDetailsFileLocation=`echo $var | cut -f2 -d\=`
            ;;
    esac
done

stopJboss() {
    echo -en $"\nstopping openshift jboss \n"
    ssh $sshUrl "stop_app.sh"
    sleep 2
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

function getRemoteFileSize() {
    fileSize=$(ssh $sshUrl "
        wc -c < $remoteDir/$file;
    ")
    echo -ne "remote file size of $file = $fileSize"
}

#rsync with recursion, verbosity & compression flags
function openshiftRsync() {
    echo -en "rsync() :  rsync -avz $localDir $sshUrl:$remoteDir\n"
    ssh $sshUrl "
        mkdir -p $remoteDir;
    "
    rsync -avz $localDir $sshUrl:$remoteDir
}


function copyFileToRemote() {
    getRemoteFileSize
    localFileSize=$(ls -nl $localDir/$file | awk '{print $5}')
    if [ $fileSize -eq $localFileSize ]; then
        echo -en "\nno need to copy $file"
    else
        echo -en "\nupdate to $file is needed.  local=$localFileSize : remote=$fileSize"
        ssh $sshUrl "
            cd $remoteDir;
            rm $file*;
        "
        scp $localDir/$file $sshUrl:$remoteDir
    fi
}

# ssh -N -L {OPENSHIFT_INTERNAL_IP}:{port}:{OPENSHIFT_INTERNAL_IP}:{port} {UUID}@{appName}-{domain}.rhcloud.com
# determine ports by :  ssh {UUID}@{appName}-{domain}.rhcloud.com 'rhc-list-ports'
function createTunnel() {
    checkRemotePort
    if [ $socketIsOpen -ne 0 ]; then
        echo -en "\nattempting to create ssh tunnel serverIpAddr = $serverIpAddr : port = $port sshUrl = $sshUrl\n"
        ssh -N -L $serverIpAddr:$port:$serverIpAddr:$port $sshUrl &
        #echo -en "createTunnel() response = $? "
        sleep 5
        checkRemotePort
        if [ $socketIsOpen -ne 0 ]; then
            echo -en "\n unable to create tunnel.  see previous errors"
            exit 1
        fi
    fi

}

startJboss() {
    echo -en "\nattempting to start jboss using sshUrl = $sshUrl\n"
    ssh $sshUrl '
        cd git/$OPENSHIFT_APP_NAME.git;
        post_receive_app.sh
    '
}

executeMysqlScript() {
    createTunnel
    mysql -u$user -p$password -h$serverIpAddr < $file
}

executePostgresqlScript() {
    createTunnel
    scp $localDir/$file $sshUrl:/$remoteDir
    ssh $sshUrl "
        createdb guvnor;
        createdb jbpm;
        createdb jbpm_bam;

        psql -d postgres -f $remoteDir/$file
    "
}

remoteCommand() {
    echo -en "remoteCommand() command = $command";
    ssh $sshUrl "$command"
}

refreshGuvnor() {
    ssh $sshUrl "
        rm -rf $OPENSHIFT_DATA_DIR/guvnor;
        mkdir $OPENSHIFT_DATA_DIR/guvnor;
    "
    echo -en "\nabout to copy target/tmp/repository.xml to $sshUrl:$remoteDir"
    scp target/tmp/repository.xml $sshUrl:$remoteDir
}

push() {
    echo "git push $domainName"
    cd $localAppLocation
    chmod -R 775 .openshift/action_hooks
    git add -A
    git commit -m 'latest from pfp'
    git push $domainName master
}

function provisionIndividualAccount() {
    echo "hello"
}


# loops through accounts in ${openshift.account.details.file.location}, creates an Ant property file invokes the 'openshift.provision.both' target
provisionAccountsWithPFP() {
    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    echo openshift.account.details.file.location = $osAccountDetailsFileLocation
    for i in `xmlstarlet sel -t -n -m '//openshiftAccounts/account' -o 'openshift.domain.name=' -v 'accountId' -n $osAccountDetailsFileLocation`; 
    do 
        printf "\n$i\n"; 
        echo -n "" > target/openshiftAccount.properties
        xmlstarlet sel -t -n -m '//openshiftAccounts/account' -n \
        -o 'openshift.domain.name=' -v "domainId" -n \
        -o 'openshift.pfpCore.user.hash=' -v "pfpCore/uuid" -n \
        -o 'openshift.pfpCore.internal.ip=' -v "pfpCore/internal_ip" -n \
        -o 'openshift.brmsWebs.user.hash=' -v "brmsWebs/uuid" -n \
        -o 'openshift.brmsWebs.internal.ip=' -v "brmsWebs/internal_ip" -n \
        $osAccountDetailsFileLocation >> target/openshiftAccount.properties

        #ant openshift.provision.both
        ant openshift.provision.pfp.core
    done

    # will now set 'is.deployment.local' to false .... this property will only exist in an openshift deployment
    echo -n "is.deployment.local=false" >> target/openshiftAccount.properties
}


case "$1" in
    startJboss|stopJboss|copyFileToRemote|executeMysqlScript|executePostgresqlScript|refreshGuvnor|openshiftRsync|push|checkRemotePort|createTunnel|remoteCommand|provisionAccountsWithPFP)
        $1
        ;;
    *)
    echo 1>&2 $"Usage: $0 {startJboss|stopJboss|copyFileToRemote|executeMysqlScript|executePostgresqlScript|refreshGuvnor|openshiftRsync|push|checkRemotePort|createTunnel|remoteCommand|provisionAccountsWithPFP}"
    exit 1
esac
