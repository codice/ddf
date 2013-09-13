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
package com.lmco.ddf.commands.catalog;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;

import com.lmco.ddf.commands.catalog.facade.CatalogFacade;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.DeleteRequestImpl;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.SourceProcessingDetails;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Command used to remove all or a subset of records (in bulk) from the Catalog.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "removeall", description = "Attempts to delete all records from the catalog.")
public class RemoveAllCommand extends CatalogCommands {

    private static final int DEFAULT_BATCH_SIZE = 100;

    static final int PAGE_SIZE_LOWER_LIMIT = 1;

    static final long UNKNOWN_AMOUNT = -1;

    static final String PROGRESS_FORMAT = " Currently %1$s record(s) removed out of %2$s \r";

    static final String BATCH_SIZE_ERROR_MESSAGE_FORMAT = "Improper batch size [%1$s]. For help with usage: removeall --help";

    static final String WARNING_MESSAGE_FORMAT = "WARNING: This will permanently remove all %1$s"
            + "records from the Catalog. Do you want to proceed? (yes/no): ";

    @Argument(name = "Batch size", description = "Number of Metacards to delete at a time until completion. Change this argument based on system memory and Catalog limits. "
            + "Must be a positive integer.", index = 0, multiValued = false, required = false)
    int batchSize = DEFAULT_BATCH_SIZE;

    @Option(name = "-e", required = false, aliases = {"--expired"}, multiValued = false, description = "Remove only expired records from the Catalog. "
            + "Expired records are based on the Metacard EXPIRATION field.")
    boolean expired = false;

    @Option(name = "-f", required = false, aliases = {"--force"}, multiValued = false, description = "Force the removal without a confirmation message.")
    boolean force = false;

    @Override
    protected Object doExecute() throws Exception {

        PrintStream console = System.out;

        if (batchSize < PAGE_SIZE_LOWER_LIMIT) {
            printColor(console, Ansi.Color.RED,
                    String.format(BATCH_SIZE_ERROR_MESSAGE_FORMAT, batchSize));

            return null;
        }

        CatalogFacade catalog = this.getCatalog();

        if (isAccidentalRemoval(console)) {
            return null;
        }

        FilterBuilder filterBuilder = getFilterBuilder();

        QueryRequest firstQuery = getIntendedQuery(filterBuilder, batchSize, expired, true);
        QueryRequest subsequentQuery = getIntendedQuery(filterBuilder, batchSize, expired, false);

        long totalAmountDeleted = 0;
        long start = System.currentTimeMillis();

        SourceResponse response = null;
        try {
            response = catalog.query(firstQuery);
        } catch (UnsupportedQueryException e) {
            firstQuery = getAlternateQuery(filterBuilder, batchSize, expired, true);
            subsequentQuery = getAlternateQuery(filterBuilder, batchSize, expired, false);

            response = catalog.query(firstQuery);
        }

        if (response == null) {
            printColor(console, Ansi.Color.RED, "No response from Catalog.");
            return null;
        }

        if (needsAlternateQueryAndResponse(response)) {
            firstQuery = getAlternateQuery(filterBuilder, batchSize, expired, true);
            subsequentQuery = getAlternateQuery(filterBuilder, batchSize, expired, false);

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

        console.println();

        console.printf(" %d file(s) removed in %3.3f seconds%n", totalAmountDeleted, (end - start)
                / MILLISECONDS_PER_SECOND);

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
            if (next != null
                    && next.getException() != null
                    && next.getException().getMessage() != null
                    && next.getException().getMessage()
                            .contains(UnsupportedQueryException.class.getSimpleName())) {
                return true;
            }

        }

        return false;
    }

    boolean isAccidentalRemoval(PrintStream console) throws IOException {
        if (!force) {
            StringBuffer buffer = new StringBuffer();
            System.err.println(String.format(WARNING_MESSAGE_FORMAT, (expired ? "expired " : "")));
            System.err.flush();
            for (;;) {
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

    private QueryRequest getIntendedQuery(FilterBuilder filterBuilder, int batchSize,
            boolean isRequestForExpired, boolean isRequestForTotal) throws InterruptedException {

        Filter filter = filterBuilder.attribute(Metacard.ID).is().like().text("*");

        if (isRequestForExpired) {
            filter = filterBuilder.attribute(Metacard.EXPIRATION).before().date(new Date());
        }

        QueryImpl query = new QueryImpl(filter);

        query.setRequestsTotalResultsCount(isRequestForTotal);

        query.setPageSize(batchSize);

        return new QueryRequestImpl(query);
    }

    private QueryRequest getAlternateQuery(FilterBuilder filterBuilder, int batchSize,
            boolean isRequestForExpired, boolean isRequestForTotal) throws InterruptedException {

        Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text("*");

        if (isRequestForExpired) {
            DateTime twoThousandYearsAgo = new DateTime().minusYears(2000);

            // less accurate than a Before filter, this is only used for those
            // Sources who cannot understand the Before filter.
            filter = filterBuilder.attribute(Metacard.EXPIRATION).during()
                    .dates(twoThousandYearsAgo.toDate(), new Date());
        }

        QueryImpl query = new QueryImpl(filter);

        query.setRequestsTotalResultsCount(isRequestForTotal);

        query.setPageSize(batchSize);

        return new QueryRequestImpl(query);
    }

}
