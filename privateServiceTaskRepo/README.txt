PURPOSE :
  provide a working demonstration of how to build a custom service task repository


documentation :
    please review the following sections in the jbpm user doc for details:
        #  1)  14.2.1: Creating the work definition
        #  2)  14.3  : Service Repository


Configuration :
      please review the following property in processFlowProvision/build.properties :

        service.repository.url

      by default, service.repository.url is set to the jbpm5 public service task repository at :  http://people.redhat.com/kverlaen/repository

      to enable this example repository rather than the default jbpm5 public repo, please comment out this default value and replace with :
        service.repository=${ENV.JBOSS_PROJECTS}/jboss/serviceRepository
        service.repository.url=file://${ENV.JBOSS_PROJECTS}/jboss/serviceRepository


build process
    from processFlowProvision home directory execute :
        1)  ant
                among many other tasks, sets appropriate value for service.repository property in runtime designer.war/profiles/jbpm.xml

        2)  ant serviceTasksTest
                builds the demo service repository on local disk


usage
    start BRMS runtime and use the new service task as per the jbpm5 user documentation

    
    

09:03:26,026 SEVERE [JbpmServiceRepositoryServlet] Could not find the package for uuid: a5c048d9-9dc2-4be0-b35c-01c337021d47
09:03:26,065 WARNING [WebApplicationExceptionMapper] WebApplicationException has been caught : javax.jcr.PathNotFoundException: refundTask.wid
09:03:26,066 INFO  [JbpmServiceRepositoryServlet] check wid connection response code: 500
09:03:26,079 WARNING [WebApplicationExceptionMapper] WebApplicationException has been caught : javax.jcr.PathNotFoundException: refundTask.png
09:03:26,079 INFO  [JbpmServiceRepositoryServlet] check icon connection response code: 500
09:03:26,250 WARN  [ValueConstraint] validation of reference constraint is not yet implemented
09:03:26,408 INFO  [STDOUT] created wid configuration:200
09:03:26,424 WARNING [WebApplicationExceptionMapper] WebApplicationException has been caught : A rule of that name already exists in that package.
09:03:26,426 INFO  [JbpmServiceRepositoryServlet] icon creation response code: 500
09:03:26,426 INFO  [STDOUT] created icon:500

