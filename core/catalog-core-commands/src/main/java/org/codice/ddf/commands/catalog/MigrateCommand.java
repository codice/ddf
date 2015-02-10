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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Provider;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceProcessingDetails;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.ServiceComparator;

@Command(scope = CatalogCommands.NAMESPACE, name = "migrate", description = "Migrates Metacards from a Federated Source into the Catalog.")
public class MigrateCommand extends DuplicateCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateCommand.class);

    private CatalogFacade ingestProvider;

    private CatalogFacade framework;

    private long start;

    private AtomicInteger queryIndex = new AtomicInteger(1);

    private AtomicInteger ingestCount = new AtomicInteger(0);

    @Override
    protected Object doExecute() throws Exception {

        List<CatalogProvider> providers = getCatalogProviders();

        if (providers.isEmpty() || providers.size() < 2) {
            console.println("Not enough CatalogProviders installed to migrate");
            return null;
        }

        console.println("The \"FROM\" provider is: " + providers.get(0).getClass().getSimpleName());
        CatalogProvider provider = providers.get(1);
        console.println("The \"TO\" provider is: " + provider.getClass().getSimpleName());
        String answer = getInput("Do you wish to continue? (yes/no)");
        if (!"yes".equalsIgnoreCase(answer)) {
            console.println();
            console.println("Now exiting...");
            console.flush();
            return null;
        }

        ingestProvider = new Provider(provider);

        framework = getCatalog();

        start = System.currentTimeMillis();

        final Filter filter = (cqlFilter != null) ?
                CQL.toFilter(cqlFilter) :
                getFilter(getFilterStartTime(start), start, Metacard.MODIFIED);

        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(true);
        query.setPageSize(batchSize);
        query.setSortBy(new SortByImpl(Metacard.MODIFIED, SortOrder.DESCENDING));
        QueryRequest queryRequest = new QueryRequestImpl(query);
        SourceResponse response = null;
        try {
            response = framework.query(queryRequest);
        } catch (FederationException e) {
            printErrorMessage("Error occurred while querying the Framework." + e.getMessage());
            return null;
        } catch (SourceUnavailableException e) {
            printErrorMessage("Error occurred while querying the Framework." + e.getMessage());
            return null;
        } catch (UnsupportedQueryException e) {
            printErrorMessage("Error occurred while querying the Framework." + e.getMessage());
            return null;
        }

        final long totalPossible = response.getHits();
        if (totalPossible == 0) {
            console.println("No records were found to migrate.");
            return null;
        }

        console.println("Starting migration for " + totalPossible + " Records");

        if (multithreaded > 1 && totalPossible > batchSize) {
            BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(multithreaded);
            RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
            final ExecutorService executorService = new ThreadPoolExecutor(multithreaded,
                    multithreaded, 0L, TimeUnit.MILLISECONDS, blockingQueue,
                    rejectedExecutionHandler);
            console.printf("Running %d threads during replication.%n", multithreaded);

            do {
                LOGGER.debug("In loop at iteration {}", queryIndex.get());
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        int count = queryAndIngest(framework, ingestProvider, queryIndex.get(),
                                filter);
                        printProgressAndFlush(start, totalPossible, ingestCount.addAndGet(count));
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
                int count = queryAndIngest(framework, ingestProvider, queryIndex.get(), filter);
                printProgressAndFlush(start, totalPossible, ingestCount.addAndGet(count));
            } while (queryIndex.addAndGet(batchSize) <= totalPossible);
        }

        console.println();
        long end = System.currentTimeMillis();
        String completed = String.format(
                " %d record(s) replicated; %d record(s) failed; completed in %3.3f seconds.",
                ingestCount.get(), failedCount.get(), (end - start) / MILLISECONDS_PER_SECOND);
        LOGGER.info("Replication Complete: {}", completed);
        console.println(completed);

        return null;
    }



    @Override
    protected List<Metacard> query(CatalogFacade framework, int startIndex, Filter filter) {
        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(false);
        query.setPageSize(batchSize);
        query.setSortBy(new SortByImpl(Metacard.MODIFIED, SortOrder.DESCENDING));
        QueryRequest queryRequest = new QueryRequestImpl(query);
        query.setStartIndex(startIndex);
        SourceResponse response = null;
        try {
            LOGGER.debug("Querying with startIndex: {}", startIndex);
            response = framework.query(queryRequest);
        } catch (UnsupportedQueryException e) {
            printErrorMessage(String.format("Received error from Framework: %s%n", e.getMessage()));
            return null;
        } catch (SourceUnavailableException e) {
            printErrorMessage(String.format("Received error from Frameworks: %s%n", e.getMessage()));
            return null;
        } catch (FederationException e) {
            printErrorMessage(String.format("Received error from Frameworks: %s%n", e.getMessage()));
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

    private List<CatalogProvider> getCatalogProviders() {
        ServiceTracker st = new ServiceTracker(getBundleContext(), CatalogProvider.class.getName(),
                null);
        st.open();
        ServiceReference<CatalogProvider>[] serviceRefs = st.getServiceReferences();

        Map<ServiceReference<CatalogProvider>, CatalogProvider> map = new TreeMap<ServiceReference<CatalogProvider>, CatalogProvider>(
                new ServiceComparator());

        if (null != serviceRefs) {
            for (ServiceReference<CatalogProvider> serviceReference : serviceRefs) {
                map.put(serviceReference, (CatalogProvider) st.getService(serviceReference));
            }
        }

        return new ArrayList<CatalogProvider>(map.values());
    }
}
