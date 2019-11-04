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
package org.codice.ddf.commands.solr;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.SolrMetacardClientImpl;
import ddf.security.Subject;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.codice.ddf.security.common.Security;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;

@Service
@Command(
  scope = SolrCommands.NAMESPACE,
  name = "reindex",
  description =
      "Reindexes data from a source collection to current Catalog Framework collection(s)."
)
public class ReindexCommand extends SolrCommands {
  private static final int PAGE_SIZE = 200;

  private static final int WRITE_TXN_SIZE = 100;

  @VisibleForTesting protected static final String EARLY_TIME = "1900-01-01T00:00:00.000Z";

  private Security security = Security.getInstance();

  private String cursorMark = CursorMarkParams.CURSOR_MARK_START;

  private Reader readerThread = null;

  private SolrMetacardClientImpl metacardClient =
      new SolrMetacardClientImpl(null, null, null, new DynamicSchemaResolver());

  private long totalCount = 0;

  private AtomicLong count = new AtomicLong();

  private long startTime;

  private ThreadPoolExecutor publishExecutor;

  private SolrClient solrjClient = null;

  @Reference private CatalogFramework catalogFramework;

  @Reference private SolrClientFactory clientFactory;

  @Option(
    name = "-s",
    aliases = {"--source"},
    description =
        "The source Solr system to retrieve data. Should be in the form of http://host:port.",
    required = true
  )
  private String sourceSolrHost;

  @Option(
    name = "-c",
    aliases = {"--sourceCollection"},
    description = "The source collection (or alias) to migrate data from.",
    required = true
  )
  private String sourceCollection;

  @Option(
    name = "-f",
    aliases = {"--field"},
    description =
        "Field used for date comparisons. Default (Date of indexing): metacard.created_tdt",
    required = false
  )
  private String field = "metacard.created_tdt";

  @Option(
    name = "-a",
    aliases = {"--after"},
    description =
        "After date used to restrict data to be migrated. Should be in the format: 2019-01-01T00:00:00Z.",
    required = false
  )
  private String afterDate;

  @Option(
    name = "-b",
    aliases = {"--before"},
    description =
        "Before date used to restrict data to be migrated. Should be in the format: 2019-01-01T00:00:00Z.",
    required = false
  )
  private String beforeDate;

  @Option(
    name = "-p",
    aliases = {"--practice"},
    description = "Display the query that would run and exit. No data will be migrated",
    required = false
  )
  private boolean practice = false;

  @Option(
    name = "-t",
    aliases = {"--threads"},
    description =
        "Number of writer threads for parallel processing. Default is number of processors",
    required = false
  )
  int numThreads = Runtime.getRuntime().availableProcessors();

  @Override
  public Object execute() throws Exception {
    if (StringUtils.isEmpty(sourceSolrHost) || StringUtils.isEmpty(sourceCollection)) {
      throw new IllegalArgumentException(
          "Source Solr Host and Source Collection need to be provided");
    }
    if (practice) {
      printInfoMessage("Solr Query: " + getQuery());
      return null;
    }

    if (solrjClient == null) {
      solrjClient = clientFactory.newClient(sourceCollection);
    }

    try {
      if (isSolrClientAvailable(solrjClient)) {
        totalCount = getHits(solrjClient);
        if (totalCount > 0) {
          LOGGER.debug("Number of records to reindex: {}", totalCount);
          startTime = System.currentTimeMillis();
          printProgress();
          migrate(solrjClient);
          waitForCompletion();
          printInfoMessage("\nRe-Index complete");
        } else {
          printInfoMessage("\nNothing to re-index");
        }
        stopWorkers(false);
      } else {
        printErrorMessage("The Solr client is not available.");
      }
    } catch (Exception e) {
      printErrorMessage("Unable to complete re-index: " + e.getMessage());
      stopWorkers(true);
      LOGGER.info("Reindexing failed", e);
      throw e;
    }

    printInfoMessage("Re-Indexing has been completed. " + count.get() + " records processed");

    return null;
  }

  @VisibleForTesting
  protected void setSolrjClient(SolrClient solrjClient) {
    this.solrjClient = solrjClient;
  }

  @VisibleForTesting
  protected void setMetacardClient(SolrMetacardClientImpl metacardClient) {
    this.metacardClient = metacardClient;
  }

