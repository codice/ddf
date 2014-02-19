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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;

import org.apache.camel.component.http.CamelServlet;
import org.apache.camel.component.http.HttpConsumer;
import org.apache.camel.component.servlet.HttpRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class  CamelServletHTTPRegistry implements HttpRegistry {
	  private static final transient Logger LOG = LoggerFactory.getLogger(CamelServletHTTPRegistry.class);
	 
	  private static HttpRegistry singleton;
	 
	  private final Set<HttpConsumer> consumers;
	  private final Set<CamelServlet> providers;
	 
	  public  CamelServletHTTPRegistry() {
	    consumers = new HashSet();
	    providers = new HashSet();
	  }
	 
	  /**
	   * Lookup or create a HttpRegistry
	   */
	  public static synchronized HttpRegistry getSingletonHttpRegistry() {
	    if (singleton == null) {
	      singleton = new  CamelServletHTTPRegistry();
	    }
	    return singleton;
	  }
	 
	  @Override
	  public void register(HttpConsumer consumer) {
	    LOG.debug("Registering consumer for path {} providers present: {}",
	     consumer.getPath(), providers.size());
	    consumers.add(consumer);
	    for (CamelServlet provider : providers) {
	      provider.connect(consumer);
	    }
	  }
	 
	  @Override
	  public void unregister(HttpConsumer consumer) {
	    LOG.debug("Unregistering consumer for path {} ", consumer.getPath());
	    consumers.remove(consumer);
	    for (CamelServlet provider : providers) {
	      provider.disconnect(consumer);
	    }
	  }
	 
	  @SuppressWarnings("rawtypes")
	  public void register(CamelServletProvider provider, Map properties) {
	    LOG.info("Registering provider through OSGi service listener {}", properties);
	    try {
	      CamelServlet camelServlet = provider.getCamelServlet();
	      camelServlet.setServletName((String) properties.get("servlet-name"));
	      register(camelServlet);
	    } catch (ClassCastException cce) {
	      LOG.info("Provider is not a Camel Servlet");
	    }
	  }
	 
	  public void unregister(CamelServletProvider provider, Map<String, Object> properties) {
	    LOG.info("Deregistering provider through OSGi service listener {}", properties);
	    try {
	      CamelServlet camelServlet = provider.getCamelServlet();
	      unregister((CamelServlet)provider);
	    } catch (ClassCastException cce) {
	      LOG.info("Provider is not a Camel Servlet");
	    }
	  }
	 
	  @Override
	  public void register(CamelServlet provider) {
	    LOG.debug("Registering CamelServlet with name {} consumers present: {}",
	     provider.getServletName(), consumers.size());
	    providers.add(provider);
	    for (HttpConsumer consumer : consumers) {
	      provider.connect(consumer);
	    }
	  }
	 
	  @Override
	  public void unregister(CamelServlet provider) {
	    providers.remove(provider);
	  }
	 
	  public void setServlets(List<Servlet> servlets) {
	    providers.clear();
	    for (Servlet servlet : servlets) {
	      if (servlet instanceof CamelServlet) {
	        providers.add((CamelServlet) servlet);
	      }
	    }
	  }

	@Override
	public CamelServlet getCamelServlet(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	}