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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.util.impl.ResultIterable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.solr.client.solrj.SolrServerException;
import org.codice.ddf.security.Security;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.client.solrj.UnavailableSolrException;
import org.codice.solr.factory.SolrClientFactory;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
  scope = "offline-solr-gazetteer",
  name = "synccatalog",
  description = "Syncs all catalog items to the offline solr gazetteer core"
)
public class SyncCatalogCommand implements Action {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RemoveAllOfflineSolrGazetteerCommand.class);
  public static final int PARTITION_SIZE = 256;

  @Reference private SolrClientFactory clientFactory;

  @Reference private Session session;

  @Reference private CatalogFramework catalogFramework;
  @Reference private FilterBuilder filterBuilder;
  @Reference private Security security;

  private final RetryPolicy retryPolicy =
      new RetryPolicy()
          .retryOn(ImmutableList.of(UnavailableSolrException.class, SolrServerException.class))
          .withMaxDuration(5, TimeUnit.SECONDS)
          .withBackoff(50, 1_000, TimeUnit.MILLISECONDS);

  @Override
  public Object execute() throws Exception {
    return security.runWithSubjectOrElevate(this::executeWithSubject);
  }

  public Object executeWithSubject() throws Exception {
    SolrClient solrClient =
        clientFactory.newClient(OfflineGazetteerPlugin.STANDALONE_GAZETTEER_CORE_NAME);

    Failsafe.with(retryPolicy).get(() -> solrClient.ping());

    Iterable<Result> iterable =
        ResultIterable.resultIterable(catalogFramework, getGazetteerFilter());

    session.getConsole().println("Starting sync...");
    long counter = 0;
    Instant start = Instant.now();

    for (List<Result> results : Iterables.partition(iterable, PARTITION_SIZE)) {
      if (Thread.interrupted()) {
        LOGGER.info("Catalog sync interrupted early, exiting");
        session.getConsole().println("Catalog sync interrupted, exiting");
        Thread.currentThread().interrupt();
        throw new InterruptedException();
      }

      try {
        solrClient.add(
            results
                .stream()
                .map(Result::getMetacard)
                .map(OfflineGazetteerPlugin::convert)
                .collect(Collectors.toList()));
      } catch (SolrServerException | IOException e) {
        LOGGER.info("error while adding items to solr", e);
        session.getConsole().printf("An error occured while syncing: %s", e.getMessage());
        throw e;
      }
      counter += results.size();
    }

    session
        .getConsole()
        .printf(
            "%nComplete. Processed %d items in %s%n",
            counter, Duration.between(start, Instant.now()).toString());

    return null;
  }

  public QueryRequest getGazetteerFilter() {
    return new QueryRequestImpl(
        new QueryImpl(
            filterBuilder.attribute(Core.METACARD_TAGS).like().text("gazetteer"),
            1,
            PARTITION_SIZE,
            new SortByImpl(Core.ID, SortOrder.ASCENDING),
            true,
            TimeUnit.MINUTES.toMillis(3)));
  }
}
