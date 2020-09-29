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

import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.GAZETTEER_REQUEST_HANDLER;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_BUILD_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_Q_KEY;

import java.io.IOException;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.codice.solr.client.solrj.SolrClient;

@Service
@Command(
    scope = "offline-solr-gazetteer",
    name = "build-suggester-index",
    description = "Sends a request to build the suggester index")
public class BuildGazetteerSuggesterIndexCommand extends AbstractSolrClientCommand {

  @Override
  void executeWithSolrClient(SolrClient solrClient) throws SolrServerException, IOException {
    SolrQuery query = new SolrQuery();
    query.setRequestHandler(GAZETTEER_REQUEST_HANDLER);
    query.setParam(SUGGEST_Q_KEY, "CatalogSolrGazetteerBuildSuggester");
    query.setParam(SUGGEST_BUILD_KEY, true);
    query.setParam(SUGGEST_DICT_KEY, SUGGEST_DICT);

    solrClient.query(query);
  }
}
