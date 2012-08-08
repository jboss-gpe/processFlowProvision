package org.jboss.processFlow.openshift;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

public class ShifterProvisioner {

    public static final String OPENSHIFT_PFP_PROPERTIES = "/openshift.pfp.properties";
    public static final String OPENSHIFT_HOST = "openshift.host";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION  = "openshift.account.details.file.location";
    public static final String OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR  = "openshift.account.provisioning.log.dir";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_SCHEMA_FILE="/openshift_account_details.xsd";

    private static Logger log = Logger.getLogger("ShifterProvisioner"); 
    private static String openshiftHost;
    private static String openshiftAccountDetailsFile;
    private static String openshiftAccountProvisioningLogDir;

    public static void main(String args[] ) throws Exception{
        setProps();
        validateAccountDetails();
    }
    
    public static void validateAccountDetails() throws Exception {
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
