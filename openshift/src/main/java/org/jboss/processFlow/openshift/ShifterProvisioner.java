package org.jboss.processFlow.openshift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;

public class ShifterProvisioner {

    public static final String OPENSHIFT_PFP_PROPERTIES = "/openshift.pfp.properties";
    public static final String OPENSHIFT_REST_URI = "openshift.rest.uri";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION  = "openshift.account.details.file.location";
    public static final String OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR  = "openshift.account.provisioning.log.dir";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_SCHEMA_FILE="/openshift_account_details.xsd";
    public static final String ACCOUNT_ID = "tns:accountId";
    public static final String PASSWORD = "tns:password";

    private static Logger log = Logger.getLogger("ShifterProvisioner"); 
    private static String openshiftRestURI;
    private static String openshiftAccountDetailsFile;
    private static String openshiftAccountProvisioningLogDir;
    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static DocumentBuilder builder;
    private static File accountLogDir;

    public static void main(String args[] ) throws Exception{
    	RegisterBuiltin.register(ResteasyProviderFactory.getInstance());
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
    	private OpenshiftClient osClient;
    	private DefaultHttpClient httpClient;
    	
		public ProvisionerThread(String accountId, String password){
			this.accountId = accountId;
			this.password = password;
			accountLog = new File(accountLogDir, accountId+".log");
		}
		public void run() {
			logBuilder.append("now provisioning openshift accountId = "+accountId);
			try {
				prepConnection();
				
				Response.Status sObj = osClient.getDomains();
				String message = "getDomains() response status = "+sObj.getStatusCode()+" : reason = "+sObj.getReasonPhrase();
				if(Status.OK ==  sObj)
					logBuilder.append(message);
				else
					throw new RuntimeException(message);
				
			}catch(Exception x){
				throw new RuntimeException(x);
			}finally{
				if(httpClient != null)
					httpClient.getConnectionManager().shutdown();
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
    	
		private void prepConnection() throws Exception {
			httpClient = new DefaultHttpClient();
			
			/* the following prevents this type of exception:  
			 * 		javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
			 * with this, the client does not need a valid public cert to conduct handshake with server
			 */
			SSLContext sslContext = SSLContext.getInstance( "SSL" );
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					System.out.println("getAcceptedIssuers =============");
					return null;
				}
				
				public void checkClientTrusted(X509Certificate[] certs,
						String authType) {
					System.out.println("checkClientTrusted =============");
				}
				
				public void checkServerTrusted(X509Certificate[] certs,
						String authType) {
					System.out.println("checkServerTrusted =============");
				}
			} }, new SecureRandom());
			SSLSocketFactory ssf = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = httpClient.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", 443, ssf));  
			
			//UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(accountId, password);
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "admin");
			
			
			    HttpGet httpget = new HttpGet(openshiftRestURI+"/domains");
			    httpget.setHeader("Accept", "application/json");
			    httpget.addHeader(BasicScheme.authenticate(credentials,"US-ASCII",false));

			    HttpResponse response = httpClient.execute(httpget);
			    HttpEntity entity = response.getEntity();
			    log.info("response = "+response);
			    InputStream content = (InputStream)entity.getContent();
                BufferedReader in =  new BufferedReader (new InputStreamReader (content));
                    
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                }
			
			
			URL urlObj = new URL(openshiftRestURI);
			AuthScope aScope = new AuthScope(urlObj.getHost(), urlObj.getPort());
			httpClient.getCredentialsProvider().setCredentials(aScope, credentials);
			
			httpClient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
			ApacheHttpClient4Executor cExecutor = new ApacheHttpClient4Executor(httpClient, new BasicHttpContext());
			osClient = ProxyFactory.create(OpenshiftClient.class, openshiftRestURI, cExecutor);
		}
    }
    
    
    static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {

        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
            request.setHeader("Accept", "application/json");

            if (authState.getAuthScheme() == null) {
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                if (creds == null)
                    throw new HttpException("No credentials for preemptive authentication");
                authState.setAuthScheme(new BasicScheme());
                authState.setCredentials(creds);
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

        if(props.getProperty(OPENSHIFT_REST_URI) == null)
            throw new RuntimeException("must pass system property : "+OPENSHIFT_REST_URI);
        openshiftRestURI = props.getProperty(OPENSHIFT_REST_URI);
        if(props.getProperty(OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION) == null)
            throw new RuntimeException("must pass system property : "+OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION);
        openshiftAccountDetailsFile = props.getProperty(OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION);
        if(props.getProperty(OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR) == null)
            throw new RuntimeException("must pass system property : "+OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR);
        openshiftAccountProvisioningLogDir = props.getProperty(OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR);

        StringBuilder sBuilder = new StringBuilder("setProps() props = ");
        sBuilder.append("\n\topenshiftRestURI = "+openshiftRestURI);
        sBuilder.append("\n\tpenshiftAccountDetailsFile = "+openshiftAccountDetailsFile);
        sBuilder.append("\n\topenshiftAccountProvisioningLogDir = "+openshiftAccountProvisioningLogDir);
        log.info(sBuilder.toString());
    }
}
