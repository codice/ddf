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
package org.codice.ddf.commands.catalog;

import com.google.common.collect.Iterables;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.util.impl.ResultIterable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DuplicateCommands extends CqlCommands {

  protected static final int MAX_BATCH_SIZE = 1000;

  private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateCommands.class);

  protected AtomicInteger ingestedCount = new AtomicInteger(0);

  protected AtomicInteger failedCount = new AtomicInteger(0);

  protected Set<Metacard> failedMetacards = Collections.synchronizedSet(new HashSet<>());

  protected long start;

  @Option(
    name = "--batchsize",
    required = false,
    aliases = {"-b"},
    multiValued = false,
    description =
        "Number of Metacards to query and ingest at a time. Change this argument based on system memory and Catalog Provider limits."
  )
  int batchSize = MAX_BATCH_SIZE;

  @Option(
    name = "--multithreaded",
    required = false,
    aliases = {"-m"},
    multiValued = false,
    description =
        "Number of threads to use when ingesting. Setting this value too high for your system can cause performance degradation."
  )
  int multithreaded = 1;

  @Option(
    name = "--failedDir",
    required = false,
    aliases = {"-f"},
    multiValued = false,
    description = "Option to specify where to write metacards that failed to ingest."
  )
  String failedDir;

  @Option(
    name = "--maxMetacards",
    required = false,
    aliases = {"-mm", "-max"},
    multiValued = false,
    description = "Option to specify a maximum amount of metacards to query."
  )
  int maxMetacards;

  /**
   * In batches, loops through a query of the queryFacade and an ingest to the ingestFacade of the
   * metacards from the response until there are no more metacards from the queryFacade or the
   * maxMetacards has been reached.
   *
   * @param queryFacade - the CatalogFacade to duplicate from
   * @param ingestFacade - the CatalogFacade to duplicate to
   * @param filter - the filter to query with
   */
  protected void duplicateInBatches(
      CatalogFacade queryFacade, CatalogFacade ingestFacade, Filter filter, String sourceId)
      throws InterruptedException {
    AtomicInteger queryIndex = new AtomicInteger(1);

    final long originalQuerySize;
    if (maxMetacards > 0 && maxMetacards < batchSize) {
      originalQuerySize = maxMetacards;
    } else {
      originalQuerySize = batchSize;
    }

    Function<Integer, QueryRequest> queryTemplate =
        (index) ->
            new QueryRequestImpl(
                new QueryImpl(
                    filter,
                    index,
                    (int) originalQuerySize,
                    new SortByImpl(Metacard.EFFECTIVE, SortOrder.DESCENDING),
                    true,
                    TimeUnit.MINUTES.toMillis(5)),
                Collections.singletonList(sourceId));

    List<Metacard> initialMetacards =
        ResultIterable.resultIterable(
                (queryRequest -> {
                  SourceResponse response = queryFacade.query(queryRequest);
                  if (response.getHits() != -1) {
                    maxMetacards = (int) response.getHits();
                  }
                  return response;
                }),
                queryTemplate.apply(queryIndex.get()),
                (int) originalQuerySize)
            .stream()
            .map(Result::getMetacard)
            .collect(Collectors.toList());

    if (initialMetacards.isEmpty()) {
      LOGGER.debug("Query returned 0 results.");
      console.println(String.format("No results were returned by the source [%s]", sourceId));
      return;
    }

    ingestMetacards(ingestFacade, initialMetacards);

    if (initialMetacards.size() < originalQuerySize) {
      // all done if results exhausted in the first batch
      printProgressAndFlush(
          start, maxMetacards < 1 ? initialMetacards.size() : maxMetacards, ingestedCount.get());
      return;
    }

    final long totalWanted = maxMetacards;
    final AtomicBoolean done = new AtomicBoolean(false);
    if (multithreaded > 1) {
      BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(multithreaded);
      RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
      final ExecutorService executorService =
          new ThreadPoolExecutor(
              multithreaded,
              multithreaded,
              0L,
              TimeUnit.MILLISECONDS,
              blockingQueue,
              StandardThreadFactoryBuilder.newThreadFactory("duplicateCommandsThread"),
              rejectedExecutionHandler);
      console.printf("Running a maximum of %d threads during replication.%n", multithreaded);

      printProgressAndFlush(
          start, Math.max(totalWanted, initialMetacards.size()), ingestedCount.get());
      int index;
      while (!done.get()) {
        index = queryIndex.addAndGet(batchSize);
        final int taskIndex = index;

        executorService.submit(
            () -> {
              int querySize = (int) getQuerySizeFromIndex(totalWanted, taskIndex);
              if (querySize < 1) {
                // If we don't need any more metacards, we're finished
                done.set(true);
                return;
              }
              List<Metacard> metacards =
                  ResultIterable.resultIterable(
                          queryFacade::query, queryTemplate.apply(taskIndex), querySize)
                      .stream()
                      .map(Result::getMetacard)
                      .collect(Collectors.toList());

              if (metacards.size() < querySize) {
                done.set(true);
              }
              if (!metacards.isEmpty()) {
                ingestMetacards(ingestFacade, metacards);
              }
              printProgressAndFlush(
                  start, Math.max(totalWanted, ingestedCount.get()), ingestedCount.get());
            });
      }

      executorService.shutdown();
      try {
        executorService.awaitTermination(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        throw e;
      }

      printProgressAndFlush(start, Math.max(totalWanted, ingestedCount.get()), ingestedCount.get());
    } else { // Single threaded
      ResultIterable iter;
      if (maxMetacards > 0) {
        iter =
            ResultIterable.resultIterable(
                queryFacade::query, queryTemplate.apply(1 + batchSize), maxMetacards);
      } else {
        iter =
            ResultIterable.resultIterable(queryFacade::query, queryTemplate.apply(1 + batchSize));
      }

      Iterables.partition(iter, batchSize)
          .forEach(
              (batch) -> {
                printProgressAndFlush(start, totalWanted, ingestedCount.get());
                if (batch.isEmpty()) {
                  return;
                }
                ingestMetacards(
                    ingestFacade,
                    batch.stream().map(Result::getMetacard).collect(Collectors.toList()));
              });
    }

    printProgressAndFlush(start, totalWanted, ingestedCount.get());

    if (failedCount.get() > 0) {
      LOGGER.info("Not all records were ingested. [{}] failed", failedCount.get());
      if (StringUtils.isNotBlank(failedDir)) {
        try {
          writeFailedMetacards(failedMetacards);
        } catch (IOException e) {
          console.println("Error occurred while writing failed metacards to failedDir.");
        }
      }
    }
  }

  /**
   * On the final iteration of the loop when the maxMetacards is less than the number of metacards
   * available in the ingestFacade, the query should only return the remaining wanted metacards, not
   * the full batch size.
   *
   * @param maxMetacards - maximum number of metacards wanted or 0
   * @param currentIndex - the index to be queried next
   * @return how many metacards should be returned by a query starting at {@param currentIndex}
   */
  private long getQuerySizeFromIndex(final long maxMetacards, final long currentIndex) {
    if (maxMetacards > 0) {
      return Math.min(maxMetacards - (currentIndex - 1), batchSize);
    }
    return batchSize;
  }

  protected List<Metacard> ingestMetacards(CatalogFacade provider, List<Metacard> metacards) {
    if (metacards.isEmpty()) {
      return Collections.emptyList();
    }
    List<Metacard> createdMetacards = new ArrayList<>();
    LOGGER.debug("Preparing to ingest {} records", metacards.size());
    CreateRequest createRequest = new CreateRequestImpl(metacards);

    CreateResponse createResponse;
    try {
      createResponse = provider.create(createRequest);
      createdMetacards = createResponse.getCreatedMetacards();
    } catch (IngestException e) {
      printErrorMessage(String.format("Received error while ingesting: %s%n", e.getMessage()));
      LOGGER.debug("Error during ingest. Attempting to ingest batch individually.");
      return ingestSingly(provider, metacards);
    } catch (SourceUnavailableException e) {
      printErrorMessage(String.format("Received error while ingesting: %s%n", e.getMessage()));
      LOGGER.debug("Error during ingest:", e);
      return createdMetacards;
    } catch (Exception e) {
      printErrorMessage(
          String.format("Unexpected Exception received while ingesting: %s%n", e.getMessage()));
      LOGGER.debug("Unexpected Exception during ingest:", e);
      return createdMetacards;
    }

    ingestedCount.addAndGet(createdMetacards.size());
    failedCount.addAndGet(metacards.size() - createdMetacards.size());
    failedMetacards.addAll(subtract(metacards, createdMetacards));

    return createdMetacards;
  }

  private List<Metacard> ingestSingly(CatalogFacade provider, List<Metacard> metacards) {
    if (metacards.isEmpty()) {
      return Collections.emptyList();
    }
    List<Metacard> createdMetacards = new ArrayList<>();
    LOGGER.debug("Preparing to ingest {} records one at time.", metacards.size());
    for (Metacard metacard : metacards) {
      CreateRequest createRequest = new CreateRequestImpl(Arrays.asList(metacard));

      CreateResponse createResponse;
      try {
        createResponse = provider.create(createRequest);
        createdMetacards.addAll(createResponse.getCreatedMetacards());
      } catch (IngestException | SourceUnavailableException e) {
        LOGGER.debug("Error during ingest:", e);
      } catch (Exception e) {
        LOGGER.debug("Unexpected Exception during ingest:", e);
      }
    }

    return createdMetacards;
  }

  protected List<Metacard> subtract(List<Metacard> queried, List<Metacard> ingested) {
    List<Metacard> result = new ArrayList<>(queried);
    result.removeAll(ingested);
    return result;
  }

  protected void writeFailedMetacards(Set<Metacard> failedMetacardsToWrite) throws IOException {
    File directory = new File(failedDir);
    if (!directory.exists()) {
      if (!directory.mkdirs()) {
        printErrorMessage("Unable to create directory [" + directory.getAbsolutePath() + "].");
        return;
      }
    }

    if (!directory.canWrite()) {
      printErrorMessage("Directory [" + directory.getAbsolutePath() + "] is not writable.");
      return;
    }
    for (Metacard metacard : failedMetacardsToWrite) {

      try (ObjectOutputStream oos =
          new ObjectOutputStream(
              new FileOutputStream(new File(directory.getAbsolutePath(), metacard.getId())))) {
        oos.writeObject(new MetacardImpl(metacard));
        oos.flush();
      }
    }
  }
}
