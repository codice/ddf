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
package org.codice.ddf.catalog.ui.metacard;

import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_TAG;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.ui.metacard.query.data.metacard.QueryMetacardTypeImpl;
import org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import spark.Request;

public class RetreiveAllQueryRequests {
  private static final int MIN_START = 1;

  private static final int MAX_PAGE_SIZE = 100;

  private static final String START = "start";

  private static final String COUNT = "count";

  private static final String ATTR = "attr";

  private static final String TEXT = "text";

  private static final String ASCENDING = "asc";

  private static final String SORT_BY = "sort_by";

  private final FilterBuilder filterBuilder;

  private static final Set<String> SEARCHABLE_ATTRIBUTES =
      ImmutableSet.of(
          Core.TITLE, Core.METACARD_OWNER, Core.DESCRIPTION, QueryAttributes.QUERY_SOURCES);

  private final Request req;

  private QueryRequest queryRequest;

  private int count;

  public RetreiveAllQueryRequests(Request req, FilterBuilder filterBuilder) {
    this.req = req;
    this.filterBuilder = filterBuilder;
    initializeQueryRequest();
  }

  private void initializeQueryRequest() {
    int start = getOrDefaultParam(req, START, MIN_START);
    this.count = getOrDefaultParam(req, COUNT, MAX_PAGE_SIZE);

    SortOrder sort = getSortOrder(req);
    String attr =
        getOrDefaultParam(req, ATTR, Core.MODIFIED, QueryMetacardTypeImpl.getQueryAttributeNames());
    String text = getOrDefaultParam(req, TEXT, null, Collections.emptySet());

    Filter filter;

    if (StringUtils.isNotBlank(text)) {
      filter = getFuzzyQueryMetacardAttributeFilter(text);
    } else {
      filter = getQueryMetacardFilter();
    }

    queryRequest =
        new QueryRequestImpl(
            new QueryImpl(
                filter,
                start,
                count,
                new SortByImpl(attr, sort),
                false,
                TimeUnit.SECONDS.toMillis(10)),
            false);
  }

  public int getCount() {
    return count;
  }

  public QueryRequest getQueryRequest() {
    return queryRequest;
  }

  private static String getOrDefaultParam(
      Request request, String key, String defaultValue, Set<String> validValues) {
    String value = request.queryParams(key);

    if (value != null && (validValues.isEmpty() || validValues.contains(value.toLowerCase()))) {
      return value;
    }

    return defaultValue;
  }

  private static int getOrDefaultParam(Request request, String key, int defaultValue) {
    String value = request.queryParams(key);

    if (value != null) {
      return Integer.parseInt(value);
    }

    return defaultValue;
  }

  private static SortOrder getSortOrder(Request request) {
    String value = request.queryParams(SORT_BY);

    if (ASCENDING.equals(value)) {
      return SortOrder.ASCENDING;
    } else {
      return SortOrder.DESCENDING;
    }
  }

  private Filter getFuzzyQueryMetacardAttributeFilter(String value) {
    List<Filter> attributeFilters =
        SEARCHABLE_ATTRIBUTES
            .stream()
            .map(name -> getFuzzyAttributeFilter(name, value))
            .collect(Collectors.toList());
    return filterBuilder.allOf(getQueryMetacardFilter(), filterBuilder.anyOf(attributeFilters));
  }

  private Filter getQueryMetacardFilter() {
    return filterBuilder.attribute(Core.METACARD_TAGS).is().equalTo().text(QUERY_TAG);
  }

  private Filter getFuzzyAttributeFilter(String attribute, String value) {
    return filterBuilder.attribute(attribute).is().like().fuzzyText(value);
  }
}
