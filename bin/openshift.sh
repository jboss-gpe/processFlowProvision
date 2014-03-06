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
        -rsaPublicKeyPath=*)
            rsaPublicKeyPath=`echo $var | cut -f2 -d\=`
            ;;
        -bldwProvisionProjectLocation=*)
            bldwProvisionProjectLocation=`echo $var | cut -f2 -d\=`
            ;;
        -skipFullBuild=*)
            skipFullBuild=`echo $var | cut -f2 -d\=`
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
    #getRemoteFileSize
    #localFileSize=$(ls -nl $localDir/$file | awk '{print $5}')
    #if [ $fileSize -eq $localFileSize ]; then
    #    echo -en "\nno need to copy $file"
    #else
    #    echo -en "\nupdate to $file is needed.  local=$localFileSize : remote=$fileSize"
    #    ssh $sshUrl "
    #        mkdir -p $remoteDir;
    #        cd $remoteDir;
    #        rm $file*;
    #    "
    #    scp $localDir/$file $sshUrl:$remoteDir
    #fi
        ssh $sshUrl "
            mkdir -p $remoteDir;
            cd $remoteDir;
            rm -f $file*;
        "
        scp $localDir/$file $sshUrl:$remoteDir
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

# iterates through accounts in ${openshift.account.details.file.location}, and sets appropriate RSA public key and namespace on each account
# usage example:
#   ./bin/openshift.sh setRSAkeyAndNamespaceOnAccounts -rsaPublicKeyPath=/home/jbride/.ssh/os_training.pub   
#
# note: if failures occur (for whatever reason), function will simply iterate to next account listed in ${openshift.account.details.file.location}
setRSAkeyAndNamespaceOnAccounts() {
    
    echo -en "\nsetRSAkeyAndNamespaceOnAccounts() -rsaPublicKeyPath = $rsaPublicKeyPath\n"; 
    cd $JBOSS_PROJECTS/processFlowProvision

    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    echo openshift.account.details.file.location = $osAccountDetailsFileLocation
    t=1
    for domainId in `xmlstarlet sel -t -n -m '//openshiftAccounts/account' -v 'domainId' -n $osAccountDetailsFileLocation`; 
    do
        eval accountId=\"`xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']' -v 'accountId' -n $osAccountDetailsFileLocation` \"
        eval password=\"`xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']' -v 'password' -n $osAccountDetailsFileLocation` \"
        echo -en "\naccountId = $accountId \t:password = $password\t: domainId = $domainId\n"; 
        ((t++))
        rhc sshkey add bldw $rsaPublicKeyPath -l $accountId -p $password

        rhc domain create $domainId -l $accountId -p $password
    done
}

