/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.cloud.loadbalancer;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;



/**
 * Retrieves the configuration from  and polls for changes. When change occurs,
 *  the HTTPSLoadBalancer is re-initialized
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public class ConfigurationPoller {
	HttpsLoadBalancer lb;
	private static final transient Logger LOGGER = Logger
			.getLogger(ConfigurationPoller.class);
	String prevKeyPass = "";
	String prevKeyStore = "";
	String prevSslPass = "";
	Timer t;
	
	public ConfigurationPoller(HttpsLoadBalancer lb){
		this.lb = lb;
		//Set configurations to the current state
		prevKeyPass = lb.getKsPasswd();
		prevKeyStore = lb.getKeystoreLoc();
		prevSslPass = lb.getKsManPasswd();
		
		t = new Timer();
		t.scheduleAtFixedRate(
		    new TimerTask()
		    {
		        public void run()
		        {
		            checkUpdateConfiguration();
		        }
		    },
		    0,      // run first occurrence immediately
		    3000);  // run every three seconds
	}
	
	private void checkUpdateConfiguration(){
		boolean recreateRoute = false;
		 try
	        {
			 	BundleContext bundleContext = lb.getBundleContext();
	            ServiceReference configAdminServiceRef = bundleContext.getServiceReference( ConfigurationAdmin.class.getName() );
	            if ( configAdminServiceRef != null )
	            {
	                ConfigurationAdmin ca = (ConfigurationAdmin) bundleContext.getService( configAdminServiceRef );
	                LOGGER.debug( "configuration admin obtained: " + ca );
	                if ( ca != null )
	                {
	                    Configuration web = ca.getConfiguration( "org.ops4j.pax.web" );
	                    String keyPass = ((String) web.getProperties().get("org.ops4j.pax.web.ssl.keypassword"));
	                    String keyStore = ((String) web.getProperties().get("org.ops4j.pax.web.ssl.keystore"));
	                    String sslPass = ((String) web.getProperties().get("org.ops4j.pax.web.ssl.password"));
	                    
	                    /**
	                     * If the configuration is different from the one before, update in HttpsLoadBalancer
	                     * , reset the previous config value, and set flag to recreate route for balancer.
	                     */
	                    if(!keyPass.equals(prevKeyPass)){
	                    	prevKeyPass = keyPass;
	                    	recreateRoute = true;
	                    } 
	                    
	                    if(!keyStore.equals(prevKeyStore)){
	                    	prevKeyStore = keyStore;
	                    	recreateRoute = true;
	                    }
	                    
	                    if(!sslPass.equals(prevSslPass)){
	                    	prevSslPass = sslPass;
	                    	recreateRoute = true;
	                    }
	                    
	                    if (recreateRoute){
	                    	//Rebuild the Camel route for the HTTPS Load Balancer
	                    	lb.init();
	                    }
	                }
	            }
	        }
	        catch ( IOException ioe )
	        {
	            LOGGER.warn( "Unable to obtain the configuration admin" );
	        }
	}
	
	public void destroy(){
		t.cancel();
	}

}
