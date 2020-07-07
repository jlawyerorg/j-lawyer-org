/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlawyer.cloud;

import java.io.IOException;
import org.aarboard.nextcloud.api.NextcloudConnector;
import org.apache.log4j.Logger;

/**
 *
 * @author jens
 */
public class CloudFactory {
    
    private static final Logger log=Logger.getLogger(CloudFactory.class.getName());
    
    public static NextcloudConnector getCloud(String serverName, boolean useHTTPS, int port, String userName, String password) {
        NextcloudConnector con=new NextcloudConnector(serverName, useHTTPS, port, userName, password);
        return con;
    }
    
    public void close(NextcloudConnector cloud) {
        if(cloud!=null) {
            try {
                cloud.shutdown();
            } catch (IOException ex) {
                log.error(ex);
            }
        }
    }
    
}
