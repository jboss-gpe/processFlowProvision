package org.jbpm.kie.services.impl.deploymentMgmt;

import java.util.Set;

import org.kie.internal.deployment.DeploymentUnit;
import org.kie.api.marshalling.ObjectMarshallingStrategy;

// new DeploymentUnit interface until equivalent is implemented in BPMS6 base product as per this BZ:
// https://bugzilla.redhat.com/show_bug.cgi?id=1017327
public interface IPfpDeploymentUnit extends DeploymentUnit {
    
    public enum ProcessEnginePersistenceType {
        JPA,
        IN_MEMORY;
    }
    
    Set<ObjectMarshallingStrategy> getMarshallingStrategies();
    ProcessEnginePersistenceType getProcessEnginePersistenceType();
}
