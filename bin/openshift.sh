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
        -rsyncDelete=*)
            rsyncDelete=`echo $var | cut -f2 -d\=`
            ;;
    esac
done

stopJboss() {
    echo -en $"\nstopping openshift jboss \n"
    ssh $sshUrl "app_ctl.sh stop"
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
    echo -en "rsync() :  rsync -avz $localDir $sshUrl:$remoteDir\t\t :  rsyncDelete = $rsyncDelete\n"
    ssh $sshUrl "
        mkdir -p $remoteDir;
    "
    if [ "x$rsyncDelete" != "x" ]; then
        rsync -avz --delete $localDir $sshUrl:$remoteDir
    else
        rsync -avz $localDir $sshUrl:$remoteDir
    fi
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
    ssh $sshUrl 'app_ctl.sh start'
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



# checkLocalJDKVersion
#     - PFP services must be compiled using JDK 1.6 since target openshift runtime is JRE 1.6
checkLocalJDKVersion() {
    version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ $version =~ .*1.6.* ]]; 
    then
        echo -en "\n checkJDKVersion() java version is fine:  $version \n\n"
    else
        echo -en "\n checkJDKVersion() java version: $version    must be 1.6.  will now exit\n\n"
        exit 1
    fi
}


# loops through accounts in ${openshift.account.details.file.location}, creates an Ant property file invokes the 'openshift.provision.both' target
provisionAccountsWithPFP() {
    checkLocalJDKVersion

    cd $JBOSS_PROJECTS/processFlowProvision
    ant pfp.clean

    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    echo openshift.account.details.file.location = $osAccountDetailsFileLocation
    t=1
    for i in `xmlstarlet sel -t -n -m '//openshiftAccounts/account' -v 'domainId' -n $osAccountDetailsFileLocation`; 
    do 
        echo -en "\n$i\n"; 
        echo -n "" > target/openshiftAccount.properties
        xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']' -n \
        -o 'openshift.domain.name=' -v "domainId" -n \
        -o 'openshift.pfpCore.user.hash=' -v "pfpCore/uuid" -n \
        -o 'openshift.pfpCore.internal.ip=' -v "pfpCore/internal_ip" -n \
        $osAccountDetailsFileLocation >> target/openshiftAccount.properties

        # will now set 'is.deployment.local' to false .... this property will only exist in an openshift deployment
        echo -n "is.deployment.local=false" >> target/openshiftAccount.properties

        ant openshift.provision.both
        ((t++))

        cd $JBOSS_PROJECTS/workshops/BusinessLogicDevelopmentWorkshop/BLDW-openshift-provision
        ant
    	cd $JBOSS_PROJECTS/processFlowProvision
    done
}

bounceMultipleAccounts() {
    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    echo openshift.account.details.file.location = $osAccountDetailsFileLocation
    t=1
    for domainId in `xmlstarlet sel -t -n -m '//openshiftAccounts/account' -v 'domainId' -n $osAccountDetailsFileLocation`; 
    do 
        eval accountId=\"`xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']' -v 'accountId' -n $osAccountDetailsFileLocation` \"
        eval password=\"`xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']' -v 'password' -n $osAccountDetailsFileLocation` \"
        echo -en "\naccountId = $accountId \t:password = $password\n"; 
        rhc app restart -a brmsWebs -l $accountId -p $password
        rhc app restart -a pfpCore -l $accountId -p $password
        ((t++))
    done
}

bounceMultipleKBase() {
    reloadUri="knowledgeService/kbase"
    getContentUri="knowledgeService/kbase/content"
    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    echo openshift.account.details.file.location = $osAccountDetailsFileLocation  : will write results to :  /tmp/kbaseRefreshResults.log
    echo -n "" > /tmp/kbaseRefreshResults.log
    t=1
    for domainId in `xmlstarlet sel -t -n -m '//openshiftAccounts/account' -v 'domainId' -n $osAccountDetailsFileLocation`; 
    do 
        eval app_url=\"`xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']/pfpCore' -v 'app_url' -n $osAccountDetailsFileLocation`\"
        echo -en "\nabout to execute:  curl -X PUT -HAccept:text/plain $app_url$reloadUri\n"; 
        curl -X PUT -HAccept:text/plain $app_url$reloadUri
        echo -en "\nabout to execute:  curl -X GET -HAccept:text/plain $app_url$getContentUri\n"; 
        curl -X GET -HAccept:text/plain $app_url$getContentUri >> /tmp/kbaseRefreshResults.log
        ((t++))
        echo -en "\n\n\n" >> /tmp/kbaseRefreshResults.log
    done
}


case "$1" in
    startJboss|stopJboss|copyFileToRemote|executeMysqlScript|executePostgresqlScript|refreshGuvnor|openshiftRsync|push|checkRemotePort|createTunnel|remoteCommand|provisionAccountsWithPFP|bounceMultipleAccounts|bounceMultipleKBase)
        $1
        ;;
    *)
    echo 1>&2 $"Usage: $0 {startJboss|stopJboss|copyFileToRemote|executeMysqlScript|executePostgresqlScript|refreshGuvnor|openshiftRsync|push|checkRemotePort|createTunnel|remoteCommand|provisionAccountsWithPFP|bounceMultipleAccounts|bounceMultipleKBase}"
    exit 1
esac
