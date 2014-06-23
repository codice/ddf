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

import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;

@Command(scope = CatalogCommands.NAMESPACE, name = "range", description = "Searches by the given range arguments (exclusively).")
public class RangeCommand extends CatalogCommands {

    private static final int MAX_LENGTH = 40;

    private static final String ID = "ID ";

    private static final String TITLE = "Title ";

    private static final String NUMBER = "#";

    private static final SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");

    @Argument(name = "ATTRIBUTE_NAME", description = "The attribute to query on.", index = 0, multiValued = false, required = true)
    String attributeName = Metacard.MODIFIED;

    @Argument(name = "PARAMETER_1", description = "The first parameter can be a Date or an asterisk (*). Dates should be formatted as MM-dd-yyyy such as 01-23-2009.", index = 1, multiValued = false, required = true)
    String parameter1 = null;

    @Argument(name = "PARAMETER_2", description = "The second parameter can be a Date or an asterisk (*). Dates should be formatted as MM-dd-yyyy such as 01-23-2009.", index = 2, multiValued = false, required = true)
    String parameter2 = null;

    @Override
    protected Object doExecute() throws Exception {

        String formatString = "%1$-7s %2$-33s %3$-26s %4$-" + MAX_LENGTH + "s%n";

        console.printf(formatString, "", "", "", "");
        console.print(Ansi.ansi().fg(Ansi.Color.CYAN).toString());
        console.printf(formatString, NUMBER, ID, attributeName, TITLE);
        console.print(Ansi.ansi().reset().toString());

        CatalogFacade catalogProvider = getCatalog();
        FilterBuilder builder = getFilterBuilder();

        Filter filter = null;

        Date wayInTheFuture = new DateTime().plusYears(5000).toDate();
        Date wayInThePast = new DateTime().minusYears(5000).toDate();
        Date endDate = wayInTheFuture;
        Date startDate = wayInThePast;

        if (WILDCARD.equals(parameter1) && WILDCARD.equals(parameter2)) {
            filter = builder.attribute(attributeName).before().date(endDate);
        } else if (WILDCARD.equals(parameter1) && !WILDCARD.equals(parameter2)) {
            try {
                endDate = formatter.parse(parameter2);
            } catch (ParseException e) {
                throw new InterruptedException("Could not parse second parameter [" + parameter2
                        + "]");
            }
            filter = builder.attribute(attributeName).before().date(endDate);
        } else if (!WILDCARD.equals(parameter1) && WILDCARD.equals(parameter2)) {
            try {
                startDate = formatter.parse(parameter1);
            } catch (ParseException e) {
                throw new InterruptedException("Could not parse first parameter [" + parameter1
                        + "]");
            }
            filter = builder.attribute(attributeName).during().dates(startDate, endDate);
        } else {
            try {
                startDate = formatter.parse(parameter1);
                endDate = formatter.parse(parameter2);
            } catch (ParseException e) {
                throw new InterruptedException("Could not parse date parameters.");
            }
            filter = builder.attribute(attributeName).during().dates(startDate, endDate);
        }

        QueryImpl query = new QueryImpl(filter);

        query.setPageSize(30);

        query.setSortBy(new SortByImpl(attributeName, SortOrder.DESCENDING.name()));

        QueryRequest queryRequest = new QueryRequestImpl(query);

        SourceResponse response = catalogProvider.query(queryRequest);

        List<Result> results = response.getResults();

        int i = 1;
        for (Result result : results) {
            Attribute attribute = result.getMetacard().getAttribute(attributeName);
            if (attribute != null && attribute.getValue() != null) {
                String returnedDate = new DateTime(attribute.getValue())
                        .toString(DATETIME_FORMATTER);
                String title = result.getMetacard().getTitle();

                console.printf(formatString, i, result.getMetacard().getId(), returnedDate,
                        title.substring(0, Math.min(title.length(), MAX_LENGTH)));
            }

            i++;
        }

        return null;
    }

}
