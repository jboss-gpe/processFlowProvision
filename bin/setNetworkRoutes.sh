#!/bin/sh

#  set network UDP network routes for both hornetq and mod_cluster

# hornetq
route add -net 231.0.0.0 netmask 255.0.0.0 virbr0

#mod_cluster
route add -net 224.0.0.0 netmask 255.0.0.0 virbr0
