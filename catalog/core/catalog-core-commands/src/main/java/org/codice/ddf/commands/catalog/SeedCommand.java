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
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static ddf.catalog.data.Metacard.ANY_TEXT;
import static ddf.catalog.filter.FilterDelegate.WILDCARD_CHAR;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
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
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;

//@formatter:off
@Service
@Command(
        scope = CatalogCommands.NAMESPACE,
        name = "seed",
        description = "Seeds the local metacard and resource caches from the enterprise or from specific federated sources.",
        detailedDescription = "This command will query the enterprise or the specified sources for metacards in increments "
                + "the size of the `--resource-limit` argument (default 20) until that number of resource downloads "
                + "have started or until there are no more results. Local resources will not be added to the cache. "
                + "This command will not re-download resources that are up-to-date in the cache, so subsequent runs "
                + "of the command will attempt to cache metacards and resources that have not already been cached.\n\n"
                + "Note: this command will trigger resource downloads in the background and they may continue after "
                + "control is returned to the console. Also, resource caching must be enabled in the catalog framework "
                + "for this command to seed the resource cache.")
//@formatter:on
public class SeedCommand extends SubjectCommands implements Action {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeedCommand.class);

    private static final Map<String, Serializable> CACHE_UPDATE_PROPERTIES =
            Collections.singletonMap("mode", "update");

    private static final String RESOURCE_CACHE_STATUS = "internal.local-resource";

    private static final ThreadPoolExecutor EXECUTOR;

    static {
        EXECUTOR = new ThreadPoolExecutor(20,
                20,
                30,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        EXECUTOR.allowCoreThreadTimeOut(true);
    }

    @Option(name = "--source", multiValued = true, aliases = {
            "-s"}, description = "Source(s) to query (e.g., -s source1 -s source2). Default is to query the entire enterprise.")
    List<String> sources;

    @Option(name = "--cql", description = "CQL expression specifying the metacards to seed.\n"
            + "CQL Examples:\n" + "\tTextual:   seed --cql \"title like 'some text'\"\n"
            + "\tTemporal:  seed --cql \"modified before 2012-09-01T12:30:00Z\"\n"
            + "\tSpatial:   seed --cql \"DWITHIN(location, POINT (1 2), 10, kilometers)\"\n"
            + "\tComplex:   seed --cql \"title like 'some text' AND modified before 2012-09-01T12:30:00Z\"")
    String cql;

    @Option(name = "--resource-limit", aliases = {"-rl"}, description =
            "The maximum number of resources to download to the cache. The number of metacards cached "
                    + "might not be equal to this number because some metacards may not have associated "
                    + "resources.")
    int resourceLimit = 20;

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
        if (resourceLimit <= 0) {
            printErrorMessage("A limit of " + resourceLimit
                    + " was supplied. The limit must be greater than 0.");
            return null;
        }

        final Filter filter = createFilter();

        final long start = System.currentTimeMillis();
        int resourceDownloads = 0;
        int downloadErrors = 0;
        int pageCount = 0;

        while (resourceDownloads < resourceLimit) {
            final QueryImpl query = new QueryImpl(filter);
            query.setPageSize(resourceLimit);
            query.setStartIndex(pageCount * resourceLimit + 1);
            ++pageCount;

            final QueryRequestImpl queryRequest;
            if (isNotEmpty(sources)) {
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
                    ResourceResponse response = framework.getResource(request,
                            requestAndSourceId.getValue());
                    ++resourceDownloads;
                    EXECUTOR.execute(new ResourceCloseHandler(response));
                } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
                    ++downloadErrors;
                    LOGGER.debug("Could not download resource for metacard [id={}]",
                            request.getAttributeValue(),
                            e);
                }

                printProgressAndFlush(start, resourceLimit, resourceDownloads);

                if (resourceDownloads == resourceLimit) {
                    break;
                }
            }
        }

        printProgressAndFlush(start, resourceDownloads, resourceDownloads);

        console.println();
        if (downloadErrors > 0) {
            printErrorMessage(downloadErrors
                    + " resource download(s) had errors. Check the logs for details.");
        }
        printSuccessMessage(
                "Done seeding. " + resourceDownloads + " resource download(s) started.");

        return null;
    }

    private static class ResourceCloseHandler implements Runnable {
        private final InputStream resourceStream;

        private ResourceCloseHandler(ResourceResponse response) {
            resourceStream = response.getResource()
                    .getInputStream();
        }

        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            try {
                while (resourceStream.read(buffer, 0, buffer.length) != -1) {
                    if (Thread.interrupted()) {
                        return;
                    }
                }
                resourceStream.close();
            } catch (IOException e) {
                LOGGER.debug("Error reading resource input stream.", e);
            }
        }
    }
}
