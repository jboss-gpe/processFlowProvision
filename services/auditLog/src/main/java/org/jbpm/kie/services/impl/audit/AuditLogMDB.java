package org.jboss.processFlow.jbpm.kie.services.impl.audit;

import javax.annotation.PostConstruct;
import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jbpm.process.audit.jms.AsyncAuditLogReceiver;

/**
 * similiar to org.jbpm.kie.services.impl.audit.AuditJMSProcessor
 * different in that obtains a EntityManagerFactory that is specific to audit log data (ie:  jbpm core and bam databases are intentionally split )
 */
@MessageDriven(name="AuditLogMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue="processFlow.asyncWorkingMemoryLogger")
})
public class AuditLogMDB extends AsyncAuditLogReceiver implements javax.jms.MessageListener {

    private static EntityManagerFactory jbpmAuditEMF;
    private static Logger log = LoggerFactory.getLogger("AuditLogMDB");

    static{
        try {
            Context jContext = new InitialContext();
            jbpmAuditEMF = (EntityManagerFactory)jContext.lookup("java:/app/jbpmAuditEMF");
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }
    
    @PostConstruct
    public void start() {
        log.info("start() jbpmAuditEMF = "+jbpmAuditEMF);
    }

    public AuditLogMDB() {
        super(jbpmAuditEMF);
    }
}
