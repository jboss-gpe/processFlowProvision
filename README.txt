processFlowProvision (aka:  PFP)
====================

one approach toward a production BPM environment leveraging Red Hat BRMS "Deployable" libraries



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
  - PFP is a downstream project to BRMS 5.3 Deployable
  - purpose of PFP :
    - provide an example of one possible production BRMS environment using 
      BRMS deployable libraries
  - PFP github branches :
    - master :  PFP/BRMS targeted for "domain" managed JBoss EAP6
    - er8   :   PFP/BRMS ER8 targeted for non-clustered JBoss EAP 5.*
    - switchyard    :   automates provisioning of switchyard BPM component




LEGAL
  - PFP is copyright of Red Hat, Inc. and distributed with a LGPL license
  - PFP is maintained by Red Hat Global Solutions and Strategies Office
  - PFP is a community project with no contractual support offerings
  - Please contact Red Hat to discuss support details for BRMS "Deployable"



FEATURES 
1)  automated provisioning
    - Automates provisioning of BRMS deployable libraries on JBoss EAP 5.1
    - Automates Hornetq or MRG-M standalone configuration
    - Provides PostgreSQL configuration templates


2)  centralized configuration
    - centralized configuration of jbpm5 properties during build phase 
      - (via a single build.properties)
    - centralized configuration of jbpm5 properties during the runtime phase 
      - (via properties-service.xml)


3)  database integration
    - integrated and performance tested  using postgresql
    - all jbpm5 / drools components now using one of 3 JCA connection pools:
        1)  jbpm-core-cp
        2)  guvnor-cp
        3)  jbpm-bam


4)  EJB stateless/singleton services
    - Exposes full functionality of BRMS APIs to remote clients
    - Allows for scalability / fail-over in distributed environment
    - Allows for wrapping with REST or SOAP endpoints
    - Allows for runtime configuration of JAAS policies
        - Authentication requirements
        - Method-level authorization
        - Programmatic authorization via SessionContext


5)  task functionality
    - No longer uses a Mina /Hornet-q messaging provider nor jbpm5 "Task Server"
    - instead, exposes task related API as EJB3
    - greatly simplifies environment
    - substantial performance and concurrency improvements 
    - leverages BRMS TaskServiceSession directly
    - BRMS human task functionality is centralized


6)  StatefulKnowledgeSession functionality
    - “stateful knowledge session per processInstance” architecture
    - allows for concurrency without invoking various optimistic lock exceptions
    - recycles database SessionInfo records after process instance completion
    - drools/jbpm5 process engine functionality is centralized
    - forwards process engine BAM events to a messaging provider
    - significantly more performant than persisting BAM event to RDBMS in same thread of execution as process engine


7)  bam functionality
    - bam data maintains relationship between:
        parent process instances and its sub-process instances
    - this allows BAM reporting that can be depicted in a tree structure
    - within the BAM audit-trail tree structure, the BAM reports can be generated that include any human task variables that existed at that time
