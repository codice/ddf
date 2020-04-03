/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

@Service
@Command(
    scope = CatalogCommands.NAMESPACE,
    name = "range",
    description = "Searches by the given date range arguments (exclusively).")
public class RangeCommand extends CatalogCommands {

  private static final int MAX_LENGTH = 40;

  private static final int MAX_RESULTS = 30;

  private static final String ID = "ID ";

  private static final String TITLE = "Title ";

  private static final String NUMBER = "#";

  private static final String DATE_FORMAT = "MM-dd-yyyy";

  @Argument(
      name = "ATTRIBUTE_NAME",
      description =
          "The attribute to query on. Valid values are \""
              + Core.MODIFIED
              + "\", \""
              + Core.CREATED
              + "\", \""
              + Metacard.EFFECTIVE
              + "\", and"
              + " \""
              + Core.EXPIRATION
              + "\".",
      index = 0,
      multiValued = false,
      required = true)
  String attributeName = Core.MODIFIED;

  @Argument(
      name = "RANGE_BEGINNING",
      description =
          "The first parameter can be a Date or an asterisk (*). Dates should be formatted as MM-dd-yyyy such as 01-23-2009.",
      index = 1,
      multiValued = false,
      required = true)
  String rangeBeginning = null;

  @Argument(
      name = "RANGE_END",
      description =
          "The second parameter can be a Date or an asterisk (*). Dates should be formatted as MM-dd-yyyy such as 01-23-2009.",
      index = 2,
      multiValued = false,
      required = true)
  String rangeEnd = null;

  @Override
  protected Object executeWithSubject() throws Exception {
    String formatString = "%1$-7s %2$-33s %3$-26s %4$-" + MAX_LENGTH + "s%n";

    console.printf(formatString, "", "", "", "");
    printHeaderMessage(String.format(formatString, NUMBER, ID, attributeName, TITLE));

    Filter filter;

    Date wayInTheFuture = new DateTime().plusYears(5000).toDate();
    Date endDate = wayInTheFuture;

    SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

    if (WILDCARD.equals(rangeBeginning) && WILDCARD.equals(rangeEnd)) {
      filter = filterBuilder.attribute(attributeName).before().date(endDate);
    } else if (WILDCARD.equals(rangeBeginning) && !WILDCARD.equals(rangeEnd)) {
      try {
        endDate = formatter.parse(rangeEnd);
      } catch (ParseException e) {
        throw new InterruptedException("Could not parse second parameter [" + rangeEnd + "]");
      }
      filter = filterBuilder.attribute(attributeName).before().date(endDate);
    } else if (!WILDCARD.equals(rangeBeginning) && WILDCARD.equals(rangeEnd)) {
      try {
        Date startDate = formatter.parse(rangeBeginning);
        filter = filterBuilder.attribute(attributeName).during().dates(startDate, endDate);
      } catch (ParseException e) {
        throw new InterruptedException("Could not parse first parameter [" + rangeBeginning + "]");
      }
    } else {
      try {
        Date startDate = formatter.parse(rangeBeginning);
        endDate = formatter.parse(rangeEnd);
        filter = filterBuilder.attribute(attributeName).during().dates(startDate, endDate);
      } catch (ParseException e) {
        throw new InterruptedException("Could not parse date parameters.");
      }
    }

    QueryImpl query = new QueryImpl(filter);

    query.setPageSize(MAX_RESULTS);

    query.setSortBy(new SortByImpl(attributeName, SortOrder.DESCENDING.name()));

    QueryRequest queryRequest = new QueryRequestImpl(query);

    SourceResponse response = getCatalog().query(queryRequest);

    List<Result> results = response.getResults();

    int i = 1;
    for (Result result : results) {
      Attribute attribute = result.getMetacard().getAttribute(attributeName);
      if (attribute != null && attribute.getValue() != null) {
        String returnedDate = new DateTime(attribute.getValue()).toString(DATETIME_FORMATTER);
        String title = result.getMetacard().getTitle();

        console.printf(
            formatString,
            i,
            result.getMetacard().getId(),
            returnedDate,
            title.substring(0, Math.min(title.length(), MAX_LENGTH)));
      }

      i++;
    }

    return null;
  }
}
