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
import org.apache.http.client.methods.HttpDelete;
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
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.spi.MarshalledEntity;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GenericType;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;

public class ShifterProvisioner {

    public static final String OPENSHIFT_PFP_PROPERTIES = "/openshift.pfp.properties";
    public static final String OPENSHIFT_REST_URI = "openshift.rest.uri";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_FILE_LOCATION  = "openshift.account.details.file.location";
    public static final String OPENSHIFT_ACCOUNT_PROVISIONING_LOG_DIR  = "openshift.account.provisioning.log.dir";
    public static final String OPENSHIFT_ACCOUNT_DETAILS_SCHEMA_FILE="/openshift_account_details.xsd";
    public static final String OPENSHIFT_BRMS_WEBS_APP_SIZE="openshift.brmsWebs.app.size";
    public static final String OPENSHIFT_PFP_CORE_APP_SIZE="openshift.pfpCore.app.size";
    public static final String OPENSHIFT_PFP_CORE_SCALED_APP="openshift.pfpCore.scaled.app";
    public static final String ACCOUNT_ID = "tns:accountId";
    public static final String PASSWORD = "tns:password";
    public static final String DOMAIN_ID = "tns:domainId";
    public static final String REFRESH_DOMAIN ="openshift.account.refresh.domain";
    public static final String CREATE_PFP_CORE="openshift.account.create.pfp.core";
    public static final String CREATE_BRMS_WEBS="openshift.account.create.brms.webs";
    public static final String DUMP_RESPONSE_TO_FILE="openshift.dump.response.to.file";
    public static final String OPENSHIFT_DUMP_DIR="openshift.dump.dir";
    
    public static final String DATA = "data";
    public static final String LINKS = "links";
    public static final String ADD_APPLICATION = "ADD_APPLICATION";
    public static final String LIST_APPLICATIONS = "LIST_APPLICATIONS";
    public static final String GET = "GET";
    public static final String DELETE = "DELETE";
    public static final String HREF = "href";
    public static final String START = "START";
    public static final String GET_DOMAIN = "GET_DOMAIN";
    public static final String DELETE_DOMAIN = "DELETE_DOMAIN";
    public static final String CREATE_DOMAIN = "CREATE_DOMAIN";
    public static final String BRMS_WEBS = "brmsWebs";
    public static final String PFP_CORE = "pfpCore";
    public static final String LARGE = "large";
    public static final String MEDIUM = "medium";
    public static final String SMALL = "small";
    public static final String JBOSSAS7 = "jbossas-7";
    public static final String EAP6 = "jbosseap-6.0";
    public static final String MESSAGES = "messages";
    public static final String ADD_DATABASE_CARTRIDGE = "ADD_DATABASE_CARTRIDGE";
    public static final String POSTGRESQL_8_4 = "postgresql-8.4";
    public static final String TEXT = "text";
    



    private static Logger log = Logger.getLogger("ShifterProvisioner"); 
    private static String openshiftRestURI;
    private static String openshiftAccountDetailsFile;
    private static String openshiftAccountProvisioningLogDir;
    private static String openshiftBrmsWebsAppSize = SMALL;
    private static String openshiftPfpCoreAppSize = SMALL;
    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static DocumentBuilder builder;
    private static File accountLogDir;
    private static boolean refreshDomain=true;
    private static boolean createPfpCore=true;
    private static boolean createBrmsWebs=true;
    private static boolean openshiftPfpCoreScaledApp=false;
    private static boolean dumpResponseToFile=false;
    private static String openshiftDumpDir;
    private static boolean dumpDirCreated = false;

