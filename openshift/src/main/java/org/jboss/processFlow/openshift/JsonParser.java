package org.jboss.processFlow.openshift;

import java.io.InputStream;
import java.util.Properties;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class JsonParser {
	
	public static final String OPENSHIFT_PFP_PROPERTIES = "/openshift.pfp.properties";
	public static final String JSON_FILE = "json.file";
	
	private static Logger log = Logger.getLogger("JsonParser");
	private static String jsonFile;

    public static void main(String args[]) throws Exception{
    	getSystemProperties();
    	parse();
    }
    
    private static void parse() throws Exception {
    	String jsonString = null;
    	InputStream iStream = null;
        try {
            iStream = JsonParser.class.getResourceAsStream("/"+jsonFile);
            if(iStream == null)
                throw new RuntimeException("parse() unable to find the following file on classpath : "+jsonFile);

            jsonString = IOUtils.toString(iStream);
            iStream.close();
        }catch(RuntimeException x) {
            throw x;
        }catch(Exception x) {
            throw new RuntimeException(x);
        }
    	ObjectMapper jsonMapper = new ObjectMapper();
    	JsonNode rootNode = jsonMapper.readValue(jsonString, JsonNode.class);
    	JsonNode firstNode = rootNode.path("messages").path(1).path("text");
    	log.info("parse() string = "+firstNode);
    	
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

        StringBuilder sBuilder = new StringBuilder("setProps() props = ");
        sBuilder.append("\n\tjsonFile = "+jsonFile);
        log.info(sBuilder.toString());
    }
}
