package org.jbpm.kie.services.impl.deploymentMgmt;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.jbpm.kie.services.impl.deploymentMgmt.IPfpDeploymentUnit.ProcessEnginePersistenceType;
import org.kie.internal.deployment.DeploymentService;
import org.kie.internal.deployment.DeploymentUnit;
import org.kie.internal.deployment.DeploymentUnit.RuntimeStrategy;
import org.jbpm.kie.services.api.Kjar;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.uberfire.io.FileSystemType;
import org.uberfire.java.nio.file.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.uberfire.io.FileSystemType.Bootstrap.BOOTSTRAP_INSTANCE;

/*
 purpose:  Create and trigger org.jbpm.kie.services.impl.event.Deploy &  org.jbpm.kie.services.impl.event.UnDeploy events.
    - there is one type of Deploy events:
        1)  import org.jbpm.kie.services.impl.KModuleDeploymentUnit
    - These events are captured by org.kie.services.remote.cdi.RuntimeManagerManager (which is the entry point into the BPMS "Execution Server")
        - the Execution Server subsequently uses these Deploy events to instantiate and populate corresponding kieBase objects via a KieContainer
        - org.kie.api.runtime.KieContainer is the container for all the KieBases of a given KieModule

    - Note:  
        - KieContainer, KModule and KieBase objects are all created via use KModuleDeploymentUnit
        - KModuleDeploymentUnit(s)
             - both ksession strategy and workItemHandler mappings are included in kJar's META-INF/kmodule.xml

    - NOTE:  regarding workItemHandler mapping registration, can also :
        1)  define mapping in:  WEB-INF/classes/META-INF/CustomWorkItemHandlers.conf
        2)  define programmatically via:  org.drools.core.command.runtime.process.RegisterWorkItemHandlerCommand
*/
@ApplicationScoped
public class DeploymentMgmtBean implements IDeploymentMgmtBean {

    private static Logger log = LoggerFactory.getLogger("DeploymentMgmtBean");
    
    @Inject
    @Kjar // org.jboss.processFlow.services.remote.cdi.PfpKModuleDeploymentService extends org.jbpm.kie.services.impl.KModuleDeploymentService
    private DeploymentService kjarService;
    
    private List<DeploymentUnit> dUnits = new CopyOnWriteArrayList<DeploymentUnit>();
    
    public static enum LocalFileSystemType implements FileSystemType {
        LOCAL_FILE_SYSTEM;
        
        public String toString() {
            return "LOCAL_FILE_SYSTEM";
        }
    }

    public void deployKJar(IPfpDeploymentUnit dUnit) {
        String dIdentifier = dUnit.getIdentifier();
        if(kjarService.getDeployedUnit(dIdentifier) == null){
            try {

                /* following stack is executed IOT create a new org.kie.api.runtime.KieContainer
                            org.drools.compiler.kie.builder.impl.KieServicesImpl.newKieContainer(KieServicesImpl.java:84)
                            org.jbpm.kie.services.impl.KModuleDeploymentService.deploy(KModuleDeploymentService.java:69)
                            org.jbpm.kie.services.impl.KModuleDeploymentService.deploy()
                            org.jboss.processFlow.services.remote.cdi.RESTApplicationStartup.start(RESTApplicationStartup.java)
                 */
                kjarService.deploy(dUnit);

                log.info("start() just deployed the following Kjar dUnit : {}", dIdentifier);
            }catch(Throwable x){
                log.error("start() exception thrown when attempting to deploy the following KJar deploymentUnit : "+dIdentifier);
                x.printStackTrace();
            }
        }else {
            log.error("start() uh-oh .... not going to attempt to start the following KJar deployment unit cause already registered : {}", dIdentifier);
        }
    }
    public void start() throws Exception {
        log.info("start() org.uberfire.nio.git.daemon.enabled = "+System.getProperty("org.uberfire.nio.git.daemon.enabled"));
        
        List<DeploymentUnit> localScopedDUnits = this.getDeploymentUnits();
        for(DeploymentUnit dUnit : localScopedDUnits){
            if (dUnit instanceof KModuleDeploymentUnit){
                deployKJar((PfpKModuleDeploymentUnit)dUnit);
                dUnits.add(dUnit);
            }else{
                log.error("start() unknown DeploymentUnit type: "+dUnit);
            }
        }
    }

    public void undeployKJar(DeploymentUnit dUnit) {
         kjarService.undeploy(dUnit);
         dUnits.remove(dUnit);
    }
    public void stop(){
         for (DeploymentUnit dUnit : dUnits) {
             if(dUnit instanceof PfpKModuleDeploymentUnit){
                 if(kjarService.getDeployedUnit(dUnit.getIdentifier()) == null)
                     log.error("stop() uh-oh .... not going to attempt to undeploy the following KModule dUnit which was previously not deployed : {}", dUnit.getIdentifier());
                 else{
                     log.info("stop() about to stop following KModule deployment unit : {}", dUnit.getIdentifier());
                     undeployKJar(dUnit);
                 }
             }else{
                 log.error("stop() unit deploymentUnit type = "+dUnit.getClass().toString());
             }
         }
    }
    
