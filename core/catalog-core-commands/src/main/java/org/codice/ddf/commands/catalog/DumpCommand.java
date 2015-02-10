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

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.geotools.filter.text.cql2.CQL;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Command(scope = CatalogCommands.NAMESPACE, name = "dump", description = "Exports Metacards from the current Catalog. Does not remove them.\n\tDate filters are ANDed together, and are exclusive for range.\n\tISO8601 format includes YYYY-MM-dd, YYYY-MM-ddTHH, YYYY-MM-ddTHH:mm, YYYY-MM-ddTHH:mm:ss, YYY-MM-ddTHH:mm:ss.sss, THH:mm:sss. See documentation for full syntax and examples.")
public class DumpCommand extends CatalogCommands {

    private static final Logger LOGGER = LoggerFactory.getLogger(DumpCommand.class);

    private final PeriodFormatter timeFormatter = new PeriodFormatterBuilder()
            .printZeroRarelyLast()
            .appendDays().appendSuffix(" day", " days").appendSeparator(" ")
            .appendHours().appendSuffix(" hour", " hours").appendSeparator(" ")
            .appendMinutes().appendSuffix(" minute", " minutes").appendSeparator(" ")
            .appendSeconds().appendSuffix(" second", " seconds")
            .toFormatter();

    private static List<MetacardTransformer> transformers = null;

    @Argument(name = "Dump directory path", description = "Directory to export Metacards into. Paths are absolute and must be in quotes.  Files in directory will be overwritten if they already exist.", index = 0, multiValued = false, required = true)
    String dirPath = null;

    @Argument(name = "Batch size", description = "Number of Metacards to retrieve and export at a time until completion. Change this argument based on system memory and CatalogProvider limits.", index = 1, multiValued = false, required = false)
    int pageSize = 1000;

    // DDF-535: remove "Transformer" alias in DDF 3.0
    @Option(name = "--transformer", required = false, aliases = {"-t",
            "Transformer"}, multiValued = false, description = "The metacard transformer ID to use to transform metacards into data files. The default metacard transformer is the Java serialization transformer.")
    String transformerId = DEFAULT_TRANSFORMER_ID;

    // DDF-535: remove "Extension" alias in DDF 3.0
    @Option(name = "--extension", required = false, aliases = {"-e",
            "Extension"}, multiValued = false, description = "The file extension of the data files.")
    String fileExtension = null;

    @Option(name = "--created-after", required = false, aliases = {
            "-ca"}, multiValued = false, description = "Include only entries created after this date/time (ISO8601 format).")
    String createdAfter = null;

    @Option(name = "--created-before", required = false, aliases = {
            "-cb"}, multiValued = false, description = "Include only entries created before this date/time (ISO8601 format).")
    String createdBefore = null;

    @Option(name = "--modified-after", required = false, aliases = {
            "-ma"}, multiValued = false, description = "Include only entries modified after this date/time (ISO8601 format).")
    String modifiedAfter = null;

    @Option(name = "--modified-before", required = false, aliases = {
            "-mb"}, multiValued = false, description = "Include only entries modified before this date/time (ISO8601 format)")
    String modifiedBefore = null;

    @Option(name = "--cql", required = false, aliases = {}, multiValued = false, description =
            "Search using CQL Filter expressions.\n"
                    + "CQL Examples:\n"
                    + "\tTextual:   search --cql \"title like 'some text'\"\n"
                    + "\tTemporal:  search --cql \"modified before 2012-09-01T12:30:00Z\"\n"
                    + "\tSpatial:   search --cql \"DWITHIN(location, POINT (1 2) , 10, kilometers)\"\n"
                    + "\tComplex:   search --cql \"title like 'some text' AND modified before 2012-09-01T12:30:00Z\"")
    String cqlFilter = null;

    @Option(name = "--multithreaded", required = false, aliases = {"-m", "Multithreaded"},
            multiValued = false, description = "Number of threads to use when dumping. Setting "
            + "this value too high for your system can cause performance degradation.")
    int multithreaded = 20;

