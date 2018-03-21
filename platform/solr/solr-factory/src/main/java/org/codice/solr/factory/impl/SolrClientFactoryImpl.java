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

import static org.apache.commons.lang.Validate.notNull;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import org.apache.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;

/**
 * Factory class used to create the proper {@link SolrClient} based on the current {@code
 * solr.client} system property.
 */
public class SolrClientFactoryImpl implements SolrClientFactory {

  private BiFunction<SolrClientFactory, String, Future<SolrClient>> newClientFunction;

  public SolrClientFactoryImpl() {
    newClientFunction = (factory, core) -> factory.newClient(core);
  }

  // Package-private for unit testing.
  SolrClientFactoryImpl(
      BiFunction<SolrClientFactory, String, Future<SolrClient>> newClientFunction) {
    this.newClientFunction = newClientFunction;
  }

  @Override
  public Future<SolrClient> newClient(String core) {
    notNull(core, "Solr core name cannot be null");

    String clientType =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.client", "HttpSolrClient"));
    SolrClientFactory factory;

    if ("EmbeddedSolrServer".equals(clientType)) {
      factory = new EmbeddedSolrFactory();
    } else if ("CloudSolrClient".equals(clientType)) {
      factory = new SolrCloudClientFactory();
    } else { // Use HttpSolrClient by default
      factory = new HttpSolrClientFactory();
    }

    return newClientFunction.apply(factory, core);
  }
}