  @VisibleForTesting
  protected void setNumThread(int numThreads) {
    this.numThreads = numThreads;
  }

  @VisibleForTesting
  protected void setSourceSolrHost(String sourceSolrHost) {
    this.sourceSolrHost = sourceSolrHost;
  }

  @VisibleForTesting
  protected void setSourceCollection(String sourceCollection) {
    this.sourceCollection = sourceCollection;
  }

  @VisibleForTesting
  protected void setSecurity(Security security) {
    this.security = security;
  }

  @VisibleForTesting
  protected void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  @VisibleForTesting
  protected void setAfterDate(String afterDate) {
    this.afterDate = afterDate;
  }

  @VisibleForTesting
  protected void setBeforeDate(String beforeDate) {
    this.beforeDate = beforeDate;
  }

  @VisibleForTesting
  protected void setField(String field) {
    this.field = field;
  }

  private boolean isSolrClientAvailable(org.codice.solr.client.solrj.SolrClient solrClient) {
    RetryPolicy retryPolicy =
        new RetryPolicy()
            .withDelay(100, TimeUnit.MILLISECONDS)
            .withMaxDuration(3, TimeUnit.MINUTES)
            .retryWhen(false);
    Failsafe.with(retryPolicy).get((Callable<Boolean>) solrClient::isAvailable);
    return solrClient.isAvailable();
  }

  private void migrate(org.codice.solr.client.solrj.SolrClient sourceSolr) {
    startDataQueryThread(sourceSolr);
    startDataWriterThreads();
  }

  private void startDataWriterThreads() {
    ThreadFactory threadFactory = Executors.defaultThreadFactory();
    publishExecutor =
        new ThreadPoolExecutor(
            numThreads,
            numThreads,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<>(numThreads),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy());
  }

  private void startDataQueryThread(org.codice.solr.client.solrj.SolrClient sourceSolr) {
    readerThread = new Reader(sourceSolr);
    readerThread.setName("reindex-reader");
    readerThread.start();
  }

  private void stopWorkers(boolean force) {
    if (readerThread != null && readerThread.isRunning()) {
      readerThread.interrupt();
    }

    if (publishExecutor != null) {
      if (!publishExecutor.isShutdown()) {
        if (force) {
          publishExecutor.shutdownNow();
        } else {
          publishExecutor.shutdown();
        }
      }
    }
  }

  private void waitForCompletion() throws InterruptedException {
    boolean running = true;
    if (readerThread == null) {
      return;
    }

    while (running) {
      if (!readerThread.isRunning()) {
        publishExecutor.shutdown();
        publishExecutor.awaitTermination(15, TimeUnit.MINUTES);
        running = false;
      }
      Thread.sleep(1000);
    }
  }

  private List<Metacard> getData(org.codice.solr.client.solrj.SolrClient sourceSolr)
      throws IOException, SolrServerException {
    final SolrQuery query = getQuery();
    List<Metacard> data = new ArrayList<>();

    LOGGER.trace("Retrieving data with query: {}", query);

    /**
     * Always query the default collection, unable to use overload function that takes in a core
     * name due to how HttpSolrClientFactory create its client
     * https://github.com/codice/ddf/blob/master/platform/solr/solr-factory-impl/src/main/java/org/codice/solr/factory/impl/HttpSolrClientFactory.java#L130
     * this limitation also spill over when using solr Cloud
     */
    QueryResponse response = sourceSolr.query(query);
    cursorMark = response.getNextCursorMark();
    SolrDocumentList docList = response.getResults();

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Query returned: {} items", docList.size());
    }