    @Option(name = "--dirlevel", required = false, multiValued = false,
            description = "Number of subdirectory levels to create.  Two characters from the ID "
                    + "will be used to name each subdirectory level.")
    int dirLevel = 0;

    @Override
    protected Object doExecute() throws Exception {
        final File dumpDir = new File(dirPath);

        if (!dumpDir.exists()) {
            printErrorMessage("Directory [" + dirPath + "] must exist.");
            console.println("If the directory does indeed exist, try putting the path in quotes.");
            return null;
        }

        if (!dumpDir.isDirectory()) {
            printErrorMessage("Path [" + dirPath + "] must be a directory.");
            return null;
        }

        if (!DEFAULT_TRANSFORMER_ID.matches(transformerId)) {
            transformers = getTransformers();
            if (transformers == null) {
                console.println(transformerId + " is an invalid metacard transformer.");
                return null;
            }
        }

        CatalogFacade catalog = getCatalog();
        FilterBuilder builder = getFilterBuilder();

        Filter createdFilter = null;
        if ((createdAfter != null) && (createdBefore != null)) {
            DateTime createStartDateTime = DateTime.parse(createdAfter);
            DateTime createEndDateTime = DateTime.parse(createdBefore);
            createdFilter = builder.attribute(Metacard.CREATED).is().during()
                    .dates(createStartDateTime.toDate(), createEndDateTime.toDate());
        } else if (createdAfter != null) {
            DateTime createStartDateTime = DateTime.parse(createdAfter);
            createdFilter = builder.attribute(Metacard.CREATED).is().after()
                    .date(createStartDateTime.toDate());
        } else if (createdBefore != null) {
            DateTime createEndDateTime = DateTime.parse(createdBefore);
            createdFilter = builder.attribute(Metacard.CREATED).is().before()
                    .date(createEndDateTime.toDate());
        }

        Filter modifiedFilter = null;
        if ((modifiedAfter != null) && (modifiedBefore != null)) {
            DateTime modifiedStartDateTime = DateTime.parse(modifiedAfter);
            DateTime modifiedEndDateTime = DateTime.parse(modifiedBefore);
            modifiedFilter = builder.attribute(Metacard.MODIFIED).is().during()
                    .dates(modifiedStartDateTime.toDate(), modifiedEndDateTime.toDate());
        } else if (modifiedAfter != null) {
            DateTime modifiedStartDateTime = DateTime.parse(modifiedAfter);
            modifiedFilter = builder.attribute(Metacard.MODIFIED).is().after()
                    .date(modifiedStartDateTime.toDate());
        } else if (modifiedBefore != null) {
            DateTime modifiedEndDateTime = DateTime.parse(modifiedBefore);
            modifiedFilter = builder.attribute(Metacard.MODIFIED).is().before()
                    .date(modifiedEndDateTime.toDate());
        }

        Filter filter = null;
        if ((createdFilter != null) && (modifiedFilter != null)) {
            // Filter by both created and modified dates
            filter = builder.allOf(createdFilter, modifiedFilter);
        } else if (createdFilter != null) {
            // Only filter by created date
            filter = createdFilter;
        } else if (modifiedFilter != null) {
            // Only filter by modified date
            filter = modifiedFilter;
        } else {
            // Don't filter by date range
            filter = builder.attribute(Metacard.ID).is().like().text(WILDCARD);
        }

        if (cqlFilter != null) {
            filter = CQL.toFilter(cqlFilter);
        }
        
        QueryImpl query = new QueryImpl(filter);
        query.setRequestsTotalResultsCount(false);
        query.setPageSize(pageSize);

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        // Avoid caching all results while dumping with native query mode
        props.put("mode", "native");

        final AtomicLong resultCount = new AtomicLong(0);
        long start = System.currentTimeMillis();

        SourceResponse response = catalog.query(new QueryRequestImpl(query, props));

        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(
                multithreaded);
        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        final ExecutorService executorService = new ThreadPoolExecutor(multithreaded,
                multithreaded, 0L, TimeUnit.MILLISECONDS, blockingQueue, rejectedExecutionHandler);

        while (response.getResults().size() > 0) {
            response = catalog.query(new QueryRequestImpl(query, props));

            if (multithreaded > 1) {
                final List<Result> results = new ArrayList<Result>(response.getResults());
                executorService.submit(new Runnable() {
                    @Override public void run() {
                        for (final Result result : results) {
                            Metacard metacard = result.getMetacard();
                            try {
                                exportMetacard(dumpDir, metacard);
                            } catch (IOException | CatalogTransformerException e) {
                                executorService.shutdownNow();
                            }
                            printStatus(resultCount.incrementAndGet());
                        }
                    }
                });
            } else {
                for (final Result result : response.getResults()) {
                    Metacard metacard = result.getMetacard();
                    exportMetacard(dumpDir, metacard);
                    printStatus(resultCount.incrementAndGet());
                }
            }

            if (response.getResults().size() < pageSize || pageSize == -1) {
                break;
            }

            if (pageSize > 0) {
                query.setStartIndex(query.getStartIndex() + pageSize);
            }
        }

        executorService.shutdown();

        while (!executorService.isTerminated()) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        long end = System.currentTimeMillis();
        String elapsedTime = timeFormatter.print(new Period(start, end).withMillis(0));
        console.printf(" %d file(s) dumped in %s\t%n", resultCount.get(), elapsedTime);
        LOGGER.info("{} file(s) dumped in {}", resultCount.get(), elapsedTime);
        console.println();

        return null;
    }

