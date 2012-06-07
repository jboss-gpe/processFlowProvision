#!/bin/sh

#  poor mans script for invoking commands on remote nodes across a cluster

SLEEP=0

for node in x277 x278 x281 x282 x293 x294 x297 x299; do
  echo "invoking $node"
  ssh $node $1
  sleep $SLEEP 
done
