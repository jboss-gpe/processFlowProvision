package org.jbpm.kie.services.impl.deploymentMgmt;

import static org.kie.scanner.MavenRepository.getMavenRepository;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.KieContainerImpl;
import org.drools.core.util.StringUtils;
import org.kie.internal.deployment.DeployedUnit;
import org.jbpm.kie.services.api.IdentityProvider;
import org.jbpm.kie.services.api.Kjar;
import org.jbpm.kie.services.api.bpmn2.BPMN2DataService;
import org.jbpm.kie.services.impl.AbstractDeploymentService;
import org.jbpm.kie.services.impl.DeployedUnitImpl;
import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.audit.ServicesAwareAuditEventBuilder;
import org.jbpm.kie.services.impl.event.DeploymentEvent;
//import org.jbpm.kie.services.impl.model.ProcessDesc;
import org.jbpm.process.audit.AbstractAuditLogger;
import org.jbpm.runtime.manager.impl.AbstractRuntimeManager;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.runtime.manager.impl.cdi.InjectableRegisterableItemsFactory;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.scanner.MavenRepository;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * replaces BPMS KModuleDeploymentService so as to specify additional config settings of the jbpm process engine
 */
@ApplicationScoped
@Kjar
public class PfpKModuleDeploymentService extends KModuleDeploymentService {

    private static Logger logger = LoggerFactory.getLogger(PfpKModuleDeploymentService.class);
    
    private static final String DEFAULT_KBASE_NAME = "defaultKieBase";

    @Inject
    private BeanManager beanManager;    
    @Inject
    private IdentityProvider identityProvider;
    @Inject
    private BPMN2DataService bpmn2Service;

    public void deploy(IPfpDeploymentUnit unit) {
        super.deploy(unit);
        if (!(unit instanceof KModuleDeploymentUnit)) {
            throw new IllegalArgumentException("Invalid deployment unit provided - " + unit.getClass().getName());
        }
        KModuleDeploymentUnit kmoduleUnit = (KModuleDeploymentUnit) unit;
        DeployedUnitImpl deployedUnit = new DeployedUnitImpl(unit);
        KieServices ks = KieServices.Factory.get();
        MavenRepository repository = getMavenRepository();
        repository.resolveArtifact(kmoduleUnit.getIdentifier());

        ReleaseId releaseId = ks.newReleaseId(kmoduleUnit.getGroupId(), kmoduleUnit.getArtifactId(), kmoduleUnit.getVersion());
        KieContainer kieContainer = ks.newKieContainer(releaseId);

        String kbaseName = kmoduleUnit.getKbaseName();
        if (StringUtils.isEmpty(kbaseName)) {
            KieBaseModel defaultKBaseModel = ((KieContainerImpl)kieContainer).getKieProject().getDefaultKieBaseModel();
            if (defaultKBaseModel != null) {
                kbaseName = defaultKBaseModel.getName();
            } else {
                kbaseName = DEFAULT_KBASE_NAME;
            }
        }
        InternalKieModule module = (InternalKieModule) ((KieContainerImpl)kieContainer).getKieModuleForKBase(kbaseName);
        if (module == null) {
            throw new IllegalStateException("Cannot find kbase with name " + kbaseName);
        }

        KieBase kbase = kieContainer.getKieBase(kbaseName);        

        AbstractAuditLogger auditLogger = getAuditLogger();
        ServicesAwareAuditEventBuilder auditEventBuilder = new ServicesAwareAuditEventBuilder();
        auditEventBuilder.setIdentityProvider(identityProvider);
        auditEventBuilder.setDeploymentUnitId(unit.getIdentifier());
        auditLogger.setBuilder(auditEventBuilder);

        // JA Bride:  instantiate either JPA or in-memory RuntimeEnvironmentBuilder
        RuntimeEnvironmentBuilder builder;
        if(IPfpDeploymentUnit.ProcessEnginePersistenceType.IN_MEMORY == unit.getProcessEnginePersistenceType()){
            builder = RuntimeEnvironmentBuilder.getDefaultInMemory();
        } else {
            builder = RuntimeEnvironmentBuilder.getDefault().entityManagerFactory(getEmf());
        }

        // JA Bride:  add additional ObjectMarshallingStrategies if specified in configuration
        if(unit.getMarshallingStrategies() != null){
            Set<ObjectMarshallingStrategy> omStrategies = unit.getMarshallingStrategies();
            builder.addEnvironmentEntry(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, omStrategies.toArray());
        }

        
        if (beanManager != null) {
            builder.registerableItemsFactory(InjectableRegisterableItemsFactory.getFactory(beanManager, auditLogger, kieContainer,
                    kmoduleUnit.getKsessionName()));
        }
        commonDeploy(unit, deployedUnit, builder.get());
    }
    
    public void undeploy(KModuleDeploymentUnit unit) {
        
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
