/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
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

@Command(scope = CatalogCommands.NAMESPACE, name = "migrate", description = "Migrates Metacards "
        + "from one Provider to another Provider.")
public class MigrateCommand extends DuplicateCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateCommand.class);

    @Option(name = "--list", required = false, aliases = {
            "-list"}, multiValued = false, description = "Print a list of Providers.")
    boolean listProviders = false;

    @Option(name = "--from", required = false, aliases = {
            "-from"}, multiValued = false, description = "The Source ID of the Provider to migrate from.")
    String fromProviderId;

    @Option(name = "--to", required = false, aliases = {
            "-to"}, multiValued = false, description = "The Source ID of the Provider to migrate to.")
    String toProviderId;

    @Override
    protected Object executeWithSubject() throws Exception {
        final List<CatalogProvider> providers = getCatalogProviders();

        if (listProviders) {
            if (providers.size() == 0) {
                console.println("There are no available providers.");
                return null;
            }
            console.println("Available providers:");
            providers.stream()
                    .map(p -> p.getClass()
                            .getSimpleName())
                    .forEach(id -> console.println("\t" + id));

            return null;
        }

        if (batchSize > MAX_BATCH_SIZE || batchSize < 1) {
            console.println("Batch Size must be between 1 and " + MAX_BATCH_SIZE + ".");
            return null;
        }

        if (providers.isEmpty() || providers.size() < 2) {
            console.println("Not enough CatalogProviders installed to migrate.");
            return null;
        }

        final CatalogProvider fromProvider = promptForProvider("FROM", fromProviderId, providers);
        if (fromProvider == null) {
            console.println("Invalid \"FROM\" provider id.");
            return null;
        }
        console.println("FROM provider ID: " + fromProvider.getClass()
                .getSimpleName());

        final CatalogProvider toProvider = promptForProvider("TO", toProviderId, providers);
        if (toProvider == null) {
            console.println("Invalid \"TO\" provider id.");
            return null;
        }
        console.println("TO provider ID: " + toProvider.getClass()
                .getSimpleName());

        CatalogFacade queryProvider = new Provider(fromProvider);
        CatalogFacade ingestProvider = new Provider(toProvider);

        start = System.currentTimeMillis();

        final Filter filter = (cqlFilter != null) ? CQL.toFilter(cqlFilter) : getFilter(
                getFilterStartTime(start),
                start,
                getTemporalProperty());

        console.println("Starting migration.");

        duplicateInBatches(queryProvider, ingestProvider, filter);

        console.println();
        long end = System.currentTimeMillis();
        String completed = String.format(
                " %d record(s) migrated; %d record(s) failed; completed in %3.3f seconds.",
                ingestedCount.get(),
                failedCount.get(),
                (end - start) / MS_PER_SECOND);
        LOGGER.debug("Migration Complete: {}", completed);
        console.println(completed);

        return null;
    }

    private CatalogProvider promptForProvider(String whichProvider, String id,
            List<CatalogProvider> providers) throws IOException {
        List<String> providersIdList = providers.stream()
                .map(p -> p.getClass()
                        .getSimpleName())
                .collect(Collectors.toList());
        while (true) {
            if (StringUtils.isBlank(id) || !providersIdList.contains(id)) {
                console.println(
                        "Please enter the Source ID of the \"" + whichProvider + "\" provider:");
            } else {
                break;
            }
            id = getInput(whichProvider + " provider ID: ");
        }

        final String providerId = id;
        final CatalogProvider provider = providers.stream()
                .filter(p -> p.getClass()
                        .getSimpleName()
                        .equals(providerId))
                .findFirst()
                .orElse(null);

        return provider;
    }

    @Override
    protected SourceResponse query(CatalogFacade framework, Filter filter, int startIndex,
            long querySize) {
        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(true);
        query.setPageSize((int) querySize);
        query.setSortBy(new SortByImpl(Metacard.MODIFIED, SortOrder.DESCENDING));
        QueryRequest queryRequest = new QueryRequestImpl(query);
        query.setStartIndex(startIndex);
        SourceResponse response;
        try {
            LOGGER.debug("Querying with startIndex: {}", startIndex);
            response = framework.query(queryRequest);
        } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
            printErrorMessage(String.format("Received error from Frameworks: %s%n",
                    e.getMessage()));
            return null;
        }
        if (response.getProcessingDetails() != null && !response.getProcessingDetails()
                .isEmpty()) {
            for (SourceProcessingDetails details : response.getProcessingDetails()) {
                LOGGER.debug("Got Issues: {}", details.getWarnings());
            }
            return null;
        }

        return response;
    }

    private List<CatalogProvider> getCatalogProviders() {
        ServiceTracker st = new ServiceTracker(getBundleContext(),
                CatalogProvider.class.getName(),
                null);
        st.open();
        ServiceReference<CatalogProvider>[] serviceRefs = st.getServiceReferences();

        Map<ServiceReference<CatalogProvider>, CatalogProvider> map =
                new TreeMap<>(new ServiceComparator());

        if (null != serviceRefs) {
            for (ServiceReference<CatalogProvider> serviceReference : serviceRefs) {
                map.put(serviceReference, (CatalogProvider) st.getService(serviceReference));
            }
        }

        return new ArrayList<>(map.values());
    }
}
