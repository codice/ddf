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
package org.codice.solr.factory.impl;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrHttpRequestRetryHandler implements HttpRequestRetryHandler {

  private static final Integer MAX_RETRY_COUNT = 11;

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrHttpRequestRetryHandler.class);

  @Override
  public boolean retryRequest(IOException e, int retryCount, HttpContext httpContext) {
    if (e instanceof InterruptedIOException) {
      LOGGER.debug("Connection timeout.");
    }
    if (e instanceof UnknownHostException) {
      LOGGER.warn("Solr Client: Unknown host.");
    }
    if (e instanceof SSLException) {
      LOGGER.warn("Solr Client: SSL handshake exception.");
    }
    LOGGER.debug("Connection failed", e);
    try {
      long waitTime = (long) Math.pow(2, Math.min(retryCount, MAX_RETRY_COUNT)) * 50;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Solr Client: Connection failed, waiting {} before retrying.",
            DurationFormatUtils.formatDurationWords(waitTime, true, true));
      }
      synchronized (this) {
        wait(waitTime);
      }
    } catch (InterruptedException ie) {
      LOGGER.debug("Exception while waiting.", ie);
    }
    return true;
  }
}
