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
package org.codice.ddf.catalog.ui.query.cql;

import static spark.Spark.halt;

import com.google.common.collect.Sets;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CqlRequest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CqlRequest.class);

  private static final String DEFAULT_SORT_ORDER = "desc";

  private static final String LOCAL_SOURCE = "local";

  private static final String CACHE_SOURCE = "cache";

  private String id;

  private String src;

  private long timeout = 300000L;

  private int start = 1;

  private int count = 10;

  private String cql;

  private List<Sort> sorts = Collections.emptyList();

  private boolean normalize = false;

  private boolean excludeUnnecessaryAttributes = true;

  public String getSrc() {
    return src;
  }

  public void setSrc(String src) {
    this.src = src;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getCql() {
    return cql;
  }

  public void setCql(String cql) {
    this.cql = cql;
  }

  public List<Sort> getSorts() {
    return sorts;
  }

  public void setSorts(List<Sort> sorts) {
    this.sorts = sorts;
  }

  public boolean isNormalize() {
    return normalize;
  }

  public void setNormalize(boolean normalize) {
    this.normalize = normalize;
  }

  public QueryRequest createQueryRequest(String localSource, FilterBuilder filterBuilder) {
    List<SortBy> sortBys =
        sorts
            .stream()
            .filter(
                s ->
                    StringUtils.isNotEmpty(s.getAttribute())
                        && StringUtils.isNotEmpty(s.getDirection()))
            .map(s -> parseSort(s.getAttribute(), s.getDirection()))
            .collect(Collectors.toList());
    if (sortBys.isEmpty()) {
      sortBys.add(new SortByImpl(Result.TEMPORAL, DEFAULT_SORT_ORDER));
    }
    Query query =
        new QueryImpl(createFilter(filterBuilder), start, count, sortBys.get(0), true, timeout);

    String source = parseSrc(localSource);

    QueryRequest queryRequest;
    if (CACHE_SOURCE.equals(source)) {
      queryRequest = new QueryRequestImpl(query, true);
      queryRequest.getProperties().put("mode", "cache");
    } else {
      queryRequest = new QueryRequestImpl(query, Collections.singleton(source));
      queryRequest.getProperties().put("mode", "update");
    }

    if (excludeUnnecessaryAttributes) {
      queryRequest
          .getProperties()
          .put("excludeAttributes", Sets.newHashSet(Metacard.METADATA, "lux"));
    }

    if (sortBys.size() > 1) {
      queryRequest
          .getProperties()
          .put("additional.sorts.bys", sortBys.subList(1, sortBys.size()).toArray(new SortBy[0]));
    }

    return queryRequest;
  }

  private String parseSrc(String localSource) {
    if (StringUtils.equalsIgnoreCase(src, LOCAL_SOURCE) || StringUtils.isBlank(src)) {
      src = localSource;
    }

    return src;
  }

  private Filter createFilter(FilterBuilder filterBuilder) {
    Filter filter = null;
    try {
      filter = ECQL.toFilter(cql);
    } catch (CQLException e) {
      halt(400, "Unable to parse CQL filter");
    }

    if (filter == null) {
      LOGGER.debug("Received an empty filter. Using a wildcard contextual filter instead.");
      filter =
          filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(FilterDelegate.WILDCARD_CHAR);
    }

    return filter;
  }

  private SortBy parseSort(String sortField, String sortOrder) {
    SortBy sort;
    switch (sortOrder.toLowerCase(Locale.getDefault())) {
      case "ascending":
      case "asc":
        sort = new SortByImpl(sortField, SortOrder.ASCENDING);
        break;
      case "descending":
      case "desc":
        sort = new SortByImpl(sortField, SortOrder.DESCENDING);
        break;
      default:
        throw new IllegalArgumentException(
            "Incorrect sort order received, must be 'asc', 'ascending', 'desc', or 'descending'");
    }

    return sort;
  }

  public String getSource() {
    return src;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isExcludeUnnecessaryAttributes() {
    return excludeUnnecessaryAttributes;
  }

  public void setExcludeUnnecessaryAttributes(boolean excludeUnnecessaryAttributes) {
    this.excludeUnnecessaryAttributes = excludeUnnecessaryAttributes;
  }

  /** POJO binding for BOON */
  public static class Sort {
    private String attribute;
    private String direction;

    public Sort(String attribute, String direction) {
      this.attribute = attribute;
      this.direction = direction;
    }

    public String getAttribute() {
      return attribute;
    }

    public void setAttribute(String attribute) {
      this.attribute = attribute;
    }

    public String getDirection() {
      return direction;
    }

    public void setDirection(String direction) {
      this.direction = direction;
    }
  }
}
