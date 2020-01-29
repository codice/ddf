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
package org.codice.solr.factory;

import org.codice.solr.client.solrj.SolrClient;

/** Interface implemented by factory classes used to create new {@link SolrClient} instances. */
public interface SolrClientFactory {
  /**
   * Requests the creation of a new {@code SolrClient} for a specific Solr core name.
   *
   * <p><i>Note:</i> The client returned might not yet be available (see {@link
   * SolrClient#isAvailable}). Even after having reported to be available, a client might suddenly
   * become unavailable. All methods will throw {@link
   * org.codice.solr.factory.impl.UnavailableSolrClient} exceptions anytime the client is
   * unavailable and the client will attempt to reestablish the connection in the background.
   *
   * @param core the name of the Solr core to create to create a client for
   * @return the newly created {@code SolrClient}
   * @throws IllegalArgumentException if <code>core</code> is <code>null</code>
   */
  SolrClient newClient(String core);

  /** @return whether or not the provider can connect to the solrCloud instance or Standalone. */
  boolean isAvailable();

  /**
   * Returns whether or not the provider connects to SolrCloud instance or Standalone. Solr Cloud
   * provides different capabilities that upstream Index and Storage providers need knowledged of
   * (e.g. Collection Aliases in SolrCloud).
   *
   * @return Provider is a SolrCloud provider.
   */
  boolean isSolrCloud();
}
