/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codice.proxy.http;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Route;
import org.apache.camel.component.servlet.DefaultHttpRegistry;
import org.apache.camel.component.servlet.HttpRegistry;
import org.apache.camel.component.servlet.ServletEndpoint;
import org.apache.camel.converter.ObjectConverter;
import org.apache.camel.http.common.CamelServlet;
import org.apache.camel.http.common.HttpConsumer;
import org.apache.camel.http.common.HttpHelper;
import org.apache.camel.http.common.HttpMessage;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Camel HTTP servlet which can be used in Camel routes to route servlet invocations in routes. */
public class HttpProxyCamelHttpTransportServlet extends CamelServlet implements Externalizable {
  private static final long serialVersionUID = -1797014782158930490L;

  private static final Logger LOGGER =
      LoggerFactory.getLogger(HttpProxyCamelHttpTransportServlet.class);

  private final Map<String, HttpConsumer> consumers = new ConcurrentHashMap<String, HttpConsumer>();

  @SuppressWarnings("squid:S2226" /* Lifecycle managed by blueprint */)
  private transient CamelContext camelContext;

  private transient HttpRegistry httpRegistry;

  private boolean ignoreDuplicateServletName;

  public HttpProxyCamelHttpTransportServlet(CamelContext camelContext) {
    this.camelContext = camelContext;
  }

  public HttpProxyCamelHttpTransportServlet() {}

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    String ignore = config.getInitParameter("ignoreDuplicateServletName");
    Boolean bool = ObjectConverter.toBoolean(ignore);
    if (bool != null) {
      ignoreDuplicateServletName = bool;
    } else {
      // always log so people can see it easier
      String msg =
          "Invalid parameter value for init-parameter ignoreDuplicateServletName with value: "
              + LogSanitizer.sanitize(ignore);
      LOGGER.debug(msg);
      throw new ServletException(msg);
    }

    String name = config.getServletName();
    String contextPath = config.getServletContext().getContextPath();

    if (httpRegistry == null) {
      httpRegistry = DefaultHttpRegistry.getHttpRegistry(name);
      CamelServlet existing = httpRegistry.getCamelServlet(name);
      if (existing != null) {
        String msg =
            "Duplicate ServetName detected: "
                + name
                + ". Existing: "
                + existing
                + " This: "
                + this
                + ". Its advised to use unique ServletName per Camel application.";
        // always log so people can see it easier
        LOGGER.debug(msg);
        if (!isIgnoreDuplicateServletName()) {
          throw new ServletException(msg);
        }
      }
      httpRegistry.register(this);
    }

