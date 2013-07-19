package org.jboss.processFlow.openshift;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class TransformAccountDetails  {
    private static final Logger log = Logger.getLogger(TransformAccountDetails.class);
    private static final Pattern accountIdPattern = Pattern.compile("<accountId>\n*(.*)</accountId>");
    private static final Pattern git_urlPattern = Pattern.compile("<git_url>\n*(.*)</git_url>");
    private static final Pattern app_urlPattern = Pattern.compile("<app_url>\n*(.*)</app_url>");
    private static final Pattern webUserIdPattern = Pattern.compile("<webUserId>\n*(.*)</webUserId>");
    private static final Pattern webPasswordPattern = Pattern.compile("<webPassword>\n*(.*)</webPassword>");
    private static final Pattern uuidPattern = Pattern.compile("<uuid>\n*(.*)</uuid>");
    private static final Pattern domainIdPattern = Pattern.compile("<domainId>\n*(.*)</domainId>");
    
    private static List<String> accountIdList;
    private static List<String> git_urlList;
    private static List<String> app_urlList;
    private static List<String> webUserIdList;
    private static List<String> uuidList;
    private static List<String> domainIdList;
    private static List<String> webPasswordList;

    public static String parse(InputStream inputStream) throws Exception
    {
        log.info("Matching OS Account Patterns");
        Matcher fit = null;
        String data = checkLines(new InputStreamReader(inputStream));
        //String data = IOUtils.toString( inputStream ).trim();
        StringBuffer result = new StringBuffer();
        
        accountIdList = getAllPattern(fit,accountIdPattern, data, "accountId");
        System.out.println("accountIdList.size(): " + accountIdList.size());
        
        git_urlList = getAllPattern(fit,git_urlPattern, data, "git_url");
        System.out.println("git_urlList.size(): " + git_urlList.size());
        
        app_urlList = getAllPattern(fit,app_urlPattern, data, "app_url");
        System.out.println("app_urlList.size(): " + app_urlList.size());
        
        webUserIdList = getAllPattern(fit,webUserIdPattern, data, "webUserId");
        System.out.println("webUserIdList.size(): " + webUserIdList.size());
        
        webPasswordList = getAllPattern(fit,webPasswordPattern, data, "webPassword");
        System.out.println("webPasswordList.size(): " + webPasswordList.size());
        
        uuidList = getAllPattern(fit,uuidPattern, data, "uuid");
        System.out.println("uuidList.size(): " + uuidList.size());
        
        domainIdList = getAllPattern(fit,domainIdPattern, data, "domainId");
        System.out.println("domainIdList.size(): " + domainIdList.size());
        
        boolean allmatching = true;
        int size =  accountIdList.size();
        if (size != git_urlList.size()) { System.out.println( "Missing a git URL? "); allmatching = false; }
        if (size != app_urlList.size()) { System.out.println( "Missing a app_url? "); allmatching = false; }
        if (size != webUserIdList.size()) { System.out.println( "Missing a webUserI? "); allmatching = false; }
        if (size != webPasswordList.size()) { System.out.println( "Missing a webPassword? "); allmatching = false; }
        if (size != uuidList.size()) { System.out.println( "Missing a git uuid? "); allmatching = false; }
        if (size != domainIdList.size()) { System.out.println( "Missing a domainId? "); allmatching = false; }
        
        if (allmatching)
        {
            for (int i = 0; i < accountIdList.size(); i++)
                result.append(accountIdList.get(i)).append("\t").append(uuidList.get(i)).append("\t").append(domainIdList.get(i)).append("\t").append(app_urlList.get(i))
                .append("\t").append(webUserIdList.get(i)).append("\t").append(webPasswordList.get(i)).append("\t").append(git_urlList.get(i)).append("\n");
        }
        
        return result.toString();
    }
    
    
    private static List<String> getAllPattern(Matcher fit, Pattern p, String data, String patternName) {
        fit = p.matcher(data);
        ArrayList<String> values = new ArrayList<String>();
        while (fit.find())
        {    try {  values.add(fit.group(1)); }
            catch(Exception e) { log.warn("Exception while parsing " + patternName + ": " + e); }
        }
        return values;
    }
    
    private static String checkLines(Reader input) throws IOException {
        String currentAccountId = "none";
        int lineNo = 100;
        
        BufferedReader reader = new BufferedReader(input);
        StringBuffer result = new StringBuffer();
        String line = reader.readLine();
        while (line != null) {
            result.append(line);
            result.append("\n");
            
            //System.out.println("LINE: \t" + line);
            
            if (line.contains("<account>") && lineNo > 13) { lineNo = 0; }
            //else { System.out.println( " - Error in Account: " + currentAccountId); break; }
            
            if (line.contains("<accountId>") && lineNo != 1) { System.out.println( " -- Error in Account: " + currentAccountId); break; }
            else if (line.contains("<accountId>") && lineNo == 1) { currentAccountId = line; }
            
            if (!inputMatch(line, "<password>", 2, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<domainId>", 3, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<pfpcore>", 4, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<uuid>", 5, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<git_url>", 6, lineNo, currentAccountId)) { break; }
            //if (!inputMatch(line, "pfpcore.git", 7, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<app_url>", 8, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<internal_ip>", 9, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<webUserId>", 10, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<webPassword>", 11, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<webAdminPassword>", 12, lineNo, currentAccountId)) { break; }
            if (!inputMatch(line, "<pfpPassword>", 13, lineNo, currentAccountId)) { break; }
            
            line = reader.readLine();
            lineNo++;
        }
        return result.toString();
    }
    
    private static boolean inputMatch(String line, String token, int expectedLineNo, int actualLineNo, String currentAccountId)
    {
        return true;
        /*if (line.contains(token))
        {
            if (! (line.contains(token) && (expectedLineNo == actualLineNo)))
            {
                System.out.println( "Error in Account: " + currentAccountId + " Current Token: " + token);
                return false;
            }
        }
        return true;
        */
    }

    public static void main(String args[]) {
        String pathToAccountDetails = System.getProperty("openshift.account.details.file.location");
        System.out.println("TransformAccountDetails.main() pathToAccountDetails = "+pathToAccountDetails);
        InputStream iStream = null;
        try {
            iStream = new FileInputStream(pathToAccountDetails);
            String accounts = TransformAccountDetails.parse(iStream);
            System.out.println(accounts);
        }catch(Exception x){
            x.printStackTrace();
        }finally{
            if(iStream != null) {
                try { iStream.close(); }catch(Exception x) {x.printStackTrace();}
            }
        }
    }

}