# iterates through accounts in ${openshift.account.details.file.location}, creates an Ant property file and invokes the ant 'openshift.provision.pfp.core' target
provisionAccountsWithPFP() {
    checkLocalJDKVersion

    cd $JBOSS_PROJECTS/processFlowProvision

    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    if [ "x$bldwProvisionProjectLocation" = "x" ]; then
        bldwProvisionProjectLocation=$JBOSS_PROJECTS/workshops/BusinessLogicDevelopmentWorkshop/BLDW-openshift-provision
    fi
    echo openshift.account.details.file.location = $osAccountDetailsFileLocation    :  bldwProvisionProjectLocation = $bldwProvisionProjectLocation
    t=1

    # prior to looping, delete target/pfp/services to trigger a rebuild of all of PFP
    if [ "$skipFullBuild" = "true" ]; 
        then
            echo -en "\nwill not trigger a full rebuild of PFP\n\n";
        else
            rm -rf target/pfp/services;
    fi

    echo -en "\n\nprovisionAccountsWithPFP() BEGIN in 5 seconds\n\n"
    sleep 5 
    date1=$(date +"%s")
    for i in `xmlstarlet sel -t -n -m '//openshiftAccounts/account' -v 'domainId' -n $osAccountDetailsFileLocation`; 
    do 
        # ensure that openshift.accounts.details.xml has appropriate information for this account
        eval pfpcoreUserHash=\"`xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']' -v 'pfpcore/uuid' -n $osAccountDetailsFileLocation` \"
        if [ "x$pfpcoreUserHash" = "x " ]; 
            then
                echo "pfpcore/uuid for account $i in $osAccountDetailsFileLocation is empty.  have you executed:  ant openshift.provision.empty.accounts  ?";
                exit 1;
            else
                echo "openshift.pfpcore.user.hash for account $i in $osAccountDetailsFileLocation is:$pfpcoreUserHash:";
        fi

        #ssh directly to gear, retrieve internal_ip and use this value to replace inline openshift_account_details using
        eval ssh_url=\"`xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']'/pfpcore -v 'git_url' -n $osAccountDetailsFileLocation` \"
        ssh_url=${ssh_url:7}
        totalLength=${#ssh_url}
        urlLength=$(($totalLength-20))
        ssh_url=${ssh_url:0:$urlLength}
        eval internalIp=\"`ssh $ssh_url 'echo $OPENSHIFT_JBOSSEAP_IP'` \"
        echo -en "\nssh_url = $ssh_url : internalIp = $internalIp\n"
        xmlstarlet ed -L -u '//openshiftAccounts/account['$t']'/pfpcore/internal_ip -v $internalIp $osAccountDetailsFileLocation

        # create openshiftAccount.properties file used by bldw provisioning
        echo -n "" > target/openshiftAccount.properties
        xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']' -n \
        -o 'openshift.domain.name=' -v "domainId" -n \
        -o 'openshift.pfpcore.user.hash=' -v "pfpcore/uuid" -n \
        -o 'openshift.pfpcore.internal.ip=' -v "pfpcore/internal_ip" -n \
        $osAccountDetailsFileLocation >> target/openshiftAccount.properties

        
        # will now set 'is.deployment.local' to false .... this property will only exist in an openshift deployment
        echo -n "is.deployment.local=false" >> target/openshiftAccount.properties

        echo -en "\n\nprovisionAccountsWithPFP() ***** now provisioning: $i with brms/pfp :  log can be found in /tmp/$i.log\n\n"; 
        #if ant openshift.provision.pfp.core -Dbounce.servers=false > /tmp/$i.log 2>&1
        #then
        #    echo "just provisioned $i with brms/pfp"
        #else
        #    echo "ERROR :  please review /tmp/$i.log"
        #    exit 1
        #fi

        #echo -en "\n\nprovisionAccountsWithPFP() ***** now provisioning: $i with bldw :  log can be found in /tmp/$i.bldw.log\n\n"; 
        #cd $bldwProvisionProjectLocation
        #if ant > /tmp/$i.bldw.log 2>&1
        #then
        #    echo "just provisioned $i with bldw"
        #else
        #    echo "ERROR :  please review /tmp/$i.bldw.log"
        #    exit 1
        #fi
    	cd $JBOSS_PROJECTS/processFlowProvision
        ((t++))
    done

    date2=$(date +"%s")
    diff=$(($date2-$date1))
    echo -en "\n\nprovisionAccountsWithPFP() DONE ... completed in:   $(($diff / 60)) minutes and $(($diff % 60)) seconds\n\n"
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
        rhc app restart -a pfpcore -l $accountId -p $password
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
        eval app_url=\"`xmlstarlet sel -t -n -m '//openshiftAccounts/account['$t']/pfpcore' -v 'app_url' -n $osAccountDetailsFileLocation`\"
        echo -en "\nabout to execute:  curl -X PUT -HAccept:text/plain $app_url$reloadUri\n"; 
        curl -X PUT -HAccept:text/plain $app_url$reloadUri
        echo -en "\nabout to execute:  curl -X GET -HAccept:text/plain $app_url$getContentUri\n"; 
        curl -X GET -HAccept:text/plain $app_url$getContentUri >> /tmp/kbaseRefreshResults.log
        ((t++))
        echo -en "\n\n\n" >> /tmp/kbaseRefreshResults.log
    done
}

listDigResultsForEachAccount() {
    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    t=1
    echo listDigResultsForEachAccount  : will write results to :  dig_results.txt
    echo -en "dig results as follows:\n\n\n" > dig_results.txt
    for git_url in `xmlstarlet sel -t -n -m '//openshiftAccounts/account[*]/pfpcore' -v 'git_url' -n $osAccountDetailsFileLocation`; 
    do 
        git_url=${git_url#*@}
        totalLength=${#git_url}
        urlLength=$(($totalLength-19))
        git_url=${git_url:0:$urlLength}
        echo -en "git_url = $git_url\n"
        echo -en "git_url = $git_url\n" >> dig_results.txt
        dig +short $git_url >> dig_results.txt
        echo -en "\n\n" >> dig_results.txt
    done
}

executeCommandsAcrossAllAccounts() {
    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    t=1
    for id in `xmlstarlet sel -t -n -m '//openshiftApps/app[*]' -v 'id' -n $osAccountDetailsFileLocation`; 
    do 
        echo -en "\n"
        echo -en "\n*********   id = $id    **************\n"
        rhc create-app $id https://raw.github.com/jboss-gpe-ose/openshift-origin-cartridge-bpms-full/master/metadata/manifest.yml -g medium --no-git
        sleep 2
        rhc cartridge add -a $id -c mysql-5.1
        sleep 2
        rhc cartridge-stop bpms -a $id
        #rhc app delete -a $id --confirm
        sleep 2

        ((t++))
    done
        echo -en "\n\n"
}


case "$1" in
    startJboss|stopJboss|copyFileToRemote|executeMysqlScript|executePostgresqlScript|refreshGuvnor|openshiftRsync|push|checkRemotePort|createTunnel|remoteCommand|setRSAkeyAndNamespaceOnAccounts|provisionAccountsWithPFP|bounceMultipleAccounts|bounceMultipleKBase|executeCommandsAcrossAllAccounts|listDigResultsForEachAccount)
        $1
        ;;
    *)
    echo 1>&2 $"Usage: $0 {startJboss|stopJboss|copyFileToRemote|executeMysqlScript|executePostgresqlScript|refreshGuvnor|openshiftRsync|push|checkRemotePort|createTunnel|remoteCommand|setRSAkeyAndNamespaceOnAccounts|provisionAccountsWithPFP|bounceMultipleAccounts|bounceMultipleKBase|executeCommandsAcrossAllAccounts|listDigResultsForEachAccount}"
    exit 1
esac
