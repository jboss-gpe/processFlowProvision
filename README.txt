processFlowProvision (aka:  PFP)
====================

one approach toward a production BPM environment leveraging Red Hat BRMS "Deployable" libraries



Jeff Bride

welcome Java developers to Process Flow Provision
you have reached the location where PFP source code is centrally maintained

for Java /*nix administrators, please review :
    1)  docs/ADMIN_GUIDE.txt 

for Java developers, please review docs/ADMIN_GUIDE.txt and in addition:
    2)  docs/DEVELOPER_GUIDE.txt



OVERVIEW
  - PFP is a downstream project to BRMS 5.3.* "Deployable"
  - purpose of PFP :
    - provide an example of one possible production BRMS environment using 
      BRMS deployable libraries


PFP github branches :
    - master

    - 5.3.1_eap6.1.1
        - targets "domain" managed JBoss EAP6.1.1 in local environment
        - targets 'standalone' EAP 6 in 'openshift' mode
        - leverages Red Hat/JBoss BRMS5.3.1 Deployable
        - configurable to use either mysql, oracle or postgres
        - leverages embedded hornetq provided by EAP 6
            - alternative is JBoss AMQ
        - pfp services implemented using CDI & JPA 2.1 with EJB 3.1 wrappings
        - provisions additional droolsjbpm web archives such as :
            1)  jboss-brms
            2)  web designer
            3)  business-central-server
            4)  business-central
        - provisioning scripts work only in a *nix variant (read: no windows)

    - 6.1.0
        - tracks Red Hat / JBoss BPMS6
        - used as one of the key components of:   "BPMS6 Engine" cartridge for Openshift

PFP github tags :
    - 5.3.1_eap6.0
        - used for provisioning the Openshift Online accounts that students of our "BLD Workshop" work on

    - 5.3.0.GA    
        - targeted for non-clustered JBoss EAP 5.*
        - tracks BRMS 5.3.* releases
        - pfp services implemented using EJB 3.0 and JPA 1.0 & MBeans
        - postgresql is the only database supported
        - provisioning scripts work on any operating system
        - requires separate 'standalone' hornetq 2.2.14 provider





LEGAL
  - PFP is copyright of Red Hat, Inc. and distributed with a LGPL license
  - PFP is maintained by Red Hat Global Partner Strategy & Enablement Office
  - PFP is a community project with no contractual support offerings
  - Please contact Red Hat to discuss support details for BRMS "Deployable"



FEATURES 
1)  automated provisioning
    - Automates provisioning of BRMS deployable libraries on JBoss EAP in a *nix environment
    - Automates configuration of hornetq in ha clustered mode
    - Provides PostgreSQL, mysql and Oracle RDBMS configuration templates


2)  centralized configuration
    - centralized configuration of jbpm properties during build phase 
      - (via a single build.properties)
    - purpose
        - BRMS deployable has configuration files throughout it's various 
          sub-components
        - instead of manually modifying it's various config files, PFP allows
          the developer/admin to configure properties from a single properties
          file
          


3)  database integration
    - performance tested using postgresql
    - all jbpm / drools components now using one of 3 JCA connection pools:
        1)  jbpm-core-cp
        2)  jbpm-bam
        3)  guvnor-cp
    - centralized configuration for the following databases:
        1)  postgresql
        2)  mysql
        3)  oracle 11


4)  CDI singleton services
    - Exposes full functionality of BRMS APIs to remote clients
    - Allows for scalability / fail-over in distributed environment
    - Allows for wrapping with EJB, REST or SOAP endpoints
    - avoids management of jbpm/drools knowledge sessions in client code
    - simplifies usability of the jpm5 engine from the client perspective


5)  EJB wrappers of CDI singleton services
    - provides remote access to CDI singleton services
    - Allows for runtime configuration of JAAS policies
        - Authentication requirements
        - Method-level authorization
        - Programmatic authorization via SessionContext


6)  task functionality
    - No longer uses a Mina /Hornet-q messaging provider nor jbpm "Task Server"
    - instead, exposes task related API as EJB3
    - greatly simplifies environment
    - substantial performance and concurrency improvements 
    - leverages BRMS TaskServiceSession directly
    - BRMS human task functionality is centralized


7)  StatefulKnowledgeSession management:
    - drools/jbpm process engine functionality is centralized
    - forwards process engine BAM events to a messaging provider
    - significantly more performant than persisting BAM event to RDBMS in same
      thread of execution as process engine
    - two implementions:
        1)  PER_PROCESS_INSTANCE strategy
          - one StatefulKnoweldgeSession is dedicated for lifecycle of a process instance
          - allows for concurrency and scalability
          - appropriate when bpmn2 definitions do not include rules nodes
          - prevents optimistic lock exceptions that may occur in concurrent
            environments
          - recycles database SessionInfo records after process completion
            -prevents SessionInfo table from continuosly growing
          - quartz timers
            - timers included in a BPMN2 process definition are scheduled in a 
              database backed Quartz Scheduler
            - doing so allows timer triggers to fire in Quartz while a process instance
              is in a wait state
        2)  SINGLETON strategy
          - one StatefulKnowledgeSession is instantiated per JVM
          - appropriate when bpmn2 definitions include rules nodes 


8)  bam functionality
    - bam data maintains relationship between:
        parent process instances and its sub-process instances
    - this allows BAM reporting that can be depicted in a tree structure
    - within the BAM audit-trail tree structure, the BAM reports can be generated that include any human task variables that existed at that time



GETTING STARTED
    - in the doc directory of this project is an ADMIN_GUIDE.txt
