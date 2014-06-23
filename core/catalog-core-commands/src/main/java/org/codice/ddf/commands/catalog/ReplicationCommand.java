/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.commands.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceProcessingDetails;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Custom Karaf command to replicate records from a Federated Source into the Catalog.
 * 
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "replicate", description = "Replicates Metacards from a Federated Source into the Catalog.")
public class ReplicationCommand extends DuplicateCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationCommand.class);

    private long start;

    private List<Metacard> failedMetacards = Collections
            .synchronizedList(new ArrayList<Metacard>());

    private AtomicInteger queryIndex = new AtomicInteger(1);

    private AtomicInteger ingestCount = new AtomicInteger(0);

    @Argument(name = "Source Id", description = "The ID of the Source to replicate the data from.", index = 0, multiValued = false, required = false)
    String sourceId;

    @Override
    protected Object doExecute() throws Exception {
        final CatalogFacade catalog = getCatalog();

        final CatalogFacade framework = new Framework(getService(CatalogFramework.class));
        Set<String> sourceIds = framework.getSourceIds();

        while (true) {
            if (StringUtils.isBlank(sourceId) || !sourceIds.contains(sourceId)) {
                console.println("Please enter the Source ID you would like to replicate:");
                for (String id : sourceIds) {
                    console.println("\t" + id);
                }
            } else {
                break;
            }
            sourceId = getInput("ID:  ");
        }

        if (batchSize > MAX_BATCH_SIZE || batchSize < 1) {
            console.println("Batch Size must be between 1 and 1000.");
            return null;
        }
        
        start = System.currentTimeMillis();

        final Filter filter = getFilter(getFilterStartTime(start), start, Metacard.EFFECTIVE);

        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(true);
        query.setPageSize(batchSize);
        query.setSortBy(new SortByImpl(Metacard.EFFECTIVE, SortOrder.DESCENDING));
        QueryRequest queryRequest = new QueryRequestImpl(query, Arrays.asList(sourceId));
        SourceResponse response = null;
        try {
            response = framework.query(queryRequest);
        } catch (Exception e) {
            console.println("Error occured while querying the Federated Source.\n" + e.getMessage());
            return null;
        }


        final long totalPossible = response.getHits();
        if (totalPossible == 0) {
            console.println("No records were found to replicate.");
            return null;
        }
        
        console.println("Starting replication for " + totalPossible + " Records");

        if (multithreaded > 1 && totalPossible > batchSize) {
            BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(multithreaded);
            RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
            final ExecutorService executorService = new ThreadPoolExecutor(multithreaded,
                    multithreaded, 0L, TimeUnit.MILLISECONDS, blockingQueue,
                    rejectedExecutionHandler);
            console.printf("Running %d threads during replication.\n", multithreaded);

            do {
                LOGGER.debug("In loop at iteration {}", queryIndex.get());
                final int startIndex = queryIndex.get();
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        int count = queryAndIngest(framework, catalog, startIndex, filter);
                        printProgressAndFlush(start, totalPossible,
                                ingestCount.addAndGet(count));
                    }
                });
            } while (queryIndex.addAndGet(batchSize) <= totalPossible);
            executorService.shutdown();

            while (!executorService.isTerminated()) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        } else {
            do {
                int count = queryAndIngest(framework, catalog, queryIndex.get(), filter);
                printProgressAndFlush(start, totalPossible, ingestCount.addAndGet(count));
            } while (queryIndex.addAndGet(batchSize) <= totalPossible);
        }

        console.println();
        long end = System.currentTimeMillis();
        String completed = String.format(
                " %d record(s) replicated; %d record(s) failed; completed in %3.3f seconds.",
                ingestCount.get(), failedCount.get(), (end - start)
                        / MILLISECONDS_PER_SECOND);
        LOGGER.info("Replication Complete: {}", completed);
        console.println(completed);

        if (StringUtils.isNotBlank(failedDir)) {
            writeFailedMetacards(failedMetacards);
        }

        return null;
    }

    @Override
    protected List<Metacard> query(CatalogFacade framework, int startIndex, Filter filter) {
        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(false);
        query.setPageSize(batchSize);
        query.setSortBy(new SortByImpl(Metacard.EFFECTIVE, SortOrder.DESCENDING));
        QueryRequest queryRequest = new QueryRequestImpl(query, Arrays.asList(sourceId));
        query.setStartIndex(startIndex);
        SourceResponse response = null;
        try {
            LOGGER.debug("Querying with startIndex: {}", startIndex);
            response = framework.query(queryRequest);
        } catch (UnsupportedQueryException e) {
            console.printf("Recieved error from %s: %s\n", sourceId, e.getMessage());
            return null;
        } catch (SourceUnavailableException e) {
            console.printf("Recieved error from %s: %s\n", sourceId, e.getMessage());
            return null;
        } catch (FederationException e) {
            console.printf("Recieved error from %s: %s\n", sourceId, e.getMessage());
            return null;
        }
        if (response.getProcessingDetails() != null && !response.getProcessingDetails().isEmpty()) {
            for (SourceProcessingDetails details : response.getProcessingDetails()) {
                LOGGER.debug("Got Issues: {}", details.getWarnings());
            }
            return null;
        }
        List<Metacard> metacards = new ArrayList<Metacard>();
        for (Result result : response.getResults()) {
            metacards.add(result.getMetacard());
        }
        return metacards;
    }


}