    public static void main(String args[] ) throws Exception{
        RegisterBuiltin.register(ResteasyProviderFactory.getInstance());
        
        getSystemProperties();
        validateAccountDetailsXmlFile();
        provisionAccounts();
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
                
                /*  now parsing openshift accounts
                 *  will iterate through each account and with each account will spawn a new thread
                 *  thread is responsible for provisioning that openshift account
                 */
                String accountId = null;
                String password = null;
                String domainId = null;
                NodeList accountDetailsList = ((Element)account).getChildNodes();
                for(int y=0; y<=accountDetailsList.getLength(); y++){
                    Node detail = accountDetailsList.item(y);
                    if(detail != null && detail.getNodeType() == Node.ELEMENT_NODE){
                        Element detailElem = (Element)detail;
                        if(ACCOUNT_ID.equals(detailElem.getNodeName()))
                            accountId = detailElem.getTextContent();
                        else if(PASSWORD.equals(detailElem.getNodeName()))
                            password = detailElem.getTextContent();
                        else if(DOMAIN_ID.equals(detailElem.getNodeName()))
                            domainId = detailElem.getTextContent();
                        else
                            throw new RuntimeException("provisionAccounts() invalid Element = "+detailElem.getNodeName()+" : in file = "+openshiftAccountDetailsFile);
                    }
                }
                ProvisionerThread shifterProvisioner = new ProvisionerThread(accountId, password, domainId);
                Thread pThread = new Thread(shifterProvisioner);
                pThread.start();
            }
        }
    }
    
    static class ProvisionerThread implements Runnable {

        private String accountId;
        private String password;
        private String domainId;
        private File accountLog;
        private StringBuilder logBuilder = new StringBuilder();
        private OpenshiftClient osClient;
        private DefaultHttpClient httpClient;
        private ObjectMapper jsonMapper;
        private String body = null;
        
        public ProvisionerThread(String accountId, String password, String domainId){
            this.accountId = accountId;
            this.password = password;
            this.domainId = domainId;
            accountLog = new File(accountLogDir, accountId+".log");
            jsonMapper = new ObjectMapper();
        }
        public void run() {
            logBuilder.append("\n\n"+START+" :  now provisioning openshift accountId = "+accountId);
            try {
                prepConnection();
                prepRESTeasy();
                if(refreshDomain)
                	refreshDomain();
                if(createPfpCore)
                	createPfpCore();
                if(createBrmsWebs)
                	createBrmsWebs();
            }catch(Exception x){
                throw new RuntimeException(x);
            }finally{
                if(httpClient != null)
                    httpClient.getConnectionManager().shutdown();
                if(accountLog != null){
                    FileOutputStream fStream = null;
                    try {
                    	//write log to file:  append only if refreshDomain == false
                        fStream = new FileOutputStream(accountLog, !refreshDomain);
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
        
        private void createPfpCore() throws Exception {
            log.info(CREATE_PFP_CORE);
            ClientResponse<?> cResponse = osClient.createApp(domainId, PFP_CORE, EAP6, Boolean.toString(openshiftPfpCoreScaledApp), openshiftPfpCoreAppSize);
            consumeEntityAndCheckResponse(CREATE_PFP_CORE, cResponse);
            dumpResponseToFile(CREATE_PFP_CORE);
            JsonNode rootNode = jsonMapper.readValue(body, JsonNode.class);
            logAppDetails(rootNode);
            
            log.info(ADD_DATABASE_CARTRIDGE);
            cResponse = osClient.addCartridge(domainId, PFP_CORE, POSTGRESQL_8_4);
            consumeEntityAndCheckResponse(ADD_DATABASE_CARTRIDGE, cResponse);
            dumpResponseToFile(ADD_DATABASE_CARTRIDGE);
            rootNode = jsonMapper.readValue(body, JsonNode.class);
            String dbAddResponseText = rootNode.path(MESSAGES).path(1).path(TEXT).getTextValue();
            logBuilder.append("\n\t");
            logBuilder.append(dbAddResponseText);

        }
        private void createBrmsWebs() throws Exception {
            log.info(CREATE_BRMS_WEBS);
            ClientResponse<?> cResponse = osClient.createApp(domainId, BRMS_WEBS, EAP6, "false", openshiftBrmsWebsAppSize);
            consumeEntityAndCheckResponse(CREATE_BRMS_WEBS, cResponse);
            dumpResponseToFile(CREATE_BRMS_WEBS);
            JsonNode rootNode = jsonMapper.readValue(body, JsonNode.class);
            logAppDetails(rootNode);
            
            log.info(ADD_DATABASE_CARTRIDGE);
            cResponse = osClient.addCartridge(domainId, BRMS_WEBS, POSTGRESQL_8_4);
            consumeEntityAndCheckResponse(ADD_DATABASE_CARTRIDGE, cResponse);
            dumpResponseToFile(ADD_DATABASE_CARTRIDGE);
            rootNode = jsonMapper.readValue(body, JsonNode.class);
            String dbAddResponseText = rootNode.path(MESSAGES).path(1).path(TEXT).getTextValue();
            logBuilder.append("\n\t");
            logBuilder.append(dbAddResponseText);
        }
        private void refreshDomain() throws Exception {
            log.info(GET_DOMAIN);
            // 1)  get any existing openshift domains for this account
            ClientResponse<?> cResponse = osClient.getDomains();
            consumeEntityAndCheckResponse(GET_DOMAIN, cResponse);
            
            JsonNode rootNode = jsonMapper.readValue(body, JsonNode.class);
            String deleteDomainHref = rootNode.path(DATA).path(0).path(LINKS).path(DELETE).path(HREF).getTextValue();
            if(deleteDomainHref == null){
                logBuilder.append("\n\n"+DELETE_DOMAIN+": refreshDomain() no pre-existing domain found");
            }else {
                log.info(DELETE_DOMAIN);
                // 2)  this is an existing openshift domain.  delete the domain along with any apps
                HttpDelete httpRequest = new HttpDelete(deleteDomainHref+"?force=true");
                httpRequest.setHeader("Accept", "*/*");
                HttpResponse dResponse = httpClient.execute(httpRequest);
                checkResponse(DELETE_DOMAIN, dResponse);
            }
            
            // 3) create a new domain using the openshift domainId (which must be unique across all openshift)
            log.info(CREATE_DOMAIN);
            cResponse = osClient.createDomain(domainId);
            consumeEntityAndCheckResponse(CREATE_DOMAIN, cResponse);
        }
        private void logAppDetails(JsonNode rootNode) {
        	logBuilder.append("\n\tdomain_id= "+rootNode.path(DATA).path("domain_id").getTextValue());
        	logBuilder.append("\n\tcurrent_ip (external) = "+rootNode.path("messages").path(1).path("text").getTextValue());
        	logBuilder.append("\n\tinternal_ip = TO_DO:  openshift API does not provide in response");
        	logBuilder.append("\n\tsshUrl = "+rootNode.path(DATA).path("ssh_url").getTextValue().substring(6));
        	logBuilder.append("\n\tuuid= "+rootNode.path(DATA).path("uuid").getTextValue());
        	logBuilder.append("\n\tgit_url = "+rootNode.path(DATA).path("git_url").getTextValue());
        	logBuilder.append("\n\tapp_url= "+rootNode.path(DATA).path("app_url").getTextValue());
        	logBuilder.append("\n\tgear_count= "+rootNode.path(DATA).path("gear_count") );
        	logBuilder.append("\n\tgear_profile= "+rootNode.path(DATA).path("gear_profile").getTextValue() );
        }
        private void dumpResponseToFile(String fileName) throws Exception {
        	if(!dumpResponseToFile)
        		return;
        	
        	FileOutputStream fStream = null;
        	try{
        		if(!dumpDirCreated){
        			File dirObj = new File(openshiftDumpDir);
        			if(!dirObj.exists()){
        				dirObj.mkdirs(); 
        			}
        			dumpDirCreated = true;
        		}
        		File fileObj = new File(openshiftDumpDir+fileName+".dump");
        		fStream = new FileOutputStream(fileObj, false);
        		fStream.write(body.getBytes());
        	}finally {
        		if(fStream != null)
        			fStream.close();
        	}
        }
        private void checkResponse(String eventName, HttpResponse response) throws Exception {
            int status = response.getStatusLine().getStatusCode();
            StringBuilder messageBuilder = new StringBuilder("\n\n"+eventName+" : response status = "+status+" : phrase = "+response.getStatusLine().getReasonPhrase());
            if(status > 204){
                HttpEntity entity = null;
                InputStream content = null;
                try {
                    entity = response.getEntity();
                    if(entity != null){    
                        content = (InputStream)entity.getContent();
                        BufferedReader in =  new BufferedReader (new InputStreamReader (content));
                        String line;
                        messageBuilder.append("\n");
                        while ((line = in.readLine()) != null) {
                            messageBuilder.append(line);
                        }
                    }
                }finally {
                    if(content != null){ content.close(); }
                }
                logBuilder.append(messageBuilder.toString());
                throw new RuntimeException(messageBuilder.toString());
            }else {
                logBuilder.append(messageBuilder.toString());
            }
        }
        private void consumeEntityAndCheckResponse(String eventName, ClientResponse response) {
            int status = response.getStatus();
            MarshalledEntity<String> mBody = (MarshalledEntity<String>) response.getEntity(new GenericType<MarshalledEntity<String>>(){});
            body = mBody.getEntity();
            StringBuilder messageBuilder = new StringBuilder("\n\n"+eventName+" : response status = "+status);
            if(status > 204) {
                messageBuilder.append("\n"+body);
                logBuilder.append(messageBuilder.toString());
                throw new RuntimeException(messageBuilder.toString());
            }else {
                logBuilder.append(messageBuilder.toString());
            }
        }
        
        // Openshift broker uses BASIC authentication ... this function preps http client to support BASIC auth
        private void prepConnection() throws Exception {
            
            httpClient = new DefaultHttpClient();
            
            /* the following prevents this type of exception:  
             *         javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
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
            
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(accountId, password);
            /*  httpClient mechanism ... not used in favor of using RESTeasy client
                HttpGet httpget = new HttpGet(openshiftRestURI+"/domains");
                httpget.setHeader("Accept", "application/json");
                httpget.addHeader(BasicScheme.authenticate(credentials,"US-ASCII",false));
             */
            
            URL urlObj = new URL(openshiftRestURI);
            AuthScope aScope = new AuthScope(urlObj.getHost(), urlObj.getPort());
            httpClient.getCredentialsProvider().setCredentials(aScope, credentials);
            
            httpClient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
        }
        private void prepRESTeasy() {
            ApacheHttpClient4Executor cExecutor = new ApacheHttpClient4Executor(httpClient, new BasicHttpContext());
            osClient = ProxyFactory.create(OpenshiftClient.class, openshiftRestURI, cExecutor);
        }
    }
    
    
    
    /*
     * ensures that first request to openshift broker includes authentication credentials in header
     * otherwise, apache commons httpClient will fire off an initial request that does NOT include auth credentials
     * Not including auth credentials in the first request is per the http specification
     * however, public openshift is misbehaving such that if auth credentials are not included in a request, the openshift broker fails a returns a 500 Internal Server Error
     * see :  http://stackoverflow.com/questions/9539141/httpclient-sends-out-two-requests-when-using-basic-auth
     */
    static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {

        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

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
    
    private static void validateAccountDetailsXmlFile() throws Exception {
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

    private static void getSystemProperties() {
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
        
        if(props.getProperty(OPENSHIFT_BRMS_WEBS_APP_SIZE) != null)
            openshiftBrmsWebsAppSize = props.getProperty(OPENSHIFT_BRMS_WEBS_APP_SIZE);
        if(props.getProperty(OPENSHIFT_PFP_CORE_APP_SIZE) != null)
            openshiftPfpCoreAppSize = props.getProperty(OPENSHIFT_PFP_CORE_APP_SIZE);
        
        openshiftDumpDir = props.getProperty(OPENSHIFT_DUMP_DIR, "/tmp/openshift/dump/");
        
        if(props.getProperty(REFRESH_DOMAIN) != null)
        	refreshDomain = Boolean.parseBoolean(props.getProperty(REFRESH_DOMAIN));
        if(props.getProperty(CREATE_PFP_CORE) != null)
        	createPfpCore = Boolean.parseBoolean(props.getProperty(CREATE_PFP_CORE));
        if(props.getProperty(CREATE_BRMS_WEBS) != null)
        	createBrmsWebs = Boolean.parseBoolean(props.getProperty(CREATE_BRMS_WEBS));
        if(props.getProperty(OPENSHIFT_PFP_CORE_SCALED_APP) != null)
        	openshiftPfpCoreScaledApp = Boolean.parseBoolean(props.getProperty(OPENSHIFT_PFP_CORE_SCALED_APP));
        if(props.getProperty(DUMP_RESPONSE_TO_FILE) != null)
        	dumpResponseToFile = Boolean.parseBoolean(props.getProperty(DUMP_RESPONSE_TO_FILE));

        StringBuilder sBuilder = new StringBuilder("setProps() props = ");
        sBuilder.append("\n\topenshiftRestURI = "+openshiftRestURI);
        sBuilder.append("\n\tpenshiftAccountDetailsFile = "+openshiftAccountDetailsFile);
        sBuilder.append("\n\topenshiftAccountProvisioningLogDir = "+openshiftAccountProvisioningLogDir);
        sBuilder.append("\n\topenshift.brmsWebs.app.size = "+openshiftBrmsWebsAppSize);
        sBuilder.append("\n\topenshift.pfpCore.app.size = "+openshiftPfpCoreAppSize);
        sBuilder.append("\n\trefreshDomain = "+refreshDomain);
        sBuilder.append("\n\tcreatePfpCore = "+createPfpCore);
        sBuilder.append("\n\tcreateBrmsWebs = "+createBrmsWebs);
        sBuilder.append("\n\topenshiftPfpCoreScaledApp = "+openshiftPfpCoreScaledApp);
        sBuilder.append("\n\tdumpResponseToFile = "+dumpResponseToFile);
        sBuilder.append("\n\topenshift.dump.dir = "+openshiftDumpDir);
        log.info(sBuilder.toString());
    }
}
