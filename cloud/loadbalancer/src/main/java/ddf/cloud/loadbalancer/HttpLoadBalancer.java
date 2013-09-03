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

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.log4j.Logger;

/**
 * 
 * Works in conjunction with the configuration to create and manage the HTTP
 * Load Balancer.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public class HttpLoadBalancer implements LoadBalancer {
	private String serverUris;

	private String loadPort;

	private CamelContext camelContext;

	private RouteDefinition routeDef;

	private static final Logger LOGGER = Logger
			.getLogger(HttpLoadBalancer.class);

	/**
	 * @param camelContext
	 *            The Camel context needed to configure routes
	 */
	public HttpLoadBalancer(final CamelContext camelContext) {
		this.camelContext = camelContext;
		LOGGER.trace("HttpLoadBalancer constructor done");
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
		LOGGER.trace("INSIDE: init()");

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

	public String getServerUris() {
		return serverUris;
	}

	/**
	 * @param serverUris
	 *            Comma delimited String list of server IP address and ports
	 *            from configuration
	 */
	public void setServerUris(String serverUris) {
		LOGGER.info("ENTERING: setServerUris");

		this.serverUris = serverUris;

	}

	public void setLoadPort(String loadPort) {
		LOGGER.info("ENTERING: setLoadPort");

		this.loadPort = loadPort;

	}

	/**
	 * Destroy the route
	 */
	public void destroy() {
		LOGGER.trace("INSIDE: destroy()");
		LOGGER.debug("Removing route: " + routeDef.getDescriptionText());
		try {
			camelContext.suspendRoute(routeDef.getId());
			camelContext.stopRoute(routeDef.getId());
			camelContext.removeRoute(routeDef.getId());
		} catch (Exception e) {
			LOGGER.warn(e.getMessage());
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
			init();
		}

		LOGGER.trace("EXITING: updateCallback");
	}

	/**
     * 
     */
	private void configureCamelRoute() {
		LOGGER.trace("ENTERING: configureCamelRoute");

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
				camelContext, serverList, loadPort);

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
				LOGGER.warn("Route Definition list is larger than expected: " + routeDefList.size());
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
