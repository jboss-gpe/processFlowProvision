package org.jboss.processFlow;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;


public class GuvnorRestApi {
    
    private String guvnorURI;
    private InputStream iStream;
    
    public GuvnorRestApi(String guvnorURI){
        this.guvnorURI = guvnorURI;
    }
    
    public InputStream getBinaryPackage(String packageName) throws Exception {
        URL url = new URL(guvnorURI + "/rest/packages/"+packageName+"/binary");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM);
        
        String userpassword = "admin:admin";
        String encodedAuth = Base64.encodeBase64String(userpassword.getBytes());

        connection.setRequestProperty("Authorization", "Basic "+encodedAuth);
        
        connection.connect();
        if(connection.getResponseCode() != 200){
            throw new Exception("Bad response code: "+connection.getResponseCode());
        }
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
