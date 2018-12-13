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

import com.google.common.annotations.VisibleForTesting;
import ddf.platform.solr.security.SolrPasswordUpdate;
import ddf.security.encryption.EncryptionService;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.BiFunction;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;

/**
 * Factory class used to create the proper {@link SolrClient} based on the current {@code
 * solr.client} system property.
 */
public final class SolrClientFactoryImpl implements SolrClientFactory {

  private final BiFunction<SolrClientFactory, String, SolrClient> newClientFunction;
  private final EncryptionService encryptionService;
  private final SolrPasswordUpdate solrPasswordUpdate;

  @SuppressWarnings("unused" /* used by blueprint */)
  public SolrClientFactoryImpl(
      EncryptionService encryptionService, SolrPasswordUpdate solrPasswordUpdate) {
    this.newClientFunction = (factory, core) -> factory.newClient(core);
    this.encryptionService = encryptionService;
    this.solrPasswordUpdate = solrPasswordUpdate;
  }

  @VisibleForTesting
  SolrClientFactoryImpl(BiFunction<SolrClientFactory, String, SolrClient> newClientFunction) {
    this.newClientFunction = newClientFunction;
    this.encryptionService = null;
    this.solrPasswordUpdate = null;
  }

  @Override
  public SolrClient newClient(String core) {
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
      factory = new HttpSolrClientFactory(encryptionService, solrPasswordUpdate);
    }

    return newClientFunction.apply(factory, core);
  }
}
