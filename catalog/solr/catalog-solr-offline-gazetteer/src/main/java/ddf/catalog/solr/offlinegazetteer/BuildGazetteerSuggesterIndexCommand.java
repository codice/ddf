package ddf.catalog.solr.offlinegazetteer;

import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.COLLECTION_NAME;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.GAZETTEER_REQUEST_HANDLER;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_BUILD_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_Q_KEY;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
  scope = "offline-solr-gazetteer",
  name = "build-suggester-index",
  description = "Sends a request to build the suggester index"
)
public class BuildGazetteerSuggesterIndexCommand implements Action {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BuildGazetteerSuggesterIndexCommand.class);

  @Reference protected Session session;

  @Reference private SolrClientFactory clientFactory;

  @Override
  public Object execute() throws Exception {
    SolrClient solrClient = clientFactory.newClient(COLLECTION_NAME);

    Boolean response =
        Failsafe.with(
                new RetryPolicy()
                    .retryWhen(false)
                    .withMaxDuration(5, TimeUnit.SECONDS)
                    .withBackoff(25, 1_000, TimeUnit.MILLISECONDS))
            .get(() -> solrClient.isAvailable());
    if (response == null || !response) {
      LOGGER.error("Could not contact solr to build suggester index");
      session.getConsole().println("Could not contact solr to build suggester index, exiting.");
      return null;
    }
    SolrQuery query = new SolrQuery();
    query.setRequestHandler(GAZETTEER_REQUEST_HANDLER);
    query.setParam(SUGGEST_Q_KEY, "CatalogSolrGazetteerBuildSuggester");
    query.setParam(SUGGEST_BUILD_KEY, true);
    query.setParam(SUGGEST_DICT_KEY, SUGGEST_DICT);

    try {
      solrClient.query(query);
    } catch (SolrServerException | IOException e) {
      LOGGER.info("Error while trying to build suggester", e);
      session.getConsole().println("Error while trying to build suggester.");
      throw e;
    }

    session.getConsole().println("Suggester built successfully.");
    return null;
  }
}
