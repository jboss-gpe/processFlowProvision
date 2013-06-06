package org.jboss.processFlow.util;

import javax.naming.InitialContext;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import org.kie.internal.command.Context;
import org.drools.core.command.impl.GenericCommand;
import org.drools.core.command.impl.KnowledgeCommandContext;
import org.kie.api.runtime.KieSession;

/* Completely plagarized from Maciej Swiderski's blog at :   http://mswiderski.blogspot.com/2012/10/dispose-session-in-cmt-environment.html
 * 
 * updated to work with new BPMS6/BRMS6 API
 */
public class CMTDisposeCommand implements GenericCommand<Void> {

    private static final long serialVersionUID = 1L;
    
    private String tmLookupName = "java:jboss/TransactionManager";
    
    public CMTDisposeCommand() {
        
    }

    public CMTDisposeCommand(String tmLookup) {
        this.tmLookupName = tmLookup;
    }

    @Override
    public Void execute(Context context) {
        
        final KieSession ksession = ((KnowledgeCommandContext) context).getKieSession();
        try {
            TransactionManager tm = (TransactionManager) new InitialContext().lookup(tmLookupName);
            tm.getTransaction().registerSynchronization(new Synchronization() {
                
                @Override
                public void beforeCompletion() {
                    // not used here
                    
                }
                
                @Override
                public void afterCompletion(int arg0) {
                    ksession.dispose();
                    
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }   
        
        return null;
    }

}