    private List<DeploymentUnit> getDeploymentUnits() throws Exception {

        List<DeploymentUnit> localScopedDUnits = new ArrayList<DeploymentUnit>();
        Map<String, Map<String, String>> deployments = DeployUnitParser.getParsedJsonConfig();
        //To-Do:  parse and iterate through a JSON based config file 
        for(Entry<String, Map<String, String>> deployment : deployments.entrySet()) {
            DeploymentUnit dUnit = null;
            if(DeployUnitParser.KJAR.equals(deployment.getKey())) {
                validateDeploymentUnitFields(deployment);
                dUnit = this.createKModuleDeploymentUnit(deployment.getValue());
            } else
                throw new Exception("getDeploymentUnits() unknown deployment type: "+deployment.getKey());

            localScopedDUnits.add(dUnit);
        }
        return localScopedDUnits;
    }
    
    private KModuleDeploymentUnit createKModuleDeploymentUnit(Map<String, String> details) {
        RuntimeStrategy ksessionStrategy = RuntimeStrategy.valueOf(details.get(DeployUnitParser.KSESSION_STRATEGY));
        String dId = details.get(DeployUnitParser.DEPLOYMENT_ID);
        String groupId = details.get(DeployUnitParser.GROUP_ID);
        String artifactId = details.get(DeployUnitParser.ARTIFACT_ID);
        String version = details.get(DeployUnitParser.VERSION);
        String kbaseName = details.get(DeployUnitParser.KBASE_NAME);
        String ksessionName = details.get(DeployUnitParser.KSESSION_NAME);
        ProcessEnginePersistenceType engineType = null;
        String engineTypeString = details.get(DeployUnitParser.ENGINE_TYPE);
        if(engineTypeString != null)
            engineType = ProcessEnginePersistenceType.valueOf(engineTypeString);
        else
            engineType = ProcessEnginePersistenceType.JPA;

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("createKModuleDeploymentUnit() creating KJar deploymentUnit with \n\tdeploymentId = ");
        sBuilder.append(dId);
        sBuilder.append("\n\tgroupId = ");
        sBuilder.append(groupId);
        sBuilder.append("\n\tartifactId = ");
        sBuilder.append(artifactId);
        sBuilder.append("\n\tversion = ");
        sBuilder.append(version);
        sBuilder.append("\n\tksessionStrategy = ");
        sBuilder.append(ksessionStrategy.toString());
        sBuilder.append("\n\tkieBase name = ");
        sBuilder.append(kbaseName);
        sBuilder.append("\n\tkieSession name = ");
        sBuilder.append(ksessionName);
        sBuilder.append("\n\tengineType = ");
        sBuilder.append(engineType);
        log.info(sBuilder.toString());

        PfpKModuleDeploymentUnit kUnit = new PfpKModuleDeploymentUnit(groupId, artifactId, version);
        if (StringUtils.isNotEmpty(kbaseName)) {
            kUnit.setKbaseName(kbaseName);
        }
        if (StringUtils.isNotEmpty(ksessionName)) {
            kUnit.setKsessionName(ksessionName);
        }
        
        // confused about kmodule.xml schema:  https://github.com/droolsjbpm/droolsjbpm-knowledge/blob/master/kie-api/src/main/resources/org/kie/api/kmodule.xsd
        // ksessionType can only be "stateless" or "stateful" ?? .... what about SINGLETON or PER_PROCESS_INSTANCE ??
        kUnit.setStrategy(ksessionStrategy);
        kUnit.setProcessEnginePersistenceType(engineType);
        return kUnit;
    }
    
    private void validateDeploymentUnitFields(Entry<String, Map<String, String>> deployment) throws Exception {
        Map<String, String> details = deployment.getValue();
        if(!(deployment.getKey().equals(DeployUnitParser.KJAR)) && StringUtils.isEmpty(details.get(DeployUnitParser.DEPLOYMENT_ID)))
            throw new Exception("Deployment Unit needs a property of : "+DeployUnitParser.DEPLOYMENT_ID);
        
        String kSessionStrategy = details.get(DeployUnitParser.KSESSION_STRATEGY);
        if(StringUtils.isEmpty(kSessionStrategy))
            throw new Exception("All Deployment Units to specify a ksession strategy: "+DeployUnitParser.KSESSION_STRATEGY);
        
        try {
            RuntimeStrategy.valueOf(kSessionStrategy);
        } catch (IllegalArgumentException ex) {  
            throw new Exception("following ksession strategy is not valid: "+kSessionStrategy+" for deploymentId: "+details.get(DeployUnitParser.DEPLOYMENT_ID));
        }
    }
}
