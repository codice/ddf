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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.LoadBalanceDefinition;
import org.apache.camel.model.LoadBalancerDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * 
 * Works in conjunction with the configuration to create and manage the HTTPS
 * Load Balancer.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public class HttpsLoadBalancer implements LoadBalancer {
	private String serverUris;

	private String loadPort;

	private String loadHost;

	private String keystoreLoc;

	private String ksPasswd;

	private String ksManPasswd;

	private CamelContext camelContext;

	private RouteDefinition routeDef;
	
	private BundleContext bundleContext;
	
	private ConfigurationPoller poller;

	private static final Logger LOGGER = Logger
			.getLogger(HttpsLoadBalancer.class);

	/**
	 * @param camelContext
	 *            The Camel context needed to configure routes
	 */
	public HttpsLoadBalancer(final CamelContext camelContext) {
		this.camelContext = camelContext;
		LOGGER.trace("HttpsLoadBalancer constructor done");
	}

	/**
	 * This method will stop and remove any existing Camel routes in this
	 * context, and then configure a new Camel route using the properties set in
	 * the setter methods.
	 * 
	 * Invoked after all of the setter methods have been called (for initial
	 * route creation), and also called whenever an existing route is updated.
	 */
	public void init() {
		
		if (loadHost != null && !loadHost.trim().equals("")){
			LOGGER.trace("INSIDE: init()");
			
			if (poller == null){
				//Start poller
				poller = new ConfigurationPoller(this);
			}
			
			
			fetchSslConfigurations();
	
			if (routeDef != null) {
				try {
					// This stops the route before trying to remove it
					LOGGER.debug("Removing route: " + routeDef.getDescriptionText());
					camelContext.suspendRoute(routeDef.getId());
					camelContext.stopRoute(routeDef.getId());
					camelContext.removeRoute(routeDef.getId());
				} catch (Exception e) {
					LOGGER.warn(e.getMessage());
				}
			} else {
				LOGGER.debug("No routes to remove before configuring a new route");
			}
			configureCamelRoute();
		}
	}
	
	

	public String getServerUris() {
		return serverUris;
	}
	
	

	public String getLoadPort() {
		return loadPort;
	}

	public String getLoadHost() {
		return loadHost;
	}

	public String getKeystoreLoc() {
		return keystoreLoc;
	}

	public String getKsPasswd() {
		return ksPasswd;
	}

	public String getKsManPasswd() {
		return ksManPasswd;
	}

	public RouteDefinition getRouteDef() {
		return routeDef;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	/**
	 * @param serverUris
	 *            Comma delimited String list of server IP address and ports
	 *            from configuration
	 */
	public void setServerUris(String serverUris) {
		LOGGER.info("ENTERING: setServerUris - " + serverUris);
		this.serverUris = serverUris;
	}

	public void setLoadPort(String loadPort) {
		LOGGER.info("ENTERING: setLoadPort - " + loadPort);
		this.loadPort = loadPort;
	}

	public void setLoadHost(String loadHost) {
		LOGGER.info("ENTERING: setLoadHost - " + loadHost);
		this.loadHost = loadHost;
	}

	public void setKeystoreLoc(String keystoreLoc) {
		LOGGER.info("ENTERING: keystoreLoc - " + keystoreLoc);
		this.keystoreLoc = keystoreLoc;
	}

	public void setKsPasswd(String ksPasswd) {
		LOGGER.info("ENTERING: ksPasswd - " + ksPasswd);
		this.ksPasswd = ksPasswd;
	}

	public void setKsManPasswd(String ksManPasswd) {
		LOGGER.info("ENTERING: ksManPasswd - " + ksManPasswd);
		this.ksManPasswd = ksManPasswd;
	}
	
	 public void setBundleContext( BundleContext bundleContext )
    {
		 this.bundleContext = bundleContext;
    }
	 
	 
	 /**
	 * Fetches all of the necessary SSL configurations from org.ops4j.pax.web
	 */
	private void fetchSslConfigurations(){
		 try
	        {
	            ServiceReference configAdminServiceRef = bundleContext.getServiceReference( ConfigurationAdmin.class.getName() );
	            if ( configAdminServiceRef != null )
	            {
	                ConfigurationAdmin ca = (ConfigurationAdmin) bundleContext.getService( configAdminServiceRef );
	                LOGGER.debug( "configuration admin obtained: " + ca );
	                if ( ca != null )
	                {
	                    Configuration web = ca.getConfiguration( "org.ops4j.pax.web" );
	                    ksPasswd = ((String) web.getProperties().get("org.ops4j.pax.web.ssl.keypassword"));
	                    keystoreLoc = ((String) web.getProperties().get("org.ops4j.pax.web.ssl.keystore"));
	                    ksManPasswd = ((String) web.getProperties().get("org.ops4j.pax.web.ssl.password"));
	         
	                }
	            }
	        }
	        catch ( IOException ioe )
	        {
	            LOGGER.warn( "Unable to obtain the configuration admin" );
	        }
	 }
	
	/**
	 * Destroy the camel route
	 */
	/**
	 * 
	 */
	public void destroy() {
		if (routeDef != null){
			LOGGER.trace("INSIDE: destroy()");
			LOGGER.debug("Removing route: " + routeDef.getDescriptionText());
			try {
				camelContext.suspendRoute(routeDef.getId());
				camelContext.stopRoute(routeDef.getId());
				camelContext.removeRoute(routeDef.getId());
			} catch (Exception e) {
				LOGGER.warn(e.getMessage());
			}
			poller.destroy();
		}

	}

	/**
	 * Invoked when updates are made to the configuration of existing load
	 * balancer. This method is invoked by the container as specified by the
	 * update-strategy and update-method attributes in Blueprint XML file.
	 * 
	 * @param properties
	 *            Properties returned from the configuration
	 */
	public void updateCallback(Map<String, Object> properties) {
		LOGGER.trace("ENTERING: updateCallback");

		if (properties != null) {
			setServerUris((String) properties.get("serverUris"));
			setLoadPort((String) properties.get("loadPort"));
			setLoadHost((String) properties.get("loadHost"));
			setKeystoreLoc((String) properties.get("keystoreLoc"));
			setKsPasswd((String) properties.get("ksPasswd"));
			setKsManPasswd((String) properties.get("ksManPasswd"));

			init();
		}

		LOGGER.trace("EXITING: updateCallback");
	}
	

	/**
     * 
     */
	private void configureCamelRoute() {
		LOGGER.trace("ENTERING: configureCamelRoute");
		System.out.println("STUFF: " + ksPasswd + ksManPasswd + keystoreLoc);

		// Must have a list of server URIs to be able to configure the Camel
		// route.
		if (serverUris == null || serverUris.trim().equals("")) {
			LOGGER.debug("Cannot setup camel route - must specify at least one server URI to be balanced");
			return;
		}

		if (loadPort == null || loadPort.trim().equals("")) {
			LOGGER.debug("Cannot setup camel route - must specify a port for the load balancer");
			return;
		}

		String[] serverList = serverUris.split(",");
		LoadBalancerDynamicRouteBuilder builder = new LoadBalancerDynamicRouteBuilder(
				camelContext, serverList, loadPort, loadHost, ksPasswd,
				ksManPasswd, keystoreLoc);

		try {
			// Add the routes that will be built by the RouteBuilder class above
			// to this CamelContext.
			// The addRoutes() method will instantiate the RouteBuilder class
			// above.
			
			camelContext.addRoutes(builder);
			camelContext.start();

			// Save the routes created by RouteBuilder so that they can be
			// stopped and removed later if the route(s) are modified by the
			// administrator.
			List<RouteDefinition> routeDefList = builder.getRouteCollection()
					.getRoutes();
			if (routeDefList.size() > 0) {
				this.routeDef = routeDefList.get(0);
			} else {
				LOGGER.warn("Route Definition list is larger than expected");
			}
		} catch (Exception e) {
			LOGGER.warn("Unable to configure Camel route", e);
		}

		LOGGER.trace("EXITING: configureCamelRoute");
	}

	public List<RouteDefinition> getRouteDefinitions() {
		return camelContext.getRouteDefinitions();
	}
}
