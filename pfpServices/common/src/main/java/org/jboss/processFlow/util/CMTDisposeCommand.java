package org.jboss.processFlow.util;

import javax.naming.InitialContext;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import org.drools.command.Context;
import org.drools.command.impl.GenericCommand;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.runtime.StatefulKnowledgeSession;

/* Completely plagarized from Maciej Swiderski's blog at :   http://mswiderski.blogspot.com/2012/10/dispose-session-in-cmt-environment.html
 *
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
        
        final StatefulKnowledgeSession ksession = ((KnowledgeCommandContext) context).getStatefulKnowledgesession();
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
