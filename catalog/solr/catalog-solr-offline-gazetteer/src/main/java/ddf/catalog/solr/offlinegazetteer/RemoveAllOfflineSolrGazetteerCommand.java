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
package ddf.catalog.solr.offlinegazetteer;

import java.io.IOException;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

@Service
@Command(
    scope = "offline-solr-gazetteer",
    name = "removeall",
    description = "Sends a request to delete all items in the solr gazetteer collection")
public class RemoveAllOfflineSolrGazetteerCommand extends AbstractSolrClientCommand {

  @Override
  void executeWithSolrClient(SolrClient solrClient) throws SolrServerException, IOException {
    solrClient.deleteByQuery("*:*");
  }
}
