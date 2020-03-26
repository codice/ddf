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
import org.apache.commons.lang.Validate;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;

/**
 * This proxy provides the ability to segregate ping requests from other requests by sending them to
 * a second Solr client which would typically be configured not to perform any internal retries.
 */
public class PingAwareSolrClientProxy extends SolrClientProxy {
  private final SolrClient client;

  private final SolrClient pingClient;

  /**
   * Creates a new proxy with the given client and ping client.
   *
   * @param client the client to use for everything except ping requests
   * @param pingClient the client t use for ping requests
   * @throws IllegalArgumentException if <code>client</code> or <code>pingClient</code> are <code>
   *     null</code>
   */
  public PingAwareSolrClientProxy(SolrClient client, SolrClient pingClient) {
    Validate.notNull(client, "invalid null client");
    Validate.notNull(pingClient, "invalid null ping client");
    this.client = client;
    this.pingClient = pingClient;
  }

  @Override
  protected SolrClient getProxiedClient() {
    return client;
  }

  @Override
  public SolrPingResponse ping() throws SolrServerException, IOException {
    return pingClient.ping();
  }

  @Override
  public SolrPingResponse ping(String collection) throws SolrServerException, IOException {
    return pingClient.ping(collection);
  }

  @Override
  @SuppressWarnings(
      "squid:S00108" /* Using empty block of try-with-resources to close multiple resources */)
  public void close() throws IOException {
    try (final SolrClient c = client;
        final SolrClient p = pingClient) {}
  }

  @Override
  public String toString() {
    return "PingAwareSolrClientProxy(" + client + ", " + pingClient + ")";
  }
}
