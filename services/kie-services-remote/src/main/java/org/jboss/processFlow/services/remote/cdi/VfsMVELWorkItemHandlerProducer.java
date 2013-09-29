package org.jboss.processFlow.services.remote.cdi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.kie.services.api.DeployedUnit;
import org.jbpm.kie.services.impl.VFSDeploymentService;
import org.jbpm.kie.services.impl.VFSDeploymentUnit;
import org.jbpm.runtime.manager.api.WorkItemHandlerProducer;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.IOException;
import org.kie.commons.java.nio.file.DirectoryStream;
import org.kie.commons.java.nio.file.Path;
import org.mvel2.MVEL;

/*
 * purpose:   inject workItemHandlers for VFS based deployment units
 *            workItemHandlers for KJar deployments are defined in the kJar's kmodule.xml
 *  
 * details:
 *    1)  ill load WorkItemHandlers defined in an MVEL file with a suffix of *.conf
 *    2)  searches for *.conf files in VFSDeploymentUnit's "repoFolder"
 *    3)  this class is instantiated and it's getWorkItemHandlers() function is invoked with any operation to a kieSession
 *       thus, for performance reasons, maintains temporary static data structure to cache MVEL defined workItemHandlers per deploymentId
 *    4)  this is a modification to org.jbpm.kie.services.impl.VfsMVELWorkItemHandlerProducer
 *        Need to investigate why CDI injection doesn't seem to be working in org.bpm.kie.services.impl.VfsMVELWorkItemHandlerProducer
 */
public class VfsMVELWorkItemHandlerProducer implements WorkItemHandlerProducer {


    // key = deploymentId
    // value = workItemHandlers defined for this deploymentId from MVEL formatted *.conf files
    private static Map<String, List<String>> mvelContentMap = new HashMap<String, List<String>>();
    
    private VFSDeploymentService deploymentService;
    private IOService ioService;
    
    //@Inject  // TO-DO:  investigate why can't seem to inject org.jbpm.shared.services.impl.VFSFileServiceImpl
    //private FileService fs;

    public VfsMVELWorkItemHandlerProducer(VFSDeploymentService x, IOService y) {
        this.deploymentService = x;
        this.ioService = y;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, WorkItemHandler> getWorkItemHandlers(String deploymentId, Map<String, Object> params) {
        Map<String, WorkItemHandler> handlers = new HashMap<String, WorkItemHandler>();

        try {
            if(!mvelContentMap.containsKey(deploymentId)){
                DeployedUnit deployedUnit = deploymentService.getDeployedUnit(deploymentId);
                VFSDeploymentUnit vfsUnit = (VFSDeploymentUnit) deployedUnit.getDeploymentUnit();
                Path assetFolder = ioService.get(vfsUnit.getRepository()+vfsUnit.getRepositoryFolder());
                Iterable<Path> widFiles = loadFilesByType(assetFolder, "conf");
                List<String> mvelContentList = new ArrayList<String>();
                for (Path widPath : widFiles) {
                    byte[] contentBytes = ioService.readAllBytes(widPath);
                    String content = new String(contentBytes, "UTF-8");
                    mvelContentList.add(content);
                }
                mvelContentMap.put(deploymentId, mvelContentList);
            }
            List<String> mvelContentList = mvelContentMap.get(deploymentId);
            for(String content : mvelContentList){
                handlers.putAll((Map<String, WorkItemHandler>) MVEL.eval( content, params ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return handlers;
    }
    
    public Iterable<Path> loadFilesByType( final Path path, final String fileType ) {
        return ioService.newDirectoryStream( path, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept( final Path entry ) throws IOException {
                if ( !org.kie.commons.java.nio.file.Files.isDirectory(entry) && 
                        (entry.getFileName().toString().endsWith( fileType )
                                || entry.getFileName().toString().matches(fileType))) {
                    return true;
                }
                return false;
            }
        } );
    }
}
