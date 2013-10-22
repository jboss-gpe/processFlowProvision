package org.jboss.processFlow.services.remote.cdi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jbpm.kie.services.api.DeployedUnit;
import org.jbpm.kie.services.api.DeploymentUnit;
import org.jbpm.kie.services.api.IdentityProvider;
import org.jbpm.kie.services.api.Vfs;
import org.jbpm.kie.services.impl.DeployedUnitImpl;
import org.jbpm.kie.services.impl.VFSDeploymentService;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.jbpm.kie.services.impl.audit.ServicesAwareAuditEventBuilder;
import org.jbpm.kie.services.impl.event.DeploymentEvent;
import org.jbpm.kie.services.impl.model.ProcessInstanceDesc;
import org.jbpm.process.audit.AbstractAuditLogger;
import org.jbpm.runtime.manager.impl.AbstractRuntimeManager;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.runtime.manager.impl.cdi.InjectableRegisterableItemsFactory;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;


/**
 * replaces BPMS VFSDeploymentService so as to specify additional config settings of the jbpm process engine
 *
 */
@ApplicationScoped
@Vfs
public class PfpVFSDeploymentService extends VFSDeploymentService {
    
    @Inject
    private BeanManager beanManager;
    
    @Inject
    private IdentityProvider identityProvider;
    
    public void deploy(IPfpDeploymentUnit pfpUnit) {
        
        if (deploymentsMap.containsKey(pfpUnit.getIdentifier())) {
            throw new IllegalStateException("Unit with id " + pfpUnit.getIdentifier() + " is already deployed");
        }
        if (!(pfpUnit instanceof VFSDeploymentUnit)) {
            throw new IllegalArgumentException("Invalid deployment unit provided - " + pfpUnit.getClass().getName());
        }

        DeployedUnitImpl deployedUnit = new DeployedUnitImpl(pfpUnit);
        VFSDeploymentUnit vfsUnit = (VFSDeploymentUnit) pfpUnit;
        
        // JA Bride:  instantiate either JPA or in-memory RuntimeEnvironmentBuilder
        RuntimeEnvironmentBuilder builder;
        if(IPfpDeploymentUnit.ProcessEnginePersistenceType.IN_MEMORY == pfpUnit.getProcessEnginePersistenceType()){
            builder = RuntimeEnvironmentBuilder.getDefaultInMemory();
        } else {
            builder = RuntimeEnvironmentBuilder.getDefault().entityManagerFactory(getEmf());
        }
        
        // JA Bride:  add additional ObjectMarshallingStrategies if specified in configuration
        if(pfpUnit.getMarshallingStrategies() != null){
            Set<ObjectMarshallingStrategy> omStrategies = pfpUnit.getMarshallingStrategies();
            builder.addEnvironmentEntry(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, omStrategies.toArray());
        }

        AbstractAuditLogger auditLogger = getAuditLogger();
        ServicesAwareAuditEventBuilder auditEventBuilder = new ServicesAwareAuditEventBuilder();
        auditEventBuilder.setIdentityProvider(identityProvider);
        auditEventBuilder.setDeploymentUnitId(vfsUnit.getIdentifier());
        auditLogger.setBuilder(auditEventBuilder);
        if (beanManager != null) {
            builder.registerableItemsFactory(InjectableRegisterableItemsFactory.getFactory(beanManager, auditLogger));
        }
        loadProcesses(vfsUnit, builder, deployedUnit);
        loadRules(vfsUnit, builder, deployedUnit); 

        commonDeploy(vfsUnit, deployedUnit, builder.get());
    }
    
    public void undeploy(DeploymentUnit unit) {
        
        /*  JA Bride:  TO-DO :
         *   - current implementation attempts to query jbm core database to determine active process instances by deploymentId
         *   - if infact needed, then need to modify to invoke remote BAM service to determine this information
         
        List<Integer> states = new ArrayList<Integer>();
        states.add(ProcessInstance.STATE_ACTIVE);
        states.add(ProcessInstance.STATE_PENDING);
        states.add(ProcessInstance.STATE_SUSPENDED);
        Collection<ProcessInstanceDesc> activeProcesses = runtimeDataService.getProcessInstancesByDeploymentId(unit.getIdentifier(), states);
        if (!activeProcesses.isEmpty()) {
            throw new IllegalStateException("Undeploy forbidden - there are active processes instances for deployment " 
                                            + unit.getIdentifier());
        }*/
        synchronized (this) {
            DeployedUnit deployed = deploymentsMap.remove(unit.getIdentifier());
            if (deployed != null) {
                RuntimeManager manager = deployed.getRuntimeManager();
                ((AbstractRuntimeManager)manager).close(true);
            }
            if (undeploymentEvent != null) {
                undeploymentEvent.fire(new DeploymentEvent(unit.getIdentifier(), deployed));
            }
        }
    }

}
