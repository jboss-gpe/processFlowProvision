package org.jboss.processFlow.openshift;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class JsonParser {
	
	public static final String OPENSHIFT_PFP_PROPERTIES = "/openshift.pfp.properties";
	public static final String OPENSHIFT_DUMP_DIR="openshift.dump.dir";
	public static final String JSON_FILE = "json.file";
	
	private static Logger log = Logger.getLogger("JsonParser");
	private static String jsonFile;
	private static String openshiftDumpDir;

    public static void main(String args[]) throws Exception{
    	getSystemProperties();
    	parse();
    }
    
    private static void parse() throws Exception {
    	String jsonString = null;
    	InputStream iStream = null;
        try {
        	String filePath = openshiftDumpDir+jsonFile;
            iStream = new FileInputStream(filePath);
            if(iStream == null)
                throw new RuntimeException("parse() unable to find the following file: "+filePath);

            jsonString = IOUtils.toString(iStream);
            iStream.close();
        }catch(RuntimeException x) {
            throw x;
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    	ObjectMapper jsonMapper = new ObjectMapper();
    	JsonNode rootNode = jsonMapper.readValue(jsonString, JsonNode.class);
    	log.info("parse() current_ip (external) = "+rootNode.path("messages").path(1).path("text").getTextValue());
    	log.info("parse() sshUrl = "+rootNode.path("data").path("ssh_url").getTextValue().substring(6));
    	log.info("parse() uuid= "+rootNode.path("data").path("uuid").getTextValue());
    	log.info("parse() git_url = "+rootNode.path("data").path("git_url").getTextValue());
    	log.info("parse() app_url= "+rootNode.path("data").path("app_url").getTextValue());
    	log.info("parse() gear_count= "+rootNode.path("data").path("gear_count") );
    	
    }
    
    private static void getSystemProperties() {
        InputStream iStream = null;
        Properties props = null;
        try {
            iStream = JsonParser.class.getResourceAsStream(OPENSHIFT_PFP_PROPERTIES);
            if(iStream == null)
                throw new RuntimeException("setProps() unable to find the following file on classpath : "+OPENSHIFT_PFP_PROPERTIES);

            props = new Properties();
            props.load(iStream);
            
            iStream.close();
        }catch(RuntimeException x) {
            throw x;
        }catch(Exception x) {
            throw new RuntimeException(x);
        }

        if(props.getProperty(JSON_FILE) == null)
            throw new RuntimeException("must pass system property : "+JSON_FILE);
        jsonFile = props.getProperty(JSON_FILE);

        openshiftDumpDir = props.getProperty(OPENSHIFT_DUMP_DIR, "/tmp/openshift/dump/");
        
        StringBuilder sBuilder = new StringBuilder("setProps() props = ");
        sBuilder.append("\n\tjsonFile = "+jsonFile);
        sBuilder.append("\n\topenshiftDumpDir = "+openshiftDumpDir);
        log.info(sBuilder.toString());
    }
}
