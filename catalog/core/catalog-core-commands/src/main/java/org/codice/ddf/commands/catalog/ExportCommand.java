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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.transformer.zip.JarSigner;
import org.codice.ddf.commands.catalog.export.ExportItem;
import org.codice.ddf.commands.catalog.export.IdAndUriMetacard;
import org.codice.ddf.commands.util.CatalogCommandRuntimeException;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.impl.DeleteStorageRequestImpl;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.security.SubjectUtils;
import ddf.security.common.audit.SecurityLogger;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

/**
 * Experimental.
 */
@Service
@Command(scope = CatalogCommands.NAMESPACE, name = "export", description = "Exports Metacards and history from the current Catalog")
public class ExportCommand extends CqlCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportCommand.class);

    private static final SimpleDateFormat ISO_8601_DATE_FORMAT;

    static {
        ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSS'Z'");
        ISO_8601_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final Supplier<String> name =
            () -> "export-" + ISO_8601_DATE_FORMAT.format(Date.from(Instant.now())) + ".zip";

    private static MetacardTransformer transformer;

    private final int PAGE_SIZE = 64;

    private Filter revisionFilter;

    @Reference
    private JarSigner jarSigner;

    @Reference
    private StorageProvider storageProvider;

    @Option(name = "--output", description = "Output file to export Metacards into. Paths are absolute and must be in quotes. Will default to auto generated name inside of ddf.home", multiValued = false, required = false, aliases = {
            "-o"})
    String output = Paths.get(System.getProperty("ddf.home"), name.get())
            .toString();

    @Option(name = "--delete", required = true, aliases = {"-d",
            "delete"}, multiValued = false, description = "Whether or not to delete metacards after export")
    boolean delete = false;

    @Option(name = "--archived", required = false, aliases = {"-a",
            "archived"}, multiValued = false, description = "Equivalent to --cql \"\\\"metacard-tags\\\" like 'deleted'\"")
    boolean archived = false;

    @Override
    protected Object executeWithSubject() throws Exception {
        Filter filter = getFilter();
        transformer = getTransformers(DEFAULT_TRANSFORMER_ID).stream()
                .findFirst()
                .orElseThrow(() -> new CatalogCommandRuntimeException(
                        "Could not find transformer " + DEFAULT_TRANSFORMER_ID));
        revisionFilter = initRevisionFilter();

        final File outputFile = initOutputFile(output);
        if (outputFile.exists()) {
            printErrorMessage(String.format("File [%s] already exists!", outputFile.getPath()));
            return null;
        }

        final File parentDirectory = outputFile.getParentFile();
        if (parentDirectory == null || !parentDirectory.isDirectory()) {
            printErrorMessage(String.format("Directory [%s] must exist.", output));
            console.println("If the directory does indeed exist, try putting the path in quotes.");
            return null;
        }

        String filename = FilenameUtils.getName(outputFile.getPath());
        if (StringUtils.isBlank(filename) || !filename.endsWith(".zip")) {
            console.println("Filename must end with '.zip' and not be blank");
            return null;
        }

        SecurityLogger.audit("Called catalog:dump command with path : {}\nCurrent user: {}",
                output,
                SubjectUtils.getName(SecurityUtils.getSubject()));

        // TODO (RCZ) - Warn if breaking associations
        ZipFile zipFile = new ZipFile(outputFile);

        console.println("Starting metacard export...");
        Instant start = Instant.now();
        List<ExportItem> exportedItems = doMetacardExport(zipFile, filter);
        console.println("Metacards exported in in: " + Duration.between(start, Instant.now()));
        console.println("Number of metacards exported: " + exportedItems.size());
        console.println();

        console.println("Starting content export...");
        start = Instant.now();
        List<ExportItem> exportedContentItems = doContentExport(zipFile, exportedItems);
        console.println("Content exported in: " + Duration.between(start, Instant.now()));
        console.println("Number of content exported: " + exportedContentItems.size());

        console.println();

        if (delete) {
            doDelete(exportedItems, exportedContentItems);
        }

        console.println("Signing zip file...");
        jarSigner.signJar(zipFile.getFile(),
                System.getProperty("org.codice.ddf.system.hostname"),
                System.getProperty("javax.net.ssl.keyStorePassword"),
                System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword"));
        console.println("zip file signed.");

        return null;
    }

    private File initOutputFile(String output) {
        String resolvedOutput;
        File initialOutputFile = new File(output);
        if (initialOutputFile.isDirectory()) {
            // If directory was specified, auto generate file name
            resolvedOutput = Paths.get(initialOutputFile.getPath(), name.get())
                    .toString();
        } else {
            resolvedOutput = output;
        }

        return new File(resolvedOutput);
    }

    private List<ExportItem> doMetacardExport(ZipFile zipFile, Filter filter) {
        List<ExportItem> exportedItems = new ArrayList<>(1024);
        for (Result result : new QueryResulterable(catalogFramework,
                (i) -> getQuery(filter, i, PAGE_SIZE),
                PAGE_SIZE)) {
            writeToZip(zipFile, result);
            exportedItems.add(new ExportItem(result.getMetacard()
                    .getId(),
                    getTag(result),
                    result.getMetacard()
                            .getResourceURI()));
            // Fetch and export all history for each exported item
            for (Result revision : new QueryResulterable(catalogFramework,
                    (i) -> getQuery(getHistoryFilter(result), i, PAGE_SIZE),
                    PAGE_SIZE)) {
                writeToZip(zipFile, revision);
                exportedItems.add(new ExportItem(revision.getMetacard()
                        .getId(),
                        getTag(revision),
                        revision.getMetacard()
                                .getResourceURI()));
            }
        }
        return exportedItems;
    }

    private List<ExportItem> doContentExport(/*Mutable,IO*/ZipFile zipFile,
            List<ExportItem> exportedItems) throws ZipException {
        List<ExportItem> contentItemsToExport = exportedItems.stream()
                // Only things with a resource URI
                .filter(ei -> ei.getResourceURI() != null)
                // Only our content scheme
                .filter(ei -> ei.getResourceURI()
                        .getScheme()
                        .startsWith(ContentItem.CONTENT_SCHEME))
                // Deleted Metacards have no content associated
                .filter(ei -> !ei.getMetacardTag()
                        .equals("deleted"))
                // for revision metacards, only those that have their own content
                .filter(ei -> !ei.getMetacardTag()
                        .equals("revision") || ei.getResourceURI()
                        .getSchemeSpecificPart()
                        .equals(ei.getId()))
                .filter(distinctByKey(ei -> ei.getResourceURI()
                        .getSchemeSpecificPart()))
                .collect(Collectors.toList());

        List<ExportItem> exportedContentItems = new ArrayList<>(1024);
        for (ExportItem contentItem : contentItemsToExport) {
            ResourceResponse resource;
            try {
                resource =
                        catalogFramework.getLocalResource(new ResourceRequestById(contentItem.getId()));
            } catch (IOException | ResourceNotSupportedException e) {
                throw new CatalogCommandRuntimeException(
                        "Something went wrong while fetching resources",
                        e);
            } catch (ResourceNotFoundException e) {
                continue;
            }
            writeToZip(zipFile, contentItem, resource);
            exportedContentItems.add(contentItem);
        }
        return exportedContentItems;
    }

    private void doDelete(List<ExportItem> exportedItems, List<ExportItem> exportedContentItems) {
        Instant start;
        console.println("Starting delete");
        start = Instant.now();
        for (ExportItem exported : exportedItems) {
            try {
                catalogProvider.delete(new DeleteRequestImpl(exported.getId()));
            } catch (IngestException e) {
                printErrorMessage("Could not delete metacard: " + exported.toString());
            }
        }

        for (ExportItem exportedContentItem : exportedContentItems) {
            try {
                storageProvider.delete(new DeleteStorageRequestImpl(Collections.singletonList(new IdAndUriMetacard(
                        exportedContentItem.getId(),
                        exportedContentItem.getResourceURI())), Collections.emptyMap()));
            } catch (StorageException e) {
                printErrorMessage(
                        "Could not content for metacard: " + exportedContentItem.toString());
            }
        }
        console.println(
                "Metacards and Content deleted in: " + Duration.between(start, Instant.now()));
        console.println("Number of content deleted: " + exportedItems.size());
        console.println("Number of metacards deleted: " + exportedContentItems.size());
    }

    private void writeToZip(/*Mutable,IO*/ ZipFile zipFile, ExportItem exportItem,
            ResourceResponse resource) throws ZipException {
        ZipParameters parameters = new ZipParameters();
        parameters.setSourceExternalStream(true);
        String id = exportItem.getId();
        parameters.setFileNameInZip(Paths.get("metacards",
                id.substring(0, 3),
                id,
                resource.getResource()
                        .getName())
                .toString());
        zipFile.addStream(resource.getResource()
                .getInputStream(), parameters);
    }

    private void writeToZip(/*Mutable,IO*/ ZipFile zipFile, Result result) {
        ZipParameters parameters = new ZipParameters();
        parameters.setSourceExternalStream(true);
        String id = result.getMetacard()
                .getId();
        parameters.setFileNameInZip(Paths.get("metacards", id.substring(0, 3), id, id + ".xml")
                .toString());

        try {
            BinaryContent binaryMetacard = transformer.transform(result.getMetacard(),
                    Collections.emptyMap());
            zipFile.addStream(binaryMetacard.getInputStream(), parameters);
        } catch (CatalogTransformerException | ZipException e) {
            LOGGER.error("Error processing result and adding to ZIP", e);
            throw new CatalogCommandRuntimeException(e);
        }
    }

    /**
     * Generates stateful predicate to filter distinct elements by a certain key in the object.
     *
     * @param keyExtractor Function to pull the desired key out of the object
     * @return the stateful predicate
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private String getTag(Result r) {
        Set<String> tags = r.getMetacard()
                .getTags();
        if (tags.contains("deleted")) {
            return "deleted";
        } else if (tags.contains("revision")) {
            return "revision";
        } else {
            return "nonhistory";
        }
    }

    private Filter initRevisionFilter() {
        return filterBuilder.attribute(Metacard.TAGS)
                .is()
                .like()
                .text("revision");
    }

    private Filter getHistoryFilter(Result result) {
        String id;
        String typeName = result.getMetacard()
                .getMetacardType()
                .getName();
        switch (typeName) {
        case "deleted":
            id = String.valueOf(result.getMetacard()
                    .getAttribute("metacard.deleted.id")
                    .getValue());
            break;
        case "revision":
            return null;
        default:
            id = result.getMetacard()
                    .getId();
            break;
        }

        return filterBuilder.allOf(revisionFilter,
                filterBuilder.attribute("metacard.version.id")
                        .is()
                        .equalTo()
                        .text(id));

    }

    protected Filter getFilter() throws InterruptedException, ParseException, CQLException {
        Filter filter = super.getFilter();
        if (archived) {
            filter = filterBuilder.allOf(filter,
                    filterBuilder.attribute(Metacard.TAGS)
                            .is()
                            .like()
                            .text("deleted"));
        }
        return filter;
    }

    @SuppressWarnings("unchecked")
    private List<MetacardTransformer> getTransformers(String transformerId) {
        ServiceReference<MetacardTransformer>[] refs;
        try {
            refs = (ServiceReference<MetacardTransformer>[]) bundleContext.getAllServiceReferences(
                    MetacardTransformer.class.getName(),
                    String.format("(|(%s=%s))", Constants.SERVICE_ID, transformerId));

        } catch (InvalidSyntaxException e) {
            console.printf("Failed to get MetacardTransformer references due to %s",
                    e.getMessage());
            throw new RuntimeException(e);
        }

        if (refs == null || refs.length == 0) {
            throw new RuntimeException("Could not find specified transformer!");
        }

        return Arrays.stream(refs)
                .map(bundleContext::getService)
                .filter(MetacardTransformer.class::isInstance)
                .map(MetacardTransformer.class::cast)
                .collect(Collectors.toList());
    }

    private QueryRequestImpl getQuery(Filter filter, int index, int pageSize) {
        return new QueryRequestImpl(new QueryImpl(filter,
                index,
                pageSize,
                SortBy.NATURAL_ORDER,
                false,
                TimeUnit.MINUTES.toMillis(1)), new HashMap<>());
    }

    private And getIdsFilter(List<String> ids, int pageSize, Integer index) {
        return filterBuilder.allOf(filterBuilder.attribute(Metacard.TAGS)
                        .is()
                        .like()
                        .text("*"),
                filterBuilder.anyOf(ids.stream()
                        .skip(Math.max(0, index - 1))
                        .limit(pageSize)
                        .map(id -> filterBuilder.attribute(Metacard.ID)
                                .is()
                                .equalTo()
                                .text(id))
                        .collect(Collectors.toList())));
    }

    /**
     * Effectively a cursor over the results of a filter that automatically pages through all results
     */
    class QueryResulterable implements Iterable<Result> {
        private final CatalogFramework catalog;

        private final Function<Integer, QueryRequest> filter;

        private final int pageSize;

        /**
         * For paging through a single filter with a default pageSize of 64
         *
         * @param catalog catalog to query
         * @param filter  The filter to query with
         */
        public QueryResulterable(CatalogFramework catalog, Function<Integer, QueryRequest> filter) {
            this(catalog, filter, 64);
        }

        /**
         * For paging through a single filter.
         *
         * @param catalog  catalog to query
         * @param filter   The filter to query with
         * @param pageSize How many results should each page hold
         */
        public QueryResulterable(CatalogFramework catalog, Function<Integer, QueryRequest> filter,
                int pageSize) {
            this.catalog = catalog;
            this.filter = filter;
            this.pageSize = pageSize;
        }

        //        /**
        //         * For efficient batching and iterating by a given list of metacard ids.
        //         *
        //         * @param catalog  catalog to query
        //         * @param ids      List of ids to query for and iterate through
        //         * @param pageSize How many results should each page hold
        //         */
        //        QueryResulterable(CatalogFramework catalog, Function<Integer, QueryRequestImpl> filter, int pageSize) {
        //            this.catalog = catalog;
        //            this.filter = (i) -> getQuery(getIdsFilter(ids, pageSize, i), 1, pageSize + 1);
        //            this.pageSize = pageSize;
        //        }
        @Override
        public Iterator<Result> iterator() {
            return new ResultQueryIterator();
        }

        @Override
        public Spliterator<Result> spliterator() {
            int characteristics = Spliterator.DISTINCT;
            return Spliterators.spliteratorUnknownSize(this.iterator(), characteristics);
        }

        public Stream<Result> stream() {
            return StreamSupport.stream(this.spliterator(), false);
        }

        class ResultQueryIterator implements Iterator<Result> {
            private int pageIndex = 1;

            private boolean finished = false;

            private SourceResponse response = null;

            private Iterator<Result> results = null;

            ResultQueryIterator() {
                if (pageSize <= 0) {
                    this.finished = true;
                }
            }

            @Override
            public boolean hasNext() {
                ensureInitialized();
                if (results.hasNext()) {
                    return true;
                }
                if (finished) {
                    return false;
                }

                pageIndex += pageSize;
                queryNext(pageIndex);
                return hasNext();
            }

            @Override
            public Result next() {
                ensureInitialized();
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return results.next();
            }

            private void queryNext(int index) {
                try {
                    Map<String, Serializable> props = new HashMap<>();
                    // Avoid caching all results while dumping with native query mode
                    props.put("mode", "native");
                    response = catalog.query(filter.apply(index));
                } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
                    throw new CatalogCommandRuntimeException(e);
                }
                List<Result> queryResults = response.getResults();
                this.results = queryResults.iterator();

                int size = queryResults.size();
                if (size == 0 || size < pageSize) {
                    finished = true;
                }
            }

            private void ensureInitialized() throws CatalogCommandRuntimeException {
                if (response != null || results != null) {
                    return;
                }
                queryNext(pageIndex);
            }
        }
    }
}
