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

import static ddf.catalog.util.impl.ResultIterable.resultIterable;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
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
  name = "latest",
  description = "Retrieves the latest records from the Catalog based on a filter."
)
public class LatestCommand extends CatalogCommands {

  private static final int MAX_LENGTH = 40;

  private static final String ID = "ID ";

  private static final String TITLE = "Title ";

  private static final String DATE = "Modified Date ";

  private static final String NUMBER = "#";

  @Argument(
    name = "NUMBER_OF_ITEMS",
    description = "Maximum number of records to display. 0 returns 1000 records.",
    index = 0,
    multiValued = false,
    required = false
  )
  int numberOfItems = DEFAULT_NUMBER_OF_ITEMS;

  @Override
  protected Object executeWithSubject() throws Exception {
    String formatString = "%1$-7s %2$-33s %3$-26s %4$-" + MAX_LENGTH + "s%n";

    console.printf(formatString, "", "", "", "");
    printHeaderMessage(String.format(formatString, NUMBER, ID, DATE, TITLE));

    Filter filter = filterBuilder.attribute(Core.METACARD_MODIFIED).before().date(new Date());

    QueryImpl query = new QueryImpl(filter);
    query.setPageSize(numberOfItems);
    query.setSortBy(new SortByImpl(Core.METACARD_MODIFIED, SortOrder.DESCENDING.name()));

    QueryRequest queryRequest = new QueryRequestImpl(query);

    List<Result> results =
        Lists.newArrayList(
            resultIterable(
                getCatalog()::query, queryRequest, numberOfItems > 0 ? numberOfItems : 1000));

    int i = 1;
    for (Result result : results) {
      if (result.getMetacard() == null) {
        continue;
      }

      String postedDate = "";
      String title = "";

      if (result.getMetacard().getAttribute(Core.METACARD_MODIFIED) != null) {
        postedDate =
            new DateTime(result.getMetacard().getAttribute(Core.METACARD_MODIFIED).getValue())
                .toString(DATETIME_FORMATTER);
      }

      if (isNotBlank(result.getMetacard().getTitle())) {
        title = result.getMetacard().getTitle();
      }

      console.printf(
          formatString,
          i,
          result.getMetacard().getId(),
          postedDate,
          title.substring(0, Math.min(title.length(), MAX_LENGTH)));

      i++;
    }

    return null;
  }
}
