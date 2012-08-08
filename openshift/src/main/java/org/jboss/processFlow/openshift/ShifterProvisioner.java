package org.jboss.processFlow.openshift;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

public class ShifterProvisioner {

    public static final String OPENSHIFT_PFP_PROPERTIES = "/openshift.pfp.properties";
    public static final String OPENSHIFT_HOST = "openshift.host";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION  = "openshift.account.details.file.location";
    public static final String OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR  = "openshift.account.provisioning.log.dir";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_SCHEMA_FILE="/openshift_account_details.xsd";
    public static final String ACCOUNT_ID = "tns:accountId";
    public static final String PASSWORD = "tns:password";

    private static Logger log = Logger.getLogger("ShifterProvisioner"); 
    private static String openshiftHost;
    private static String openshiftAccountDetailsFile;
    private static String openshiftAccountProvisioningLogDir;
    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static DocumentBuilder builder;
    private static File accountLogDir;

    public static void main(String args[] ) throws Exception{
        setProps();
        validateAccountDetails();
        provisionAccounts();
    }
    
    private static Document createDocument(File fileObj) throws Exception {
        if(builder == null)
            builder = factory.newDocumentBuilder();
        
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
    
    private static void provisionAccounts() throws Exception {
        File xmlFile = new File(openshiftAccountDetailsFile);
        if(!xmlFile.exists())
            throw new RuntimeException("provisionAccounts() can't find xml file: "+openshiftAccountDetailsFile);
        
        accountLogDir = new File(openshiftAccountProvisioningLogDir);
        accountLogDir.mkdirs();
        
        Document accountDetailsDoc = createDocument(xmlFile);
        Element rootElement = (Element)accountDetailsDoc.getFirstChild();
        NodeList accountsList = rootElement.getChildNodes();
        for(int t=0; t<= accountsList.getLength(); t++){
            Node account = accountsList.item(t);
            if(account != null && account.getNodeType() == Node.ELEMENT_NODE) {
            	String accountId = null;
            	String password = null;
                NodeList accountDetailsList = ((Element)account).getChildNodes();
                for(int y=0; y<=accountDetailsList.getLength(); y++){
                	Node detail = accountDetailsList.item(y);
                	if(detail != null && detail.getNodeType() == Node.ELEMENT_NODE){
                		Element detailElem = (Element)detail;
                		if(ACCOUNT_ID.equals(detailElem.getNodeName()))
                			accountId = detailElem.getTextContent();
                		else if(PASSWORD.equals(detailElem.getNodeName()))
                			password = detailElem.getTextContent();
                		else
                			throw new RuntimeException("provisionAccounts() invalid Element = "+detailElem.getNodeName()+" : in file = "+openshiftAccountDetailsFile);
                	}
                }
                ProvisionerThread shifterProvisioner = new ProvisionerThread(accountId, password);
                Thread pThread = new Thread(shifterProvisioner);
                pThread.start();
            }
        }
    }
    
    static class ProvisionerThread implements Runnable {

    	private String accountId;
    	private String password;
    	private File accountLog;
    	private StringBuilder logBuilder = new StringBuilder();
    	
		public ProvisionerThread(String accountId, String password){
			this.accountId = accountId;
			this.password = password;
			accountLog = new File(accountLogDir, accountId+".log");
		}
		public void run() {
			logBuilder.append("now provisioning openshift accountId = "+accountId);
			try {
				
			}finally{
				if(accountLog != null){
					FileOutputStream fStream = null;
					try {
						fStream = new FileOutputStream(accountLog);
						fStream.write(logBuilder.toString().getBytes());
					}catch(Exception x){
						x.printStackTrace();
					}finally {
						if(fStream != null)
							try{fStream.close();}catch(Exception x){x.printStackTrace();}
					}
					
				}
			}
		}
    	
    }
    
    private static void validateAccountDetails() throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        InputStream xsdStream = null;
        InputStream xmlStream = null;
        try {
            xsdStream = ShifterProvisioner.class.getResourceAsStream(OPENSHIFT_ACCOUNT_DETAILS_SCHEMA_FILE);
            if(xsdStream == null)
                throw new RuntimeException("validateAccountDetails() can't find schema: "+OPENSHIFT_ACCOUNT_DETAILS_SCHEMA_FILE);
            Schema schemaObj = schemaFactory.newSchema(new StreamSource(xsdStream));

            File xmlFile = new File(openshiftAccountDetailsFile);
            if(!xmlFile.exists())
                throw new RuntimeException("validateAccountDetails() can't find xml file: "+openshiftAccountDetailsFile);
            
            xmlStream = new FileInputStream(xmlFile);
            Validator v = schemaObj.newValidator();
            v.validate(new StreamSource(xmlStream));
        }finally {
            if(xsdStream != null)
                xsdStream.close();
            if(xmlStream != null)
                xmlStream.close();
        }
    }
    
    private static void setProps() {
        InputStream iStream = null;
        Properties props = null;
        try {
            iStream = ShifterProvisioner.class.getResourceAsStream(OPENSHIFT_PFP_PROPERTIES);
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

        if(props.getProperty(OPENSHIFT_HOST) == null)
            throw new RuntimeException("must pass system property : "+OPENSHIFT_HOST);
        openshiftHost = props.getProperty(OPENSHIFT_HOST);
        if(props.getProperty(OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION) == null)
            throw new RuntimeException("must pass system property : "+OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION);
        openshiftAccountDetailsFile = props.getProperty(OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION);
        if(props.getProperty(OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR) == null)
            throw new RuntimeException("must pass system property : "+OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR);
        openshiftAccountProvisioningLogDir = props.getProperty(OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR);

        StringBuilder sBuilder = new StringBuilder("setProps() props = ");
        sBuilder.append("\n\topenshiftHost = "+openshiftHost);
        sBuilder.append("\n\tpenshiftAccountDetailsFile = "+openshiftAccountDetailsFile);
        sBuilder.append("\n\topenshiftAccountProvisioningLogDir = "+openshiftAccountProvisioningLogDir);
        log.info(sBuilder.toString());
    }
}
