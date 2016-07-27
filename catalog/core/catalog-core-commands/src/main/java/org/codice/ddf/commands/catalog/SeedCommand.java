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

import static java.util.AbstractMap.SimpleEntry;
import static java.util.Map.Entry;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static ddf.catalog.data.Metacard.ANY_TEXT;
import static ddf.catalog.filter.FilterDelegate.WILDCARD_CHAR;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.fusesource.jansi.Ansi;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;

@Service
@Command(scope = CatalogCommands.NAMESPACE, name = "seed", description =
        "Seeds the local metacard and product caches from the enterprise or from specific federated sources.\n\n"
                + "\tThis command will query the enterprise or the specified sources for metacards in increments "
                + "the size of the `--product-limit` argument (default 20) until that number of product downloads "
                + "have started or until there are no more results. Local products will not be added to the cache. "
                + "This command will not re-download products that are up-to-date in the cache, so subsequent runs "
                + "of the command will attempt to cache metacards and products that have not already been cached.\n\n"
                + "\tNote that this command will begin the product downloads and some may still be in progress "
                + "by the time it completes. Also, product caching must be enabled in the catalog framework for "
                + "this command to seed the product cache.")
public class SeedCommand extends SubjectCommands implements Action {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeedCommand.class);

    private static final Map<String, Serializable> CACHE_UPDATE_PROPERTIES =
            Collections.singletonMap("mode", "update");

    private static final String RESOURCE_CACHE_STATUS = "internal.local-resource";

    @Option(name = "--source", multiValued = true, aliases = {
            "-s"}, description = "Source(s) to query (e.g., -s source1 -s source2). Default is to query the entire enterprise.")
    List<String> sources;

    @Option(name = "--cql", description = "CQL expression specifying the metacards to seed.\n"
            + "CQL Examples:\n" + "\tTextual:   seed --cql \"title like 'some text'\"\n"
            + "\tTemporal:  seed --cql \"modified before 2012-09-01T12:30:00Z\"\n"
            + "\tSpatial:   seed --cql \"DWITHIN(location, POINT (1 2), 10, kilometers)\"\n"
            + "\tComplex:   seed --cql \"title like 'some text' AND modified before 2012-09-01T12:30:00Z\"")
    String cql;

    @Option(name = "--product-limit", aliases = {"-pl"}, description =
            "The maximum number of products to download to the cache. The number of metacards cached "
                    + "might not be equal to this number because some metacards may not have associated "
                    + "products.")
    int productLimit = 20;

    @Reference
    CatalogFramework framework;

    @Reference
    FilterBuilder filterBuilder;

    private final Predicate<Metacard> isNonCachedResource = metacard -> {
        Attribute cached = metacard.getAttribute(RESOURCE_CACHE_STATUS);
        return cached == null || !((Boolean) cached.getValue());
    };

    private Filter createFilter() throws Exception {
        if (isNotBlank(cql)) {
            return CQL.toFilter(cql);
        } else {
            return filterBuilder.attribute(ANY_TEXT)
                    .is()
                    .like()
                    .text(WILDCARD_CHAR);
        }
    }

    @Override
    public Object execute() throws Exception {
        return doExecute();
    }

    @Override
    protected Object executeWithSubject() throws Exception {
        if (productLimit <= 0) {
            printErrorMessage("A limit of " + productLimit
                    + " was supplied. The limit must be greater than 0.");
            return null;
        }

        final Filter filter = createFilter();

        final long start = System.currentTimeMillis();
        int productDownloads = 0;
        int downloadErrors = 0;
        int pageCount = 0;

        while (productDownloads < productLimit) {
            final QueryImpl query = new QueryImpl(filter);
            query.setPageSize(productLimit);
            query.setStartIndex(pageCount * productLimit + 1);
            ++pageCount;

            final QueryRequestImpl queryRequest;
            if (sources != null && !sources.isEmpty()) {
                queryRequest = new QueryRequestImpl(query, sources);
            } else {
                queryRequest = new QueryRequestImpl(query, true);
            }
            queryRequest.setProperties(new HashMap<>(CACHE_UPDATE_PROPERTIES));

            final QueryResponse queryResponse = framework.query(queryRequest);

            if (queryResponse.getResults()
                    .isEmpty()) {
                break;
            }

            final List<Entry<? extends ResourceRequest, String>> resourceRequests =
                    queryResponse.getResults()
                            .stream()
                            .map(Result::getMetacard)
                            .filter(isNonCachedResource)
                            .map(metacard -> new SimpleEntry<>(new ResourceRequestById(metacard.getId()),
                                    metacard.getSourceId()))
                            .collect(toList());

            for (Entry<? extends ResourceRequest, String> requestAndSourceId : resourceRequests) {
                final ResourceRequest request = requestAndSourceId.getKey();
                try {
                    framework.getResource(request, requestAndSourceId.getValue());
                    ++productDownloads;
                } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
                    ++downloadErrors;
                    LOGGER.warn("Could not download product for metacard [id={}]",
                            request.getAttributeValue(),
                            e);
                }

                printProgressAndFlush(start, productLimit, productDownloads);

                if (productDownloads == productLimit) {
                    break;
                }
            }
        }

        printProgressAndFlush(start, productDownloads, productDownloads);

        console.println();
        if (downloadErrors > 0) {
            printColor(Ansi.Color.YELLOW,
                    downloadErrors
                            + " product download(s) had errors. Check the logs for details.");
        }
        printSuccessMessage("Done seeding. " + productDownloads + " product download(s) started.");

        return null;
    }
}
