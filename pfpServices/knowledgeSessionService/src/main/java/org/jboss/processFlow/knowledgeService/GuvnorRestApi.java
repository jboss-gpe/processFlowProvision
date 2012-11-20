package org.jboss.processFlow.knowledgeService;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.ws.rs.core.MediaType;

import sun.misc.BASE64Encoder;

import org.apache.log4j.Logger;

public class GuvnorRestApi {
   
    private Logger log = Logger.getLogger("GuvnorRestApi"); 
    private String guvnorURI;
    private InputStream iStream;
    
    public GuvnorRestApi(String guvnorURI){
        this.guvnorURI = guvnorURI;
    }
    
    public InputStream getBinaryPackage(String packageName) throws java.io.IOException {
        String urlString = guvnorURI + "/rest/packages/"+packageName+"/binary";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM);
        
        BASE64Encoder enc = new sun.misc.BASE64Encoder();
        String userpassword = "admin:admin";
        String encodedAuth = enc.encode(userpassword.getBytes());
        connection.setRequestProperty("Authorization", "Basic "+encodedAuth);
        
        connection.connect();
        log.info("getBinaryPackage() response code for GET request to : "+urlString+" is : "+connection.getResponseCode());
        iStream = connection.getInputStream();
        return iStream;
    }
    
    public void close() {
        if(iStream != null) {
            try {                
                iStream.close();
            }catch(Exception x){ x.printStackTrace();    }
        }
    }

}
