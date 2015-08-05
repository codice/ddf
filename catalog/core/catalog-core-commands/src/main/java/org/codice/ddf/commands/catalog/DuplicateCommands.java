/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;

public abstract class DuplicateCommands extends CatalogCommands {

    protected static final int MAX_BATCH_SIZE = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateCommands.class);

    private static final String DATE_FORMAT = "MM-dd-yyyy";

    protected FilterBuilder builder;

    protected AtomicInteger failedCount = new AtomicInteger(0);

    @Option(name = "--batchsize", required = false, aliases = {
            "-b"}, multiValued = false, description = "Number of Metacards to ingest at a time. Change this argument based on system memory and catalog provider limits.")
    int batchSize = MAX_BATCH_SIZE;

    @Option(name = "--multithreaded", required = false, aliases = {
            "-m"}, multiValued = false, description = "Number of threads to use when ingesting. Setting this value too high for your system can cause performance degradation.")
    int multithreaded = 1;

    @Option(name = "-t", required = false, aliases = {
            "-temporal"}, multiValued = false, description = "Flag to use temporal criteria to query federated source. The default is to use \"keyword like * \"")
    boolean isUseTemporal = false;

    @Option(name = "-s", required = false, aliases = {
            "-startDate"}, multiValued = false, description = "Flag to specify a start date range to query with. Dates should be formatted as MM-dd-yyyy such as 06-10-2014.")
    String startDate;

    @Option(name = "-e", required = false, aliases = {
            "-endDate"}, multiValued = false, description = "Flag to specify a start date range to query with. Dates should be formatted as MM-dd-yyyy such as 06-10-2014.")
    String endDate;

    @Option(name = "-lasthours", required = false, aliases = {
            "-hour"}, multiValued = false, description = "Option to replicate the last N hours.")
    int lastHours;

    @Option(name = "-lastdays", required = false, aliases = {
            "-day"}, multiValued = false, description = "Option to replicate the last N days.")
    int lastDays;

    @Option(name = "-lastweeks", required = false, aliases = {
            "-week"}, multiValued = false, description = "Option to replicate the last N weeks.")
    int lastWeeks;

    @Option(name = "-lastmonths", required = false, aliases = {
            "-month"}, multiValued = false, description = "Option to replicate the last N month.")
    int lastMonths;

    @Option(name = "--failedDir", required = false, aliases = {
            "-f"}, multiValued = false, description = "Option to specify where to write metacards that failed to ingest.")
    String failedDir;

    @Option(name = "--cql", required = false, aliases = {}, multiValued = false, description =
            "Remove Metacards that match a CQL Filter expressions. It is recommended to use the search command first to see which metacards will be removed.\n"
                    + "CQL Examples:\n" + "\tTextual:   search --cql \"title like 'some text'\"\n"
                    + "\tTemporal:  search --cql \"modified before 2012-09-01T12:30:00Z\"\n"
                    + "\tSpatial:   search --cql \"DWITHIN(location, POINT (1 2) , 10, kilometers)\"\n"
                    + "\tComplex:   search --cql \"title like 'some text' AND modified before 2012-09-01T12:30:00Z\"")
    String cqlFilter = null;

    private List<Metacard> failedMetacards = Collections
            .synchronizedList(new ArrayList<Metacard>());

    abstract List<Metacard> query(CatalogFacade facade, int startIndex, Filter filter);

    /**
     *
     * @param queryFacade
     *            - the CatalogFacade used for query
     * @param ingestFacade
     *            - the CatalogFacade used for ingest
     * @param startIndex
     *            - the start index of the query
     * @param filter
     *            - the filter to query with
     * @return - the number of successfully created metacards.
     */
    protected int queryAndIngest(CatalogFacade queryFacade, CatalogFacade ingestFacade,
            int startIndex, Filter filter) {
        List<Metacard> queryMetacards = null;
        queryMetacards = query(queryFacade, startIndex, filter);

        if (queryMetacards == null || queryMetacards.isEmpty()) {
            return 0;
        }

        List<Metacard> createdMetacards = ingestMetacards(ingestFacade, queryMetacards);
        int failed = queryMetacards.size() - createdMetacards.size();
        if (failed != 0) {
            LOGGER.warn("Not all records were ingested. [{}] failed", failed);
            failedCount.addAndGet(failed);
            failedMetacards.addAll(subtract(queryMetacards, createdMetacards));
        }
        return createdMetacards.size();
    }

    protected List<Metacard> ingestMetacards(CatalogFacade provider, List<Metacard> metacards) {
        if (metacards.isEmpty()) {
            return Collections.emptyList();
        }
        List<Metacard> createdMetacards = new ArrayList<Metacard>();
        LOGGER.debug("Preparing to ingest {} records", metacards.size());
        CreateRequest createRequest = new CreateRequestImpl(metacards);

        CreateResponse createResponse = null;
        try {
            createResponse = provider.create(createRequest);
            createdMetacards = createResponse.getCreatedMetacards();
        } catch (IngestException e) {
            printErrorMessage(
                    String.format("Received error while ingesting: %s%n", e.getMessage()));
            LOGGER.warn("Error during ingest. Attempting to ingest batch individually.");
            return ingestSingly(provider, metacards);
        } catch (SourceUnavailableException e) {
            printErrorMessage(
                    String.format("Received error while ingesting: %s%n", e.getMessage()));
            LOGGER.warn("Error during ingest:", e);
            return createdMetacards;
        } catch (Exception e) {
            printErrorMessage(String.format("Unexpected Exception received while ingesting: %s%n",
                    e.getMessage()));
            LOGGER.warn("Unexpected Exception during ingest:", e);
            return createdMetacards;
        }

        return createdMetacards;
    }

    private List<Metacard> ingestSingly(CatalogFacade provider, List<Metacard> metacards) {
        if (metacards.isEmpty()) {
            return Collections.emptyList();
        }
        List<Metacard> createdMetacards = new ArrayList<Metacard>();
        LOGGER.debug("Preparing to ingest {} records one at time.", metacards.size());
        for (Metacard metacard : metacards) {
            CreateRequest createRequest = new CreateRequestImpl(Arrays.asList(metacard));

            CreateResponse createResponse = null;
            try {
                createResponse = provider.create(createRequest);
                createdMetacards.addAll(createResponse.getCreatedMetacards());
            } catch (IngestException | SourceUnavailableException e) {
                LOGGER.warn("Error during ingest:", e);
            } catch (Exception e) {
                LOGGER.warn("Unexpected Exception during ingest:", e);
            }
        }
        return createdMetacards;
    }

    protected Filter getFilter(long start, long end, String temporalProperty)
            throws InterruptedException, ParseException {
        if (builder == null) {
            builder = getFilterBuilder();
        }

        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
            return builder.attribute(temporalProperty).is().during()
                    .dates(formatter.parse(startDate), formatter.parse(endDate));
        } else if (start > 0 && end > 0) {
            return builder.attribute(temporalProperty).is().during()
                    .dates(new Date(start), new Date(end));
        } else if (isUseTemporal) {
            return builder.attribute(temporalProperty).is().during().last(start);
        } else {
            return builder.attribute(Metacard.ANY_TEXT).is().like().text(WILDCARD);
        }
    }

    protected long getFilterStartTime(long now) {
        long startTime = 0;
        if (lastHours > 0) {
            startTime = now - TimeUnit.HOURS.toMillis(lastHours);
        } else if (lastDays > 0) {
            startTime = now - TimeUnit.DAYS.toMillis(lastDays);
        } else if (lastWeeks > 0) {
            Calendar weeks = GregorianCalendar.getInstance();
            weeks.setTimeInMillis(now);
            weeks.add(Calendar.WEEK_OF_YEAR, -1 * lastWeeks);
            startTime = weeks.getTimeInMillis();
        } else if (lastMonths > 0) {
            Calendar months = GregorianCalendar.getInstance();
            months.setTimeInMillis(now);
            months.add(Calendar.MONTH, -1 * lastMonths);
            startTime = months.getTimeInMillis();
        }
        return startTime;
    }

    protected String getInput(String message) throws IOException {
        StringBuilder buffer = new StringBuilder();
        console.print(String.format(message));
        console.flush();
        while (true) {
            int byteOfData = session.getKeyboard().read();

            if (byteOfData < 0) {
                // end of stream
                return null;
            }
            console.print((char) byteOfData);
            if (byteOfData == '\r' || byteOfData == '\n') {
                break;
            }
            buffer.append((char) byteOfData);
        }
        return buffer.toString();
    }

    protected List<Metacard> subtract(List<Metacard> queried, List<Metacard> ingested) {
        List<Metacard> result = new ArrayList<Metacard>(queried);
        result.removeAll(ingested);
        return result;
    }

    protected void writeFailedMetacards(List<Metacard> failedMetacards)
            throws FileNotFoundException, IOException {
        File directory = new File(failedDir);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                printErrorMessage(
                        "Unable to create directory [" + directory.getAbsolutePath() + "].");
                return;
            }
        }

        if (!directory.canWrite()) {
            printErrorMessage("Directory [" + directory.getAbsolutePath() + "] is not writable.");
            return;
        }
        for (Metacard metacard : failedMetacards) {

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                    new File(directory.getAbsolutePath(), metacard.getId())))) {
                oos.writeObject(new MetacardImpl(metacard));
                oos.flush();
            }
        }
    }

}
