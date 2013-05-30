package org.jboss.processFlow.openshift;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class JsonParser {
    
    public static final String OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION  = "openshift.account.details.file.location";
    public static final String OPENSHIFT_PFP_PROPERTIES = "/openshift.pfp.properties";
    public static final String OPENSHIFT_DUMP_DIR="openshift.dump.dir";
    public static final String JSON_FILE = "json.file";
    
    private static Logger log = Logger.getLogger("JsonParser");
    private static String jsonFile;
    private static String openshiftDumpDir;
    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static DocumentBuilder builder;

    public static void main(String args[]) throws Exception{
        //getSystemProperties();
        //parse();
        addNodes();
    }
    
    private static void addNodes() throws Exception {
         String openshiftAccountDetailsFile = System.getProperty(OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION);
         File xmlFile = new File(openshiftAccountDetailsFile);
         if(!xmlFile.exists())
             throw new RuntimeException("provisionAccounts() can't find xml file: "+openshiftAccountDetailsFile);
         Document accountDetailsDoc = createDocument(xmlFile);
         XPath xpath = XPathFactory.newInstance().newXPath();
         xpath.setNamespaceContext(new AccountNameSpaceContext());
         XPathExpression expression = xpath.compile("/openshiftAccounts/account");
         NodeList accountsList = (NodeList)expression.evaluate(accountDetailsDoc, XPathConstants.NODESET);
         StringBuffer warningBuf = new StringBuffer();
         XPathExpression findPFPExpression = xpath.compile("//account/pfpcore");
         for(int p=0; p < accountsList.getLength(); p++){
             Node accountNameNode = accountsList.item(p);
             warningBuf.append("\n\t\t"+accountNameNode.getNodeValue());
             Element sshElement = accountDetailsDoc.createElement(ShifterProvisioner.SSH_URL);
             accountNameNode.appendChild(sshElement);
             Node sshNode = accountDetailsDoc.createTextNode("this is the sshUrl");
             sshElement.appendChild(sshNode);
             Node existingChildNode = (Node)findPFPExpression.evaluate(accountNameNode, XPathConstants.NODE);
             log.info("existingChildNode = "+existingChildNode);
         }
         // Use a Transformer for output
         TransformerFactory tFactory = TransformerFactory.newInstance();
         Transformer transformer = tFactory.newTransformer();
         transformer.setOutputProperty(OutputKeys.INDENT, "yes");

         DOMSource source = new DOMSource(accountDetailsDoc);
         FileOutputStream foStream = new FileOutputStream(xmlFile);
         StreamResult result = new StreamResult(foStream);
         transformer.transform(source, result);
         foStream.flush();
         foStream.close();
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
    
    private static Document createDocument(File fileObj) throws Exception {
        if(builder == null){
            builder = factory.newDocumentBuilder();
        }
        
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(fileObj);
            InputSource source = new InputSource(fileReader);
            return builder.parse(source);
        } finally {
            if(fileReader != null)
                fileReader.close();
        }
    }
}
