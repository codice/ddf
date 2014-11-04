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

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codice.proxy.http;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Route;
import org.apache.camel.component.http.CamelServlet;
import org.apache.camel.component.http.HttpConsumer;
import org.apache.camel.component.http.HttpMessage;
import org.apache.camel.component.http.helper.HttpHelper;
import org.apache.camel.component.servlet.DefaultHttpRegistry;
import org.apache.camel.component.servlet.HttpRegistry;
import org.apache.camel.component.servlet.ServletEndpoint;
import org.apache.camel.converter.ObjectConverter;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Camel HTTP servlet which can be used in Camel routes to route servlet invocations in routes.
 */
public class HttpProxyCamelHttpTransportServlet extends CamelServlet {
    private static final long serialVersionUID = -1797014782158930490L;
    private static final Logger LOG = LoggerFactory.getLogger(HttpProxyCamelHttpTransportServlet.class);

    private CamelContext camelContext;
    private HttpRegistry httpRegistry;
    private boolean ignoreDuplicateServletName;

    private ConcurrentMap<String, HttpConsumer> consumers = new ConcurrentHashMap<String, HttpConsumer>();

    public HttpProxyCamelHttpTransportServlet(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String ignore = config.getInitParameter("ignoreDuplicateServletName");
        Boolean bool = ObjectConverter.toBoolean(ignore);
        if (bool != null) {
            ignoreDuplicateServletName = bool;
        } else {
            // always log so people can see it easier
            String msg = "Invalid parameter value for init-parameter ignoreDuplicateServletName with value: " + ignore;
            LOG.error(msg);
            throw new ServletException(msg);
        }

        String name = config.getServletName();
        String contextPath = config.getServletContext().getContextPath();

        if (httpRegistry == null) {
            httpRegistry = DefaultHttpRegistry.getHttpRegistry(name);
            CamelServlet existing = httpRegistry.getCamelServlet(name);
            if (existing != null) {
                String msg = "Duplicate ServetName detected: " + name + ". Existing: " + existing + " This: " + this.toString()
                        + ". Its advised to use unique ServletName per Camel application.";
                // always log so people can see it easier
                if (isIgnoreDuplicateServletName()) {
                    LOG.warn(msg);
                } else {
                    LOG.error(msg);
                    throw new ServletException(msg);
                }
            }
            httpRegistry.register(this);
        }

        LOG.info("Initialized CamelHttpTransportServlet[name={}, contextPath={}]", getServletName(), contextPath);
    }
    
    @Override
    public void destroy() {
        DefaultHttpRegistry.removeHttpRegistry(getServletName());
        if (httpRegistry != null) {
            httpRegistry.unregister(this);
            httpRegistry = null;
        }
        LOG.info("Destroyed CamelHttpTransportServlet[{}]", getServletName());
    }
    
    @Override
    protected void service(HttpServletRequest oldRequest, HttpServletResponse response) throws ServletException, IOException {
    	
    	//Wrap request and clean the query String
    	HttpProxyWrappedCleanRequest request = new HttpProxyWrappedCleanRequest(oldRequest);
    	
    	log.trace("Service: {}", request);

        // Is there a consumer registered for the request.
        HttpConsumer consumer = resolve(request);

        if (consumer == null) {
            String path = request.getPathInfo();
            log.debug("Service Request Path = {}", path);
            String endpointName = getEndpointNameFromPath(path);
            log.debug("Endpoint Name = {}", endpointName);

            Route route = camelContext.getRoute(endpointName);
            try {
                connect((HttpConsumer) route.getConsumer());
            } catch (Exception e) {
                log.debug("Exception while creating consumer", e);
            }
            consumer = resolve(request);
        }

        if (consumer == null) {
            log.debug("No consumer to service request {}", request);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // are we suspended?
        if (consumer.getEndpoint().isSuspended()) {
            log.debug("Consumer suspended, cannot service request {}", request);
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
        exchange.setIn(new HttpMessage(exchange, request, response));
        // set context path as header
        String contextPath = consumer.getEndpoint().getPath();
        exchange.getIn().setHeader("CamelServletContextPath", contextPath);

        String httpPath = (String)exchange.getIn().getHeader(Exchange.HTTP_PATH);
        // here we just remove the CamelServletContextPath part from the HTTP_PATH
        if (contextPath != null
            && httpPath.startsWith(contextPath)) {
            exchange.getIn().setHeader(Exchange.HTTP_PATH,
                    httpPath.substring(contextPath.length()));
        }

        // we want to handle the UoW
        try {
            consumer.createUoW(exchange);
        } catch (Exception e) {
            log.error("Error processing request", e);
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
            log.error("Error processing request", e);
            throw e;
        } catch (Exception e) {
            log.error("Error processing request", e);
            throw new ServletException(e);
        } finally {
            consumer.doneUoW(exchange);
            restoreTccl(exchange, oldTccl);
        }
    }

    
    private ServletEndpoint getServletEndpoint(HttpConsumer consumer) {
        if (!(consumer.getEndpoint() instanceof ServletEndpoint)) {
            throw new RuntimeException("Invalid consumer type. Must be ServletEndpoint but is " 
                    + consumer.getClass().getName());
        }
        return (ServletEndpoint)consumer.getEndpoint();
    }

    protected HttpConsumer resolve(HttpServletRequest request) {
        String path = request.getPathInfo();
        log.debug("Request path is: {}", path);
        String endpointName = getEndpointNameFromPath(path);
        log.debug("Looking up consumer for endpoint: {}", endpointName);
        HttpConsumer answer = consumers.get(endpointName);

        if (answer == null) {
            log.debug("Consumer Keys: {}", Arrays.toString(consumers.keySet().toArray()));
            for (String key : consumers.keySet()) {
                if (consumers.get(key).getEndpoint().isMatchOnUriPrefix() && path.startsWith(key)) {
                    answer = consumers.get(key);
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
        String endpointName = (StringUtils.indexOf(path, "/", 1) != StringUtils.INDEX_NOT_FOUND) ?
                StringUtils.substring(path, 0, StringUtils.indexOf(path, "/", 1)) :
                path;
        endpointName = StringUtils.remove(endpointName, "/");
        return endpointName;
    }
}

