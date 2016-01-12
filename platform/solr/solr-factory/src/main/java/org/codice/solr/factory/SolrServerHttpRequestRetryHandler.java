/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.solr.factory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrServerHttpRequestRetryHandler implements HttpRequestRetryHandler {

    private static final Integer MAX_RETRY_COUNT = 11;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SolrServerHttpRequestRetryHandler.class);

    @Override
    public boolean retryRequest(IOException e, int retryCount, HttpContext httpContext) {
        if (e instanceof InterruptedIOException) {
            LOGGER.error("Connection timeout.");
        }
        if (e instanceof UnknownHostException) {
            LOGGER.error("Unknown host.");
        }
        if (e instanceof SSLException) {
            LOGGER.error("SSL handshake exception.");
        }
        HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
        HttpRequest request = clientContext.getRequest();
        if (!(request instanceof HttpEntityEnclosingRequest)) {
            LOGGER.error("Connection failed. Request is idempotent.");
        }
        try {
            long waitTime = (long) Math.pow(2, Math.min(retryCount, MAX_RETRY_COUNT)) * 50;
            LOGGER.debug("Connection failed, entering grace period for " + waitTime + "seconds.");
            wait(waitTime);
        } catch (InterruptedException ie) {
            LOGGER.error("Exception while waiting.", ie);
        }
        return true;
    }
}
