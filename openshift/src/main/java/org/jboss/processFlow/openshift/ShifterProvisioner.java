package org.jboss.processFlow.openshift;

import java.io.InputStream;
import java.util.Properties;
import org.apache.log4j.Logger;

public class ShifterProvisioner {

	public static final String OPENSHIFT_PFP_PROPERTIES = "/openshift.pfp.properties";
    public static final String OPENSHIFT_HOST = "openshift.host";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION  = "openshift.account.details.file.location";
    public static final String OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR  = "openshift.account.provisioning.log.dir";

    private static Logger log = Logger.getLogger("ShifterProvisioner"); 
    private static String openshiftHost;

    public static void main(String args[] ) {
        setProps();
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

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("\n\topenshiftHost = "+openshiftHost);
        log.info(sBuilder.toString());
    
    }
}
