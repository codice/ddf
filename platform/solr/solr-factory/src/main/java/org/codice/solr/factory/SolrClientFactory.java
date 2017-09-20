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

import java.util.concurrent.Future;
import org.apache.solr.client.solrj.SolrClient;

/** Interface implemented by factory classes used to create new {@link SolrClient} instances. */
public interface SolrClientFactory {

  /**
   * Requests the creation of a new {@code SolrClient} for a specific Solr core name. <br>
   * Note that {@link Future#get()} will return {@code null} if a {@link SolrClient} could not be
   * created either immediately or after a retry period determined by the implementing class.
   * Clients of this class should consider implementing retry logic if needed based on that return
   * value.
   *
   * @param core name of the Solr core to create
   * @return {@code Future} used to retrieve the new {@code SolrClient} created
   */
  Future<SolrClient> newClient(String core);
}
