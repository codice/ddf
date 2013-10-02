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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jetty.JettyHttpComponent;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;

/*********************************************
 * Dynamically builds the Camel route for the HTTP and HTTPs Load Balancer.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public class LoadBalancerDynamicRouteBuilder extends RouteBuilder {
	private String[] serverUris;
	private String port;
	private boolean enableSsl;
	private String host;
	private String ksPasswd;
	private String ksManPasswd;
	private String keystoreLoc;
	private static final int MAX_FAIL_ATTEMPTS = -1;
	private CamelContext context;

	public LoadBalancerDynamicRouteBuilder(CamelContext context,
			String[] serverUris, String port, String host, String ksPasswd,
			String ksManPasswd, String keystoreLoc) {
		// Configuring for SSL connection
		super(context);
		this.host = host;
		this.ksPasswd = ksPasswd;
		this.ksManPasswd = ksManPasswd;
		this.keystoreLoc = keystoreLoc;
		this.serverUris = serverUris;
		this.port = port;
		this.context = context;
		enableSsl = true;
	}

	public LoadBalancerDynamicRouteBuilder(CamelContext context,
			String[] serverUris, String port) {
		super(context);
		this.serverUris = serverUris;
		this.port = port;
		this.context = context;
	}

	@Override
	public void configure() throws Exception {
		String protocol = "http";

		if (enableSsl) {
			// Configure Camel route by creating a new jetty component with SSL
			// params
			protocol = "https";

			Map<Integer, SslSelectChannelConnector> connectors = new HashMap<Integer, SslSelectChannelConnector>();
			connectors.put(Integer.valueOf(port),
					createSslSocketConnector(getContext()));

			// create jetty component
			JettyHttpComponent jetty = new JettyHttpComponent();
			// add connectors
			jetty.setSslSocketConnectors(connectors);
			// add jetty to camel context
			context.removeComponent("jetty");
			context.addComponent("jetty", jetty);
		} else {
			/******************************************************************************************
			 * 0.0.0.0 is being used to match all IP addresses on a given host
			 * machine for HTTP connection
			 *******************************************************************************************/
			host = "0.0.0.0";
		}

		/**
		 * Create jetty server Uri; matchOnUriPrefix=true tells the jetty
		 * component to match on a given url with any prefix
		 */
		String jettyFromUri = "jetty:" + protocol + "://" + host + ":" + port
				+ "?matchOnUriPrefix=true&continuationTimeout=0";

		// Create fully qualified URIs to connect to nodes
		/*************************************************************************************************************
		 * bridgeEndpoint=true allows for seamless transactions between the load
		 * balancer and a given node throuwExceptionOnFailure=false allows the
		 * opportunity for the transaction to goto the next available server
		 *************************************************************************************************************/
		String[] uriList = new String[serverUris.length];
		for (int i = 0; i < serverUris.length; ++i) {
			String uri = "jetty:" + protocol + "://" + serverUris[i]
					+ "?bridgeEndpoint=true&throwExceptionOnFailure=false&enableMultipartFilter=false";
			uriList[i] = uri;
		}

		// Create Camel Route
		from(jettyFromUri).loadBalance()
		    .failover(uriList.length, false, true, java.net.ConnectException.class).to(uriList);

	}

	/**
	 * Creates an SSL select channel connector to configure the jetty component
	 * 
	 * @param context
	 *            The CamelContext
	 * @return A SslSelectChannelConnector
	 * @throws Exception
	 */
	private SslSelectChannelConnector createSslSocketConnector(
			CamelContext context) throws Exception {
		KeyStoreParameters ksp = new KeyStoreParameters();
		ksp.setResource(keystoreLoc);
		ksp.setPassword(ksPasswd);

		KeyManagersParameters kmp = new KeyManagersParameters();
		kmp.setKeyPassword(ksManPasswd);
		kmp.setKeyStore(ksp);

		SSLContextParameters sslContextParameters = new SSLContextParameters();
		sslContextParameters.setKeyManagers(kmp);

		// From Camel 2.5.0 Camel-Jetty is using SslSelectChannelConnector
		// instead of SslSocketConnector
		SslSelectChannelConnector sslSocketConnector = new SslSelectChannelConnector();
		sslSocketConnector.getSslContextFactory().setSslContext(
				sslContextParameters.createSSLContext());

		return sslSocketConnector;
	}
}
