/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.proxy.http;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.apache.camel.Component;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.ServletComponent;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Http Proxy service which creates a Camel based http proxy
 * @author ddf
 *
 */
public class HttpProxyServiceImpl extends OsgiDefaultCamelContext implements HttpProxyService {
	BundleContext bundleContext = null;
	RouteBuilder routeBuilder = null;
	String targetUri = null;
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpProxyServiceImpl.class);
	public static final String SERVLET_NAME = "CamelServlet";
	private static final String SERVLET_COMPONENT = "servlet";
	public static final String SERVLET_PATH = "/proxy";
	public static final String GENERIC_ENDPOINT_NAME = "endpoint";
	int incrementer = 0;

    public HttpProxyServiceImpl(final BundleContext bundleContext) throws Exception {
        super(bundleContext);
        this.bundleContext = bundleContext;
        
        //Add servlet to the Camel Context
	    ServletComponent servlet = new ServletComponent();
	    servlet.setCamelContext(this);
	    servlet.setServletName(SERVLET_NAME);
	    this.addComponent(SERVLET_COMPONENT,servlet);
    }
    
    public synchronized String startProxy(String targetUri) throws Exception{
    	String endpointName = GENERIC_ENDPOINT_NAME + incrementer;
    	startProxy(endpointName, targetUri);
    	incrementer++;
    	
    	return endpointName;
    }
    
    public String startProxy(final String endpointName, final String targetUri) throws Exception{

    	this.targetUri = targetUri;
	    routeBuilder = new RouteBuilder() {
	        @Override
	        public void configure() throws Exception {
	        	from("servlet:///" + endpointName)
	        	.to(targetUri + "?bridgeEndpoint=true&amp;throwExceptionOnFailure=false")
	        	.routeId(endpointName);           }
	    };
	    this.addRoutes(routeBuilder);
	    this.start();
	    LOGGER.debug("Started proxy route at servlet endpoint: " + endpointName + ", routing to: " + targetUri);
	    return endpointName;
    }
    
    public void stopProxy(String endpointName) throws Exception{
    	LOGGER.debug("Stopping proxy route at endpoint: " + endpointName);
    	this.removeRoute(endpointName);
    }
    
    public void destroy(){
    	try {
			this.stop();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
    	this.removeComponent(SERVLET_COMPONENT);
    }
    
}