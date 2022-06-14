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

import org.apache.solr.client.solrj.SolrClient;

/** Interface implemented by factory classes used to create new {@link SolrClient} instances. */
public interface SolrClientFactory {
  /**
   * Requests the creation of a new {@code SolrClient} for a specific Solr core name.
   *
   * @param core the name of the Solr core to create to create a client for
   * @return the newly created {@code SolrClient}
   * @throws IllegalArgumentException if <code>core</code> is <code>null</code>
   */
  SolrClient newClient(String core);
}
