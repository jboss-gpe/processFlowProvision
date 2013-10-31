package org.jbpm.kie.services.impl.deploymentMgmt;

import java.util.Set;

import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.kie.api.marshalling.ObjectMarshallingStrategy;

public class PfpVFSDeploymentUnit extends VFSDeploymentUnit implements IPfpDeploymentUnit {
    
    private ProcessEnginePersistenceType processEnginePersistenceType = ProcessEnginePersistenceType.JPA;
    private Set<ObjectMarshallingStrategy> marshallingStrategies = null;

    public PfpVFSDeploymentUnit(String identifier, String repositoryAlias, String repositoryFolder) {
        super(identifier, repositoryAlias, repositoryFolder);
    }

    @Override
    public Set<ObjectMarshallingStrategy> getMarshallingStrategies() {
        return marshallingStrategies;
    }
    public void setMarshallingStrategies(Set<ObjectMarshallingStrategy> x){
        marshallingStrategies = x;
    }

    @Override
    public ProcessEnginePersistenceType getProcessEnginePersistenceType() {
        return processEnginePersistenceType;
    }
    public void setProcessEnginePersistenceType(ProcessEnginePersistenceType x){
        processEnginePersistenceType = x;
    }

}
