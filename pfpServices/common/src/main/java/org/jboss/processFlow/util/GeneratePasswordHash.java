package org.jboss.processFlow.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.jboss.sasl.util.UsernamePasswordHashUtil;

public class GeneratePasswordHash {

    public static final String USER_ID = "USER_ID";
    public static final String REALM = "REALM";
    public static final String PASSWORD = "PASSWORD";
    
    public static void main(String args[]) throws Exception {

        String userId = System.getProperty(USER_ID);
        String realm = System.getProperty(REALM);
        String password = System.getProperty(PASSWORD);
        File mgtUsersPropsFile = new File(System.getProperty("MGT_USERS_PROPS_FILE"));
        if(!mgtUsersPropsFile.exists())
        	throw new RuntimeException("main() following file not found : "+System.getProperty("MGT_USERS_PROPS_FILE"));

        // given userId and realm, generate a random password and run it through a hash generator 
        String adminHash = new UsernamePasswordHashUtil().generateHashedHexURP(userId, "ManagementRealm", password.toCharArray());
        System.out.println("UsernamePasswordHashUtil.main() props file = "+System.getProperty("MGT_USERS_PROPS_FILE")+" : userId = "+userId+" : password = "+password+" : adminHash = "+adminHash);
        
        StringBuilder propsBuilder = new StringBuilder("\n");
        propsBuilder.append(userId);
        propsBuilder.append("=");
        propsBuilder.append(adminHash);
        
        FileWriter newPropsWriter = new FileWriter(mgtUsersPropsFile.getAbsolutePath(), true);
        BufferedWriter bufferedWriter = new BufferedWriter(newPropsWriter);
        bufferedWriter.write(propsBuilder.toString());
        bufferedWriter.close();
    }
}