    for (SolrDocument doc : docList) {
      try {
        Metacard metacard = metacardClient.createMetacard(doc);
        data.add(metacard);
      } catch (MetacardCreationException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unable to convert: {} to metacard", doc, e);
        }
      }
    }
    return data;
  }

  @VisibleForTesting
  SolrQuery getQuery() {
    StringBuilder querySB = new StringBuilder();
    if (StringUtils.isNotBlank(afterDate) && StringUtils.isNotBlank(beforeDate)) {
      querySB.append("[").append(afterDate).append(" TO ").append(beforeDate).append("]");
    } else if (StringUtils.isNotBlank(afterDate) && StringUtils.isBlank(beforeDate)) {
      querySB.append("[").append(afterDate).append(" TO NOW").append("]");
    } else if (StringUtils.isBlank(afterDate) && StringUtils.isNotBlank(beforeDate)) {
      querySB.append("[").append(EARLY_TIME).append(" TO ").append(beforeDate).append("]");
    }

    SolrQuery query;
    if (querySB.length() > 0) {
      query = new SolrQuery(field + ":" + querySB.toString());
    } else {
      query = new SolrQuery("*:*");
    }

    SortClause sort = new SortClause("metacard.created_tdt", ORDER.desc);
    query.setSort(sort);
    query.addSort(new SortClause("id_txt", ORDER.desc));
    query.setParam(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
    query.setRows(Math.max(PAGE_SIZE, WRITE_TXN_SIZE));
    return query;
  }

  private long getHits(org.codice.solr.client.solrj.SolrClient sourceSolr)
      throws IOException, SolrServerException {
    final SolrQuery query = getQuery();
    query.setRows(0);
    query.setStart(0);
    query.addSort(new SortClause("id_txt", ORDER.desc));
    /**
     * Always query the default collection, unable to use overload function that takes in a core
     * name due to how HttpSolrClientFactory create its client
     * https://github.com/codice/ddf/blob/master/platform/solr/solr-factory-impl/src/main/java/org/codice/solr/factory/impl/HttpSolrClientFactory.java#L130
     * this limitation also spill over when using solr Cloud
     */
    QueryResponse response = sourceSolr.query(query);
    if (response != null && response.getResults() != null) {
      return response.getResults().getNumFound();
    }
    return 0;
  }

  private void printProgress() {
    printProgressAndFlush(startTime, totalCount, count.get());
  }

  class Reader extends Thread {
    private boolean running = true;
    private org.codice.solr.client.solrj.SolrClient sourceSolr;

    public Reader(org.codice.solr.client.solrj.SolrClient sourceSolr) {
      this.sourceSolr = sourceSolr;
    }

    @Override
    public void run() {
      while (running) {
        List<Metacard> data = null;
        try {
          data = getData(sourceSolr);
        } catch (IOException | SolrServerException e) {
          LOGGER.info("Unable to query solr data", e);
        }

        if (CollectionUtils.isEmpty(data)) {
          running = false;
          LOGGER.trace("No more data to be retrieved from: {}", sourceSolrHost);
        }

        if (!running) {
          break;
        }

        try {
          LOGGER.debug("Data ({}) retrieved, adding to work queue", data.size());
          addWorkItems(data);
        } catch (InterruptedException e) {
          LOGGER.warn("Unable to complete reindexing. Process interrupted", e);
          Thread.currentThread().interrupt();
        }
      }
    }

    public boolean isRunning() {
      return running;
    }

    void addWorkItems(List<Metacard> data) throws InterruptedException {
      if (data.size() <= WRITE_TXN_SIZE) {
        WorkItem workItem = new WorkItem(data);
        publishExecutor.execute(new Publisher(workItem));
      } else {
        List<Metacard> metacards = new ArrayList<>(WRITE_TXN_SIZE);
        for (Metacard metacard : data) {
          if (metacards.size() >= WRITE_TXN_SIZE) {
            WorkItem workItem = new WorkItem(metacards);
            publishExecutor.execute(new Publisher(workItem));
            metacards = new ArrayList<>(WRITE_TXN_SIZE);
          }
          metacards.add(metacard);
        }
        if (!metacards.isEmpty()) {
          WorkItem workItem = new WorkItem(metacards);
          publishExecutor.execute(new Publisher(workItem));
        }
      }
    }
  }

  class Publisher implements Runnable {
    private WorkItem workItem;

    public Publisher(WorkItem workItem) {
      this.workItem = workItem;
    }

    @Override
    public void run() {
      CreateRequest createRequest = new CreateRequestImpl(new ArrayList<>(workItem.getMetacards()));

      Subject systemSubject =
          AccessController.doPrivileged(
              (PrivilegedAction<Subject>) () -> security.runAsAdmin(security::getSystemSubject));
      systemSubject.execute((Callable) () -> catalogFramework.create(createRequest));
      count.addAndGet(workItem.getMetacards().size());
      printProgress();
    }
  }

  class WorkItem {
    List<Metacard> metacards;

    public WorkItem(List<Metacard> metacards) {
      this.metacards = metacards;
    }

    public List<Metacard> getMetacards() {
      return metacards;
    }
  }
}