    private void exportMetacard(File dumpLocation, Metacard metacard) throws IOException,
            CatalogTransformerException {

        if (DEFAULT_TRANSFORMER_ID.matches(transformerId)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getOutputFile(dumpLocation, metacard)))) {
                oos.writeObject(new MetacardImpl(metacard));
                oos.flush();
            }
        } else {

            BinaryContent binaryContent;
            try (FileOutputStream fos = new FileOutputStream(getOutputFile(dumpLocation, metacard))) {
                if (metacard != null) {
                    for (MetacardTransformer transformer : transformers) {
                        binaryContent = transformer.transform(metacard, null);
                        if (binaryContent != null) {
                            fos.write(binaryContent.getByteArray());
                            break;
                        }
                    }
                }
            }
        }
    }

    private File getOutputFile(File dumpLocation, Metacard metacard) throws IOException {
        String extension = "";
        if (fileExtension != null) {
            extension = "." + fileExtension;
        }

        String id = metacard.getId();
        File parent = dumpLocation;

        if (dirLevel > 0 && id.length() >= dirLevel * 2) {
            for (int i = 0; i < dirLevel; i++) {
                parent = new File(parent, id.substring(i*2, i*2+2));
            }
            FileUtils.forceMkdir(parent);
        }

        return new File(parent, id + extension);
    }

    protected void printStatus(long count) {
        console.print(String.format(" %d file(s) dumped\t\r", count));
        console.flush();
    }

    private List<MetacardTransformer> getTransformers() {

        BundleContext bundleContext = getBundleContext();
        ServiceReference[] refs = null;
        try {
            refs = bundleContext.getAllServiceReferences(MetacardTransformer.class.getName(), "(|"
                    + "(" + Constants.SERVICE_ID + "=" + transformerId + ")" + ")");

        } catch (InvalidSyntaxException e) {
            console.printf("Fail to get MetacardTransformer references. ", e);
        }
        if (refs == null || refs.length == 0) {
            return null;
        }

        List<MetacardTransformer> metacardTransformerList = new ArrayList<MetacardTransformer>();
        for (int i = 0; i < refs.length; i++) {

            metacardTransformerList.add((MetacardTransformer) bundleContext.getService(refs[i]));
        }

        return metacardTransformerList;
    }

    protected <T> T getService(Class<T> clazz) throws InterruptedException {
        ServiceTracker st = new ServiceTracker(getBundleContext(), clazz.getName(), null);
        st.open();

        @SuppressWarnings("unchecked")
        T service = (T) st.waitForService(ONE_SECOND);
        if (service == null) {
            throw new InterruptedException("Could not find a service for: " + clazz.getName());
        }
        st.close();

        return service;
    }

}
