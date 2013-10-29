package org.jboss.processFlow.deploymentMgmt;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployUnitParser {
    
    public static final String DEPLOYMENTS_JSON_CONFIG_PATH = "org.jboss.processFlow.deployments.json.config.path";
    
    public static final String KSESSION_STRATEGY = "ksession.strategy";
    public static final String ENGINE_TYPE = "engine.type";
    public static final String MARSHALLING_STRATEGIES = "marshalling.strategies";
    public static final String DEPLOYMENT_ID = "deploymentId";
    public static final String REPO_FOLDER = "repoFolder";
    public static final String REPO_ALIAS = "repoAlias";
    public static final String REPO_SCHEME = "repoScheme";
    public static final String GIT_OUT_DIR = "gitOutDir";
    public static final String LOCAL_FILE_SYSTEM = "localFileSystem";
    public static final String GIT = "git";
    public static final String FILE = "file";
    public static final String KJAR = "kjar";
    public static final String GIT_USER = "gitUser";
    public static final String GIT_PASSWD = "gitPasswd";
    public static final String GIT_REMOTE_REPO_URL = "remoteGitUrl";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String GROUP_ID = "groupId";
    public static final String VERSION = "version";
    public static final String KBASE_NAME = "kbase-name";
    public static final String KSESSION_NAME = "ksession-name";
    
    private static Logger log = LoggerFactory.getLogger("RESTApplicationScopedProducer");
    private static Map<String, Map<String, String>> parsedJson;
    private static Object parseLock = new Object();
    
    /*
     * parse a json file of KieContainer properties.
     */
    public static Map<String, Map<String, String>> getParsedJsonConfig() throws Exception {
        if(parsedJson != null)
            return parsedJson;
        
        synchronized(parseLock){
            if(parsedJson != null)
                return parsedJson;
            
            String jsonConfigPath = System.getProperty(DEPLOYMENTS_JSON_CONFIG_PATH);
            if(jsonConfigPath == null || jsonConfigPath.equals(""))
                throw new Exception("parseFromJsonConf() please set a value for the following system property: "+DEPLOYMENTS_JSON_CONFIG_PATH);

            InputStream iStream = DeployUnitParser.class.getResourceAsStream(jsonConfigPath);
            if(iStream == null){
                log.warn("parseFromJsonConf() the following json config not on the classpath: "+jsonConfigPath+" : will try on filesystem");
                iStream = new FileInputStream(jsonConfigPath);
                if(iStream == null)
                    throw new Exception("parseFromJsonConf() following json config not on filesystem: "+jsonConfigPath);
            }

            ObjectMapper jsonMapper = new ObjectMapper();
            parsedJson = jsonMapper.readValue(iStream, new TypeReference<Map<String, Map<String, Object>>>() {});
            iStream.close();
            return parsedJson;
        }
    }
    
    public static Map<String, Map<String, String>> reloadParsedJsonConfig() throws Exception {
        parsedJson = null;
        return getParsedJsonConfig();
    }

}
