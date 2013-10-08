package org.jboss.processFlow.services.asyncAuditLog;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.ActivationConfigProperty;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jbpm.process.audit.jms.AsyncAuditLogReceiver;

@MessageDriven(name="AuditLogMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue="processFlow.knowledgeSessionQueue")
})
public class AuditLogMDB extends AsyncAuditLogReceiver implements javax.jms.MessageListener {

    private static EntityManagerFactory jbpmAuditEMF;

    static{
        try {
            Context jContext = new InitialContext();
            jbpmAuditEMF = (EntityManagerFactory)jContext.lookup("java:/app/jbpmAuditEMF");
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    public AuditLogMDB() {
        super(jbpmAuditEMF);
    }
}
