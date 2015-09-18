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
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;


/**
 * Command used to remove all or a subset of records (in bulk) from the Catalog.
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "removeall", description = "Attempts to delete all records from the catalog.")
public class RemoveAllCommand extends CatalogCommands {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemoveAllCommand.class);

    static final int PAGE_SIZE_LOWER_LIMIT = 1;

    static final long UNKNOWN_AMOUNT = -1;

    static final String PROGRESS_FORMAT = " Currently %1$s record(s) removed out of %2$s \r";

    static final String BATCH_SIZE_ERROR_MESSAGE_FORMAT = "Improper batch size [%1$s]. For help with usage: removeall --help";

    static final String WARNING_MESSAGE_FORMAT_CATALOG_REMOVAL = "WARNING: This will permanently remove all %1$s"
            + "records from the Catalog. Do you want to proceed? (yes/no): ";

    static final String WARNING_MESSAGE_FORMAT_CACHE_REMOVAL = "WARNING: This will permanently remove all %1$s"
            + "records from the cache. Do you want to proceed? (yes/no): ";

    private static final int DEFAULT_BATCH_SIZE = 100;

    @Argument(name = "Batch size", description =
            "Number of Metacards to delete at a time until completion. Change this argument based on system memory and Catalog limits. "
                    + "Must be a positive integer.", index = 0, multiValued = false, required = false)
    int batchSize = DEFAULT_BATCH_SIZE;

    @Option(name = "-e", required = false, aliases = {
            "--expired"}, multiValued = false, description =
            "Remove only expired records from the Catalog. "
                    + "Expired records are based on the Metacard EXPIRATION field.")
    boolean expired = false;

    @Option(name = "-f", required = false, aliases = {
            "--force"}, multiValued = false, description = "Force the removal without a confirmation message.")
    boolean force = false;

    @Option(name = "--cache", required = false, multiValued = false, description = "Only remove cached entries.")
    boolean cache = false;

    @Override
    protected Object executeWithSubject() throws Exception {

        if (batchSize < PAGE_SIZE_LOWER_LIMIT) {
            printErrorMessage(String.format(BATCH_SIZE_ERROR_MESSAGE_FORMAT, batchSize));
            return null;
        }

        if (isAccidentalRemoval(console)) {
            return null;
        }

        if (this.cache) {
            return executeRemoveAllFromCache();

        } else {
            return executeRemoveAllFromStore();
        }

    }

    private Object executeRemoveAllFromCache() throws Exception {

        long start = System.currentTimeMillis();

        getCacheProxy().removeAll();

        long end = System.currentTimeMillis();

        String info = String.format("Cache cleared in %3.3f seconds%n",
                (end - start) / MS_PER_SECOND);

        LOGGER.info(info);
        LOGGER.info("Cache cleared by catalog:removeAll with --cache option");

        console.println();
        console.print(info);

        return null;
    }

    private Object executeRemoveAllFromStore() throws Exception {
        CatalogFacade catalog = this.getCatalog();

        FilterBuilder filterBuilder = getFilterBuilder();

        QueryRequest firstQuery = getIntendedQuery(filterBuilder, true);
        QueryRequest subsequentQuery = getIntendedQuery(filterBuilder, false);

        long totalAmountDeleted = 0;
        long start = System.currentTimeMillis();

        SourceResponse response = null;
        try {
            response = catalog.query(firstQuery);
        } catch (UnsupportedQueryException e) {
            firstQuery = getAlternateQuery(filterBuilder, true);
            subsequentQuery = getAlternateQuery(filterBuilder, false);

            response = catalog.query(firstQuery);
        }

        if (response == null) {
            printErrorMessage("No response from Catalog.");
            return null;
        }

        if (needsAlternateQueryAndResponse(response)) {
            firstQuery = getAlternateQuery(filterBuilder, true);
            subsequentQuery = getAlternateQuery(filterBuilder, false);

            response = catalog.query(firstQuery);
        }

        String totalAmount = getTotalAmount(response.getHits());

        while (response.getResults().size() > 0) {

            List<String> ids = new ArrayList<String>();

            // Add metacard ids to string array
            for (Result result : response.getResults()) {
                if (result != null && result.getMetacard() != null) {
                    Metacard metacard = result.getMetacard();
                    ids.add(metacard.getId());
                }

            }

            // Delete the records
            DeleteRequestImpl request = new DeleteRequestImpl(ids.toArray(new String[ids.size()]));

            DeleteResponse deleteResponse = catalog.delete(request);

            int amountDeleted = deleteResponse.getDeletedMetacards().size();

            totalAmountDeleted += amountDeleted;
            console.print(String.format(PROGRESS_FORMAT, totalAmountDeleted, totalAmount));
            console.flush();

            // Break out if there are no more records to delete
            if (amountDeleted < batchSize || batchSize < 1) {
                break;
            }

            // Re-query when necessary
            response = catalog.query(subsequentQuery);
        }

        long end = System.currentTimeMillis();

        String info = String.format(" %d file(s) removed in %3.3f seconds%n", totalAmountDeleted,
                (end - start) / MS_PER_SECOND);

        LOGGER.info(info);
        LOGGER.info(totalAmountDeleted + " files removed using cache:removeAll command");

        console.println();
        console.print(info);

        return null;
    }

    private boolean needsAlternateQueryAndResponse(SourceResponse response) {

        Set<ProcessingDetails> processingDetails = (Set<ProcessingDetails>) response
                .getProcessingDetails();

        if (processingDetails == null || processingDetails.iterator() == null) {
            return false;
        }

        Iterator<ProcessingDetails> iterator = processingDetails.iterator();

        while (iterator.hasNext()) {

            ProcessingDetails next = iterator.next();
            if (next != null && next.getException() != null
                    && next.getException().getMessage() != null && next.getException().getMessage()
                    .contains(UnsupportedQueryException.class.getSimpleName())) {
                return true;
            }

        }

        return false;
    }

    boolean isAccidentalRemoval(PrintStream console) throws IOException {
        if (!force) {
            StringBuffer buffer = new StringBuffer();

            //use a message specific to whether they
            //are removing from cache or the catalog
            String warning = (this.cache ? WARNING_MESSAGE_FORMAT_CACHE_REMOVAL :
                                           WARNING_MESSAGE_FORMAT_CATALOG_REMOVAL);
            System.err.println(String.format(warning, (expired ? "expired " : "")));
            System.err.flush();
            while (true) {
                int byteOfData = session.getKeyboard().read();

                if (byteOfData < 0) {
                    // end of stream
                    return true;
                }
                System.err.print((char) byteOfData);
                if (byteOfData == '\r' || byteOfData == '\n') {
                    break;
                }
                buffer.append((char) byteOfData);
            }
            String str = buffer.toString();
            if (!str.equals("yes")) {
                console.println("No action taken.");
                return true;
            }
        }

        return false;
    }

    private String getTotalAmount(long hits) {

        if (hits <= UNKNOWN_AMOUNT) {
            return "UNKNOWN";
        }

        return Long.toString(hits);
    }

    private QueryRequest getIntendedQuery(FilterBuilder filterBuilder, boolean isRequestForTotal)
            throws InterruptedException {

        Filter filter = filterBuilder.attribute(Metacard.ID).is().like().text(WILDCARD);

        if (expired) {
            filter = filterBuilder.attribute(Metacard.EXPIRATION).before().date(new Date());
        }

        QueryImpl query = new QueryImpl(filter);

        query.setRequestsTotalResultsCount(isRequestForTotal);

        query.setPageSize(batchSize);

        Map<String, Serializable> properties = new HashMap<>();
        properties.put("mode", "native");

        return new QueryRequestImpl(query, properties);
    }

    private QueryRequest getAlternateQuery(FilterBuilder filterBuilder, boolean isRequestForTotal)
            throws InterruptedException {

        Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(WILDCARD);

        if (expired) {
            DateTime twoThousandYearsAgo = new DateTime().minusYears(2000);

            // less accurate than a Before filter, this is only used for those
            // Sources who cannot understand the Before filter.
            filter = filterBuilder.attribute(Metacard.EXPIRATION).during()
                    .dates(twoThousandYearsAgo.toDate(), new Date());
        }

        QueryImpl query = new QueryImpl(filter);

        query.setRequestsTotalResultsCount(isRequestForTotal);

        query.setPageSize(batchSize);

        Map<String, Serializable> properties = new HashMap<>();
        properties.put("mode", "native");

        return new QueryRequestImpl(query, properties);
    }

}
