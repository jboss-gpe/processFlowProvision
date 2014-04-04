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
        #rhc create-app $id https://raw.github.com/jboss-gpe-ose/openshift-origin-cartridge-bpms-full/master/metadata/manifest.yml mysql -g medium --no-git --noprompt
        rhc create-app $id https://raw.github.com/jboss-gpe-ose/openshift-origin-cartridge-fsw-full/master/metadata/manifest.yml mysql -g fsw_medium --no-git --noprompt
        sleep 120
        rhc app stop -a $id
        #rhc app delete -a $id --confirm

        ((t++))
    done
        echo -en "\n\n"
}

refreshBpmsArossAllAccounts() {
    if [ "x$osAccountDetailsFileLocation" = "x" ]; then
        osAccountDetailsFileLocation=$HOME/redhat/openshift/openshift_account_details.xml
    fi
    t=1
    for id in `xmlstarlet sel -t -n -m '//openshiftApps/app[*]' -v 'id' -n $osAccountDetailsFileLocation`; 
    do 
        echo -en "\n"
        echo -en "\n*********   id = $id    **************\n"
        #  ssh <url> 'ctl_app start --cart mysql; bpms/bin/contol refreshBpms
    done
}


case "$1" in
    startJboss|stopJboss|copyFileToRemote|executeMysqlScript|executePostgresqlScript|refreshGuvnor|openshiftRsync|push|checkRemotePort|createTunnel|remoteCommand|setRSAkeyAndNamespaceOnAccounts|provisionAccountsWithPFP|bounceMultipleAccounts|bounceMultipleKBase|executeCommandsAcrossAllAccounts|listDigResultsForEachAccount)
        $1
        ;;
    *)
    echo 1>&2 $"Usage: $0 {startJboss|stopJboss|copyFileToRemote|executeMysqlScript|executePostgresqlScript|refreshGuvnor|openshiftRsync|push|checkRemotePort|createTunnel|remoteCommand|setRSAkeyAndNamespaceOnAccounts|provisionAccountsWithPFP|bounceMultipleAccounts|bounceMultipleKBase|executeCommandsAcrossAllAccounts|listDigResultsForEachAccount}"
    exit 1
esac
