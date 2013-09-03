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

import static org.hamcrest.Matchers.equalTo;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.LoadBalanceDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;

import ddf.cloud.loadbalancer.HttpLoadBalancer;

/**
 * Tests the HTTP Load Balancer.
 * 
 * @author ddf.isgs@lmco.com
 */
public class HttpLoadBalancerTest extends CamelTestSupport {
	private static final transient Logger LOGGER = Logger
			.getLogger(HttpLoadBalancerTest.class);

	private CamelContext camelContext;
	private HttpLoadBalancer httpLoadBalancer;

	@After
	public void tearDown() {
		LOGGER.debug("INSIDE tearDown");
		context = null;
		camelContext = null;
	}

	@Test
	/**
	 * Tests route creation with a given load balancer port and server URI
	 * 
	 * @throws Exception
	 */
	public void testRouteCreationWithPortAndServerURI() throws Exception {
		String loadPort = "8080";
		String serverUris = "localhost:8181";

		RouteDefinition routeDefinition = createRoute(loadPort, serverUris);

		verifyRoute(routeDefinition, loadPort, serverUris);

		camelContext.removeRouteDefinition(routeDefinition);
	}

	@Test
	/**
	 * Tests route creation with a given load balancer port and a null server URI
	 * 
	 * @throws Exception
	 */
	public void testRouteCreationWithNullServerURI() throws Exception {
		String loadPort = "8080";
		String serverUris = null;
		 camelContext = super.createCamelContext();
		 
			HttpLoadBalancer httpLb = new HttpLoadBalancer(camelContext);

			httpLb.setLoadPort(loadPort);
			httpLb.setServerUris(serverUris);

			// Simulates what container would do once all setters have been invoked
			httpLb.init();
			
			assertThat(camelContext.getRouteDefinitions().size(), is(0));
	}
	
	@Test
	/**
	 * Tests route creation with a given load balancer port and an empty server URI
	 * 
	 * @throws Exception
	 */
	public void testRouteCreationWithEmptyServerURI() throws Exception {
		String loadPort = "8080";
		String serverUris = "";
		 camelContext = super.createCamelContext();
		 
			HttpLoadBalancer httpLb = new HttpLoadBalancer(camelContext);

			httpLb.setLoadPort(loadPort);
			httpLb.setServerUris(serverUris);

			// Simulates what container would do once all setters have been invoked
			httpLb.init();
			
			assertThat(camelContext.getRouteDefinitions().size(), is(0));
	}
	
	@Test
	/**
	 * Tests route creation with an empty load balancer port and a given server URI
	 * 
	 * @throws Exception
	 */
	public void testRouteCreationWithEmptyPort() throws Exception {
		String loadPort = "";
		String serverUris = "localhost:8181";
		 camelContext = super.createCamelContext();
		 
			HttpLoadBalancer httpLb = new HttpLoadBalancer(camelContext);

			httpLb.setLoadPort(loadPort);
			httpLb.setServerUris(serverUris);

			// Simulates what container would do once all setters have been invoked
			httpLb.init();
			
			assertThat(camelContext.getRouteDefinitions().size(), is(0));
	}
	
	@Test
	/**
	 * Tests route creation with a null load balancer port and a given server URI
	 * 
	 * @throws Exception
	 */
	public void testRouteCreationWithNullPort() throws Exception {
		String loadPort = null;
		String serverUris = "localhost:8181";
		 camelContext = super.createCamelContext();
		 
			HttpLoadBalancer httpLb = new HttpLoadBalancer(camelContext);

			httpLb.setLoadPort(loadPort);
			httpLb.setServerUris(serverUris);

			// Simulates what container would do once all setters have been invoked
			httpLb.init();
			
			assertThat(camelContext.getRouteDefinitions().size(), is(0));
	}
	
	@Test
	/**
	 * Tests route creation with multiple server URIs
	 * 
	 * @throws Exception
	 */
	public void testRouteCreationWithMultipleServerURIs() throws Exception {
		String loadPort = "8080";
		String serverUris = "localhost:8181,127.0.0.1:8181";
		 camelContext = super.createCamelContext();
		 
			HttpLoadBalancer httpLb = new HttpLoadBalancer(camelContext);

			httpLb.setLoadPort(loadPort);
			httpLb.setServerUris(serverUris);

			// Simulates what container would do once all setters have been invoked
			httpLb.init();
			
			assertThat(camelContext.getRouteDefinitions().size(), is(1));
	}
	
	private RouteDefinition createRoute(String loadPort, String serverUris)
			throws Exception {
		camelContext = super.createCamelContext();

		HttpLoadBalancer httpLb = new HttpLoadBalancer(camelContext);

		httpLb.setLoadPort(loadPort);
		httpLb.setServerUris(serverUris);

		// Simulates what container would do once all setters have been invoked
		httpLb.init();

		// Initial Camel route should now be created
		List<RouteDefinition> routeDefinitions = httpLb.getRouteDefinitions();

		assertThat(routeDefinitions.size(), is(1));
		LOGGER.debug("routeDefinition = " + routeDefinitions.get(0).toString());

		return routeDefinitions.get(0);
	}

	private void verifyRoute(RouteDefinition routeDefinition, String loadPort,
			String serverUris) {
		List<FromDefinition> fromDefinitions = routeDefinition.getInputs();
		assertThat(fromDefinitions.size(), is(1));
		String uri = fromDefinitions.get(0).getUri();
		LOGGER.debug("uri = " + uri);

		String expectedUri = "jetty:http://0.0.0.0:" + loadPort
				+ "?matchOnUriPrefix=true";

		assertThat(uri, equalTo(expectedUri));
		List<ProcessorDefinition<?>> processorDefinitions = routeDefinition
				.getOutputs();

		// expect 1 output: one To:
		assertThat(processorDefinitions.size(), is(1));

		ProcessorDefinition<?> pd = processorDefinitions.get(0);
		LOGGER.debug(pd);
		assertTrue(pd instanceof LoadBalanceDefinition);
		LoadBalanceDefinition shd = (LoadBalanceDefinition) pd;
		assertThat(shd.getId(), equalTo("loadbalance1"));
	}

}
