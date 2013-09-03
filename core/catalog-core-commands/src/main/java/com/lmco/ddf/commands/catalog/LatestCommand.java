/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.commands.catalog;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.PrintStream;
import java.util.Date;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

import com.lmco.ddf.commands.catalog.facade.CatalogFacade;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.SortByImpl;
import ddf.catalog.operation.QueryImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.SourceResponse;

@Command(scope = CatalogCommands.NAMESPACE, name = "latest", description = "Retrieves the latest records from the catalog based on METACARD MODIFIED date.")
public class LatestCommand extends CatalogCommands {
    private static final int MAX_LENGTH = 40;

    private static final String ID = "ID ";

    private static final String TITLE = "Title ";

    private static final String DATE = "Modified Date ";

    private static final String NUMBER = "#";

    @Argument(name = "NUMBER_OF_ITEMS", description = "Number of maximum records to display. 0 returns all records.", index = 0, multiValued = false, required = false)
    int numberOfItems = DEFAULT_NUMBER_OF_ITEMS;

    @Override
    protected Object doExecute() throws Exception {

        PrintStream console = System.out;

        String formatString = "%1$-7s %2$-33s %3$-26s %4$-" + MAX_LENGTH
                + "s%n";

        console.printf(formatString, "", "", "", "");
        console.print(Ansi.ansi().fg(Ansi.Color.CYAN).toString());
        console.printf(formatString, NUMBER, ID, DATE, TITLE);
        console.print(Ansi.ansi().reset().toString());

        CatalogFacade catalogProvider = getCatalog();

        Filter filter = getFilterBuilder().attribute(Metacard.MODIFIED)
                .before().date(new Date());

        QueryImpl query = new QueryImpl(filter);

        query.setPageSize(numberOfItems);

        query.setSortBy(new SortByImpl(Metacard.MODIFIED, SortOrder.DESCENDING
                .name()));

        QueryRequest queryRequest = new QueryRequestImpl(query);

        SourceResponse response = catalogProvider.query(queryRequest);

        List<Result> results = response.getResults();

        int i = 1;
        for (Result result : results) {
            if(result.getMetacard() == null) {
                continue;
            }
            
            String postedDate = "";
            String title = "";

            if (result.getMetacard().getModifiedDate() != null) {
                postedDate = new DateTime(result.getMetacard()
                        .getModifiedDate()).toString(DATETIME_FORMATTER);
            }

            if (isNotBlank(result.getMetacard().getTitle())) {
                title = result.getMetacard().getTitle();
            }

            console.printf(formatString, i, result.getMetacard().getId(),
                    postedDate,
                    title.substring(0, Math.min(title.length(), MAX_LENGTH)));

            i++;
        }

        return null;
    }

}
