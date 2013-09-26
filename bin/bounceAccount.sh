#!/bin/sh

#
# Bounce OpenShift accounts based on account numbers. The account numbers are passed as command-line arguments.
# For each account it will execute:
#
#   rhc app restart -a pfpCore -l gpse.training+[0-n]@redhat.com -p jb0ssredhat
#
# Usage: bounceAccounts [accountNumber1] [accountNumber2] ...
#
# This example will bounce the accounts for 55, 23 and 6.
#
#   bounceAccounts 55 23 6
#
#
for var in "$@"
do
echo "Bouncing account: $var"
rhc app restart -a pfpCore -l gpse.training+$var@redhat.com -p jb0ssredhat 
done
