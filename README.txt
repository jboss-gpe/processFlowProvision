processFlowProvision (aka:  PFP)
====================


one approach toward a production BPM environment leveraging Red Hat BPMS "Deployable" libraries



Jeff Bride

welcome Java developers to Process Flow Provision
you have reached the location where PFP source code is centrally maintained

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
1)  domain-mode, automated provisioning
    - Automates provisioning of BPMS deployable libraries on domain-mode EAP 6.1.1 
    - NOTE:  jbpm installer does this as well.  Difference being, automated
      provisioning from PFP is targeted for domain-mode EAP6.1.1 wheare as
      jbpm installer targets standalone-mode JBoss AS7.1.1
    - An illustration of the deployment architecture provisioned by PFP is found 
      in the project here:  doc/pfp-deployment-architecture.png


2)  centralized configuration
    - similar to jbpm installer, PFP provides centralized configuration of BPMS6 
    - purpose
      - BPMS has configuration files throughout it's various sub-components
      - instead of manually modifying it's various config files, PFP allows
        the developer/admin to configure BPMS6 from a single property file
          

3)  seperation of jbpm core and bam database schemas
    - to support a 'distributed' deployment architecture of BPMS, PFP
      splits the default BPMS6 db schemas into two:
        1)  jbpm-core-cp
        2)  jbpm-bam


4)  EJB wrappers of CDI singleton services
    - provides EJB remote access to CDI singleton services
    - ideal when clients desire to participate in same XA trnx that process
      and task engines are using
