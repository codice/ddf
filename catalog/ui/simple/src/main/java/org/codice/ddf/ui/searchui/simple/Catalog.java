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
package org.codice.ddf.ui.searchui.simple;

import static ddf.catalog.data.AttributeType.AttributeFormat.BINARY;
import static ddf.catalog.data.AttributeType.AttributeFormat.OBJECT;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Catalog {

  private static final Logger LOGGER = LoggerFactory.getLogger(Catalog.class);

  private static final int PAGE_SIZE = 50;

  private CatalogFramework catalog;

  private FilterBuilder filterBuilder;

  boolean isThumbnailDataUri = false;

  public Catalog(CatalogFramework catalogFramework, FilterBuilder filterBuilder) {
    this.catalog = catalogFramework;
    this.filterBuilder = filterBuilder;
  }

  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalog = catalogFramework;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  public boolean hasQuery(HttpServletRequest request) {
    String q = request.getParameter("q");
    return q != null && !q.isBlank();
  }

  public MetacardDetails metacard(String id) {
    MetacardDetails details = new MetacardDetails();

    if (id == null || id.isBlank()) {
      return details;
    }

    Filter filter = filterBuilder.attribute(Metacard.ID).is().equalTo().text(id);

    QueryImpl query = new QueryImpl(filter);
    query.setPageSize(1);

    QueryRequestImpl queryRequest = new QueryRequestImpl(query);

    try {
      QueryResponse result = catalog.query(queryRequest);
      if (result.getResults().size() == 1) {
        Metacard metacard = result.getResults().get(0).getMetacard();

        details.title = metacard.getTitle();
        details.source = metacard.getSourceId();
        details.thumbnail = createThumbnailSrc(metacard);
        details.download = createDownloadUrl(metacard);

        for (AttributeDescriptor descriptor :
            metacard.getMetacardType().getAttributeDescriptors()) {
          String name = descriptor.getName();
          AttributeType.AttributeFormat format = descriptor.getType().getAttributeFormat();
          Attribute attribute = metacard.getAttribute(name);
          if (attribute != null && attribute.getValue() != null) {
            Serializable value = metacard.getAttribute(name).getValue();

            if (BINARY.equals(format)) {
              details.attributes.put(name, ((byte[]) value).length + " bytes");
            } else if (OBJECT.equals(format)) {
              details.attributes.put(name, value.getClass().getName());
            } else {
              details.attributes.put(name, value.toString());
            }
          }
        }
      }
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      LOGGER.debug("Metacard {} not found", id, e);
    }

    return details;
  }

  public QueryResult query(HttpServletRequest request) {
    String q = request.getParameter("q");
    int start = getStart(request.getParameter("start"));
    String sort = request.getParameter("sort");
    String past = request.getParameter("past");

    QueryResult result = new QueryResult();
    result.start = start;

    Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(q);
    filter = addPast(past, filter);

    QueryImpl query = new QueryImpl(filter);
    query.setPageSize(PAGE_SIZE);
    query.setStartIndex(start);
    query.setRequestsTotalResultsCount(true);
    setSort(sort, query);

    QueryRequestImpl queryRequest = new QueryRequestImpl(query);

    QueryResponse queryResponse;
    try {
      queryResponse = catalog.query(queryRequest);
      updateResults(result, queryResponse);
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      LOGGER.debug("Exception on query {}", q, e);

      QueryResponseImpl response = new QueryResponseImpl(queryRequest);
      response.closeResultQueue();
      response.setProcessingDetails(Set.of(new ProcessingDetailsImpl("local", e)));

      queryResponse = response;
    }
    result.hasErrors = queryResponse.getProcessingDetails().size() > 0;
    setPaging(queryResponse, result);
    return result;
  }

  private void updateResults(QueryResult result, QueryResponse queryResponse) {
    if (queryResponse.getResults().size() > 0) {
      result.hasResults = true;
    }
    setMetacards(queryResponse, result);
    if (queryResponse.getHits() >= 0) {
      result.totalResults = String.valueOf(queryResponse.getHits());
    }
  }

  private void setMetacards(QueryResponse queryResponse, QueryResult result) {
    for (Result current : queryResponse.getResults()) {
      Metacard mc = current.getMetacard();

      MetacardResult metacard = new MetacardResult();
      metacard.id = mc.getId();
      metacard.title = mc.getTitle();
      metacard.source = mc.getSourceId();
      metacard.thumbnail = createThumbnailSrc(mc);
      metacard.download = createDownloadUrl(mc);

      result.metacards.add(metacard);
    }
  }

  private Filter addPast(String past, Filter filter) {
    AttributeBuilder modified = filterBuilder.attribute(Core.METACARD_MODIFIED);
    if ("day".equalsIgnoreCase(past)) {
      Date date =
          Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
      return filterBuilder.allOf(filter, modified.after().date(date));
    } else if ("week".equalsIgnoreCase(past)) {
      Date date =
          Date.from(LocalDate.now().minusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
      return filterBuilder.allOf(filter, modified.after().date(date));
    } else if ("month".equalsIgnoreCase(past)) {
      Date date =
          Date.from(
              LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
      return filterBuilder.allOf(filter, modified.after().date(date));
    } else if ("year".equalsIgnoreCase(past)) {
      Date date =
          Date.from(LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
      return filterBuilder.allOf(filter, modified.after().date(date));
    } else {
      return filter;
    }
  }

  private void setSort(String sort, QueryImpl query) {
    if ("modified".equalsIgnoreCase(sort)) {
      query.setSortBy(new SortByImpl(Core.METACARD_MODIFIED, SortOrder.DESCENDING));
    } else if ("title".equalsIgnoreCase(sort)) {
      query.setSortBy(new SortByImpl(Core.TITLE, SortOrder.ASCENDING));
    } else if ("relevance".equalsIgnoreCase(sort)) {
      query.setSortBy(new SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING));
    } else {
      query.setSortBy(new SortByImpl(Core.ID, SortOrder.ASCENDING));
    }
  }

  public String createThumbnailSrc(Metacard metacard) {
    if (metacard.getThumbnail() == null || metacard.getThumbnail().length == 0) {
      return "";
    }

    if (isThumbnailDataUri) {
      String encodedThumbnail = Base64.getEncoder().encodeToString(metacard.getThumbnail());
      return "data:image/jpeg;charset=utf-8;base64, " + encodedThumbnail;
    } else {
      return "/services/catalog/" + metacard.getId() + "?transform=thumbnail";
    }
  }

  public String createDownloadUrl(Metacard metacard) {
    if (metacard.getResourceURI() == null) {
      return "";
    }

    return "/services/catalog/sources/"
        + metacard.getSourceId()
        + "/"
        + metacard.getId()
        + "?transform=resource";
  }

  private void setPaging(QueryResponse queryResponse, QueryResult result) {
    result.previousStart = result.start - PAGE_SIZE > 0 ? result.start - PAGE_SIZE : 1;
    result.previousDisabled = result.start == 1;

    result.nextStart =
        queryResponse.getResults().size() == PAGE_SIZE ? result.start + PAGE_SIZE : result.start;
    result.nextDisabled = result.nextStart == result.start;
  }

  private static int getStart(String startIndex) {
    int start = 1;
    if (StringUtils.isNumeric(startIndex)) {
      int startParam = Integer.parseInt(startIndex);
      start = Math.max(startParam, start);
    }
    return start;
  }

  public static class QueryResult {
    public List<MetacardResult> metacards = new ArrayList<>();
    public boolean hasErrors;
    public boolean hasResults;
    public String totalResults = "Unknown";
    public int start;
    public boolean previousDisabled;
    public int previousStart;
    public boolean nextDisabled;
    public int nextStart;
  }

  public static class MetacardResult {
    public String id = "";
    public String title = "No Title";
    public String source = "Unknown Source";
    public String thumbnail = "";
    public String download = "";
  }

  public static class MetacardDetails {
    public String title = "Not Found";
    public String source = "N/A";
    public String thumbnail = "";
    public String download = "";
    public Map<String, String> attributes = new TreeMap<>();
  }
}