    LOGGER.debug(
        "Initialized CamelHttpTransportServlet[name={}, contextPath={}]",
        getServletName(),
        contextPath);
  }

  @Override
  public void destroy() {
    DefaultHttpRegistry.removeHttpRegistry(getServletName());
    if (httpRegistry != null) {
      httpRegistry.unregister(this);
      httpRegistry = null;
    }
    LOGGER.debug("Destroyed CamelHttpTransportServlet[{}]", getServletName());
  }

  @Override
  protected void doService(HttpServletRequest oldRequest, HttpServletResponse response)
      throws ServletException, IOException {

    // Wrap request and clean the query String
    HttpProxyWrappedCleanRequest request = new HttpProxyWrappedCleanRequest(oldRequest);

    log.trace("Service: {}", LogSanitizer.sanitize(request));

    // Is there a consumer registered for the request.
    HttpConsumer consumer = resolve(request);

    if (consumer == null) {
      String path = request.getPathInfo();
      log.trace("Service Request Path = {}", LogSanitizer.sanitize(path));
      String endpointName = getEndpointNameFromPath(path);
      log.trace("Endpoint Name = {}", LogSanitizer.sanitize(endpointName));

      Route route = camelContext.getRoute(endpointName);
      try {
        if (route != null) {
          connect((HttpConsumer) route.getConsumer());
        }
      } catch (Exception e) {
        log.debug("Exception while creating consumer", e);
      }
      consumer = resolve(request);
    }

    try {
      if (consumer == null) {
        log.debug("No consumer to service request {}", LogSanitizer.sanitize(request));
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      // are we suspended?
      if (consumer.getEndpoint().isSuspended()) {
        log.debug("Consumer suspended, cannot service request {}", LogSanitizer.sanitize(request));
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        return;
      }

      if (consumer.getEndpoint().getHttpMethodRestrict() != null
          && !consumer.getEndpoint().getHttpMethodRestrict().equals(request.getMethod())) {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return;
      }

      if ("TRACE".equals(request.getMethod()) && !consumer.getEndpoint().isTraceEnabled()) {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return;
      }
    } catch (IOException e) {
      LOGGER.warn("Could not send error due to: ", e.getMessage());
      LOGGER.debug("Could not send error due to: ", e);
    }

    if (consumer == null) {
      return;
    }

    // create exchange and set data on it
    Exchange exchange = new DefaultExchange(consumer.getEndpoint(), ExchangePattern.InOut);

    if (consumer.getEndpoint().isBridgeEndpoint()) {
      exchange.setProperty(Exchange.SKIP_GZIP_ENCODING, Boolean.TRUE);
    }
    if (consumer.getEndpoint().isDisableStreamCache()) {
      exchange.setProperty(Exchange.DISABLE_HTTP_STREAM_CACHE, Boolean.TRUE);
    }

    // we override the classloader before building the HttpMessage just in case the binding
    // does some class resolution
    ClassLoader oldTccl = overrideTccl(exchange);
    HttpHelper.setCharsetFromContentType(request.getContentType(), exchange);
    exchange.setIn(new HttpMessage(exchange, consumer.getEndpoint(), request, response));
    // set context path as header
    String contextPath = consumer.getEndpoint().getPath();
    exchange.getIn().setHeader("CamelServletContextPath", contextPath);

    String httpPath = (String) exchange.getIn().getHeader(Exchange.HTTP_PATH);
    // here we just remove the CamelServletContextPath part from the HTTP_PATH
    if (contextPath != null && httpPath.startsWith(contextPath)) {
      exchange.getIn().setHeader(Exchange.HTTP_PATH, httpPath.substring(contextPath.length()));
    }

    // we want to handle the UoW
    try {
      consumer.createUoW(exchange);
    } catch (Exception e) {
      log.debug("Error processing request", e);
      throw new ServletException(e);
    }

    try {
      if (log.isTraceEnabled()) {
        log.trace("Processing request for exchangeId: {}", exchange.getExchangeId());
      }
      // process the exchange
      consumer.getProcessor().process(exchange);
    } catch (Exception e) {
      exchange.setException(e);
    }

    try {
      // now lets output to the response
      if (log.isTraceEnabled()) {
        log.trace("Writing response for exchangeId: {}", exchange.getExchangeId());
      }
      Integer bs = consumer.getEndpoint().getResponseBufferSize();
      if (bs != null) {
        log.trace("Using response buffer size: {}", bs);
        response.setBufferSize(bs);
      }
      consumer.getBinding().writeResponse(exchange, response);
    } catch (IOException e) {
      log.debug("Error processing request", e);
      throw e;
    } catch (Exception e) {
      log.debug("Error processing request", e);
      throw new ServletException(e);
    } finally {
      consumer.doneUoW(exchange);
      restoreTccl(exchange, oldTccl);
    }
  }

  private ServletEndpoint getServletEndpoint(HttpConsumer consumer) {
    if (!(consumer.getEndpoint() instanceof ServletEndpoint)) {
      throw new IllegalArgumentException(
          "Invalid consumer type. Must be ServletEndpoint but is " + consumer.getClass().getName());
    }
    return (ServletEndpoint) consumer.getEndpoint();
  }

  @Override
  protected HttpConsumer resolve(HttpServletRequest request) {
    String path = request.getPathInfo();
    log.trace("Request path is: {}", LogSanitizer.sanitize(path));
    String endpointName = getEndpointNameFromPath(path);
    log.trace("Looking up consumer for endpoint: {}", LogSanitizer.sanitize(endpointName));
    HttpConsumer answer = consumers.get(endpointName);

    if (answer == null) {
      if (LOGGER.isDebugEnabled()) {
        log.debug("Consumer Keys: {}", consumers.keySet());
      }
      for (Entry<String, HttpConsumer> entry : consumers.entrySet()) {
        if (entry.getValue().getEndpoint().isMatchOnUriPrefix()
            && path.startsWith(entry.getKey())) {
          answer = consumers.get(entry.getKey());
          break;
        }
      }
    }
    return answer;
  }

  @Override
  public void connect(HttpConsumer consumer) {
    log.debug("Getting ServletEndpoint for consumer: {}", consumer);
    ServletEndpoint endpoint = getServletEndpoint(consumer);
    if (endpoint.getServletName() != null) {
      String endpointName = getEndpointNameFromPath(consumer.getPath());
      log.debug("Adding consumer for endpointName: {}", endpointName);
      consumers.put(endpointName, consumer);
    }
  }

  @Override
  public void disconnect(HttpConsumer consumer) {
    String path = consumer.getPath();
    String endpointName = getEndpointNameFromPath(path);
    log.debug("Disconnecting consumer: {}", endpointName);
    consumers.remove(endpointName);
  }

  public boolean isIgnoreDuplicateServletName() {
    return ignoreDuplicateServletName;
  }

  @Override
  public String toString() {
    String name = getServletName();
    if (name != null) {
      return "CamelHttpTransportServlet[name=" + getServletName() + "]";
    } else {
      return "CamelHttpTransportServlet";
    }
  }

  private String getEndpointNameFromPath(String path) {
    // path is like: "/example1/something/0/thing.html"
    // or is like: "/example1"
    // endpointName is: "example1"
    String endpointName =
        StringUtils.indexOf(path, "/", 1) != StringUtils.INDEX_NOT_FOUND
            ? StringUtils.substring(path, 0, StringUtils.indexOf(path, "/", 1))
            : path;
    endpointName = StringUtils.remove(endpointName, "/");
    return endpointName;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {}

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}
}
