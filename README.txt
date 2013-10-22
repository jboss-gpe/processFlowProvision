processFlowProvision (aka:  PFP)
====================


one approach toward a production BPM environment leveraging Red Hat BPMS "Deployable" libraries



Jeff Bride

welcome Java developers to Process Flow Provision
you have reached the location where PFP source code is centrally maintained

if you are a JEE and *nix administrator, recommend pointing your browser to 
the following to download a PFP binary distribution :
    http://people.redhat.com/jbride

for Java developers, please review :
    1)  docs/ADMIN_GUIDE.txt 
    2)  docs/DEVELOPER_GUIDE.txt



OVERVIEW
  - PFP is a downstream project to BPMS 6.0 Deployable
  - purpose of PFP :
    - provide an example of one possible production BPMS environment using 
      BPMS deployable libraries


PFP github branches :
  - 6.1.0
    - tracks Red Hat BPMS6
    - produces key components of: 'BPMS6 Engine' cartridge for Openshift
    - exposes BPMS6 process and task engines via :
      1)  Execution Server :   REST and JMS
      2)  EJB remoting
    - provisions example BPMS production environment in EAP6.1* domain mode


LEGAL
  - PFP is copyright of Red Hat, Inc. and distributed with a LGPL license
  - PFP is maintained by Red Hat Global Partner Strategy & Enablement Office
  - PFP is a community project with no contractual support offerings
  - Please contact Red Hat to discuss support details for BRMS "Deployable"


FEATURES 
1)  automated provisioning
    - Automates provisioning of BPMS deployable libraries on domain-mode EAP 6.1.1 
    - Automates Hornetq / JBoss A-MQ configurations
    - Provides PostgreSQL, mysql and oracle configuration templates


2)  centralized configuration
    - centralized configuration of BPMS6 properties during build phase 
      - (via a single build.properties)
    - purpose
        - BPMS Deployable has configuration files throughout it's various 
          sub-components
        - instead of manually modifying it's various config files, PFP allows
          the developer/admin to configure properties from a single properties
          file
          

3)  database integration
    - integrated and performance tested  using postgresql and mysql
    - all BPMS6 components now using one of 2 JCA connection pools:
        1)  jbpm-core-cp
        2)  jbpm-bam


4)  EJB wrappers of CDI singleton services
    - provides EJB remote access to CDI singleton services
    - ideal when clients desire to participate in same XA trnx with process
      and task engines
