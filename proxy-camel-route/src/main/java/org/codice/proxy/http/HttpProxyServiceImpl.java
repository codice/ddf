package org.codice.proxy.http;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.ServletComponent;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.osgi.framework.BundleContext;

public class HttpProxyServiceImpl extends OsgiDefaultCamelContext implements HttpProxyService {
	BundleContext bundleContext = null;
	RouteBuilder routeBuilder = null;
	String endpointName = null;
	String targetUri = null;

    public HttpProxyServiceImpl(final BundleContext bundleContext) throws Exception {
        super(bundleContext);
        this.bundleContext = bundleContext;
    }
    
    public String startProxy(String targetUri) throws Exception{
    	//Generate a random endpoint name
    	SecureRandom random = new SecureRandom();
    	String endpointName = new BigInteger(130, random).toString(32).substring(0,10);
    	startProxy(endpointName, targetUri);
    	
    	return endpointName;
    }
    
    public String startProxy(final String endpointName, final String targetUri) throws Exception{
    	
    	if (routeBuilder != null){
    		stopProxy();
    	}
    	
    	this.endpointName = endpointName;
    	this.targetUri = targetUri;
	    routeBuilder = new RouteBuilder() {
	        @Override
	        public void configure() throws Exception {
	        	from("servlet:///" + endpointName)
	        	.to(targetUri + "?bridgeEndpoint=true&amp;throwExceptionOnFailure=false")
	        	.routeId(endpointName);           }
	    };
	    ServletComponent servlet = new ServletComponent();
	    servlet.setCamelContext(this);
	    servlet.setServletName("CamelServlet");
	    this.addComponent("servlet",servlet);
	    this.addRoutes(routeBuilder);
	    this.start();
	    
	    return endpointName;
    }
    
    public void stopProxy() throws Exception{
    	this.stop();
    	this.removeRoute(endpointName);
    	this.removeComponent("servlet");
    }
}