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
package ddf.catalog.source.opensearch.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Result;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.shiro.subject.PrincipalCollection;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchParserImplTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchParserImplTest.class);

  private static final StringBuilder URL =
      new StringBuilder("http://localhost:8080/services/catalog/query");

  private static final String MAX_RESULTS = "2000";

  private static final String TIMEOUT = "30000";

  private static final String DESCENDING_TEMPORAL_SORT = "date%3Adesc";

  private OpenSearchParserImpl openSearchParserImpl;

  @Before
  public void setUp() {
    openSearchParserImpl = new OpenSearchParserImpl();
  }

  @Test
  public void populateSpatialDistanceFilterBbox() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    SpatialDistanceFilter spatialFilter = new SpatialDistanceFilter("POINT (1 1)", 1.0);
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatialFilter,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_BBOX));
  }

  @Test
  public void populateSpatialDistanceFilterBboxMin() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    SpatialDistanceFilter spatialFilter = new SpatialDistanceFilter("POINT (-211 -211)", 1.0);
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatialFilter,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_BBOX));
  }

  @Test
  public void populateSpatialDistanceFilterBboxMax() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    SpatialDistanceFilter spatialFilter = new SpatialDistanceFilter("POINT (211 211)", 1.0);
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatialFilter,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_BBOX));
  }

  @Test
  public void populateSpatialDistanceFilterInvalidWktBbox() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    SpatialDistanceFilter spatialFilter = new SpatialDistanceFilter("POLYGON (1 1)", 1.0);
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatialFilter,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.GEO_BBOX)));
  }

  @Test
  public void populateSpatialFilterInvalidWktBbox() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    SpatialFilter spatialFilter = new SpatialFilter("POINT (1 1)");
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatialFilter,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.GEO_BBOX)));
  }

  @Test
  public void populateSpatialFilterBbox() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    SpatialFilter spatialFilter = new SpatialFilter("POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))");
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatialFilter,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_BBOX));
  }

  @Test
  public void populateNullSpatialFilter() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateGeospatial(
        webClient,
        (SpatialFilter) null,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.GEO_BBOX)));
  }

  @Test
  public void populateSpatialFilterCaseInsensitiveParameters() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    SpatialFilter spatialFilter = new SpatialFilter("POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))");
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatialFilter,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,BBOX,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_BBOX));
  }

  @Test
  public void populateSpatialFilterEmptyParameters() throws Exception {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    SpatialFilter spatialFilter = new SpatialFilter("POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))");
    openSearchParserImpl.populateGeospatial(webClient, spatialFilter, true, new ArrayList<>());
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.GEO_BBOX)));
  }

  @Test
  public void populateContextual() {
    Map<String, String> searchPhraseMap = new HashMap<>();
    searchPhraseMap.put("q", "TestQuery123");

    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateContextual(
        webClient,
        searchPhraseMap,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(searchPhraseMap.get("q")));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    try {
      new URL(urlStr);
    } catch (MalformedURLException mue) {
      fail("URL is not valid: " + mue.getMessage());
    }
    LOGGER.info("URL after contextual population: {}", urlStr);
  }

  @Test
  public void populateContextualUnrecognizedParameter() {
    Map<String, String> searchPhraseMap = new HashMap<>();
    searchPhraseMap.put("z", "TestQuery123");

    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateContextual(
        webClient,
        searchPhraseMap,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString(searchPhraseMap.get("z"))));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    try {
      new URL(urlStr);
    } catch (MalformedURLException mue) {
      fail("URL is not valid: " + mue.getMessage());
    }
    LOGGER.info("URL after contextual population: {}", urlStr);
  }

  @Test
  public void populateContextualEmptyMap() {
    Map<String, String> searchPhraseMap = new HashMap<>();

    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateContextual(
        webClient,
        searchPhraseMap,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString("q=")));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    try {
      new URL(urlStr);
    } catch (MalformedURLException mue) {
      fail("URL is not valid: " + mue.getMessage());
    }
    LOGGER.info("URL after contextual population: {}", urlStr);
  }

  @Test
  public void populateContextualNullMap() {
    String urlString = URL.toString();
    assertThat(urlString, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateContextual(
        webClient,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString("q=")));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SEARCH_TERMS));
    try {
      new URL(urlStr);
    } catch (MalformedURLException mue) {
      fail("URL is not valid: " + mue.getMessage());
    }
    LOGGER.info("URL after contextual population: {}", urlStr);
  }

  /** Verify that passing in null will still remove the parameters from the URL. */
  @Test
  public void populateNullContextual() {
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateContextual(
        webClient,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString("?" + OpenSearchParserImpl.SEARCH_TERMS)));
  }

  @Test
  public void populateTemporal() {
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    Date start = new Date(System.currentTimeMillis() - 10000000);
    Date end = new Date(System.currentTimeMillis());
    StringBuilder resultStr = new StringBuilder(URL);
    TemporalFilter temporal = new TemporalFilter(start, end);

    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateTemporal(
        webClient,
        temporal,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    String startT = fmt.print(start.getTime());
    try {
      assertThat(urlStr, containsString(URLEncoder.encode(startT, "UTF-8")));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
    String endT = fmt.print(end.getTime());
    try {
      assertThat(urlStr, containsString(URLEncoder.encode(endT, "UTF-8")));
    } catch (UnsupportedEncodingException e) {
      fail(e.getMessage());
    }
    assertThat(urlStr, containsString(OpenSearchParserImpl.TIME_START));
    assertThat(urlStr, containsString(OpenSearchParserImpl.TIME_END));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.TIME_NAME)));
    try {
      new URL(resultStr.toString());
    } catch (MalformedURLException mue) {
      fail("URL is not valid: " + mue.getMessage());
    }
    LOGGER.info("URL after temporal population: {}", resultStr.toString());
  }

  @Test
  public void populateEmptyTemporal() {
    TemporalFilter temporalFilter = new TemporalFilter(0L);
    temporalFilter.setEndDate(null);
    temporalFilter.setStartDate(null);

    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateTemporal(
        webClient,
        temporalFilter,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.TIME_START));
    assertThat(urlStr, containsString(OpenSearchParserImpl.TIME_END));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.TIME_NAME)));
  }

  /** Verify that passing in null will still remove the parameters from the URL. */
  @Test
  public void populateNullTemporal() {

    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateTemporal(
        webClient,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.TIME_START)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.TIME_END)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.TIME_NAME)));
  }

  @Test
  public void populateSearchOptions() throws UnsupportedEncodingException {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    WebClient webClient = WebClient.create(URL.toString());

    openSearchParserImpl.populateSearchOptions(
        webClient,
        queryRequest,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(MAX_RESULTS));
    assertThat(urlStr, containsString(TIMEOUT));
    assertThat(urlStr, containsString(DESCENDING_TEMPORAL_SORT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.COUNT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_RESULTS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_TIMEOUT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SORT));
  }

  @Test
  public void populateSearchOptionsSortAscending() throws UnsupportedEncodingException {
    String sort = "date%3Aasc";
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    WebClient webClient = WebClient.create(URL.toString());

    openSearchParserImpl.populateSearchOptions(
        webClient,
        queryRequest,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(MAX_RESULTS));
    assertThat(urlStr, containsString(TIMEOUT));
    assertThat(urlStr, containsString(sort));
    assertThat(urlStr, containsString(OpenSearchParserImpl.COUNT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_RESULTS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_TIMEOUT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SORT));
  }

  @Test
  public void populateSearchOptionsSortRelevance() throws UnsupportedEncodingException {
    String sort = "sort=relevance%3Adesc";
    SortBy sortBy = new SortByImpl(Result.RELEVANCE, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    WebClient webClient = WebClient.create(URL.toString());

    openSearchParserImpl.populateSearchOptions(
        webClient,
        queryRequest,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(MAX_RESULTS));
    assertThat(urlStr, containsString(TIMEOUT));
    assertThat(urlStr, containsString(sort));
    assertThat(urlStr, containsString(OpenSearchParserImpl.COUNT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_RESULTS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_TIMEOUT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SORT));
  }

  @Test
  public void populateSearchOptionsSortRelevanceUnsupported() throws UnsupportedEncodingException {
    String sort = "sort=relevance%3Adesc";
    SortBy sortBy = new SortByImpl(Result.DISTANCE, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    WebClient webClient = WebClient.create(URL.toString());

    openSearchParserImpl.populateSearchOptions(
        webClient,
        queryRequest,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(MAX_RESULTS));
    assertThat(urlStr, containsString(TIMEOUT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.COUNT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_RESULTS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_TIMEOUT));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.SORT)));
    assertThat(urlStr, not(containsString(sort)));
  }

  @Test
  public void populateSearchOptionsNullSort() throws UnsupportedEncodingException {
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, null, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    WebClient webClient = WebClient.create(URL.toString());

    openSearchParserImpl.populateSearchOptions(
        webClient,
        queryRequest,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(MAX_RESULTS));
    assertThat(urlStr, containsString(TIMEOUT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.COUNT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_RESULTS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_TIMEOUT));
    assertThat(urlStr, not(containsString(DESCENDING_TEMPORAL_SORT)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.SORT)));
  }

  @Test
  public void populateSearchOptionsNegativePageSize() throws UnsupportedEncodingException {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, -1000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    WebClient webClient = WebClient.create(URL.toString());

    openSearchParserImpl.populateSearchOptions(
        webClient,
        queryRequest,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.DEFAULT_TOTAL_MAX.toString()));
    assertThat(urlStr, containsString(TIMEOUT));
    assertThat(urlStr, containsString(DESCENDING_TEMPORAL_SORT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.COUNT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_RESULTS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_TIMEOUT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SORT));
  }

  @Test
  public void populateSearchOptionsWithSubject() throws UnsupportedEncodingException {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    WebClient webClient = WebClient.create(URL.toString());

    String principalName = "principalName";
    Subject subject = getMockSubject(principalName);

    openSearchParserImpl.populateSearchOptions(
        webClient,
        queryRequest,
        subject,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(principalName));
    assertThat(urlStr, containsString(MAX_RESULTS));
    assertThat(urlStr, containsString(TIMEOUT));
    assertThat(urlStr, containsString(DESCENDING_TEMPORAL_SORT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.COUNT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_RESULTS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_TIMEOUT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SORT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SORT));
  }

  @Test
  public void populateSearchOptionsWithNullPrincipalSubject() throws UnsupportedEncodingException {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    WebClient webClient = WebClient.create(URL.toString());

    String principalName = "principalName";
    Subject subject = getMockSubject(principalName);
    when(subject.getPrincipals()).thenReturn(null);
    openSearchParserImpl.populateSearchOptions(
        webClient,
        queryRequest,
        subject,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString(principalName)));
    assertThat(urlStr, containsString(MAX_RESULTS));
    assertThat(urlStr, containsString(TIMEOUT));
    assertThat(urlStr, containsString(DESCENDING_TEMPORAL_SORT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.COUNT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_RESULTS));
    assertThat(urlStr, containsString(OpenSearchParserImpl.MAX_TIMEOUT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SORT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.SORT));
  }

  /** Verify that passing in null will still remove the parameters from the URL. */
  @Test
  public void populateNullSearchOptions() {
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateSearchOptions(
        webClient,
        null,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.COUNT)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.MAX_RESULTS)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.SRC)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.USER_DN)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.MAX_TIMEOUT)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.FILTER)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.SORT)));
  }

  @Test
  public void populatePolyGeospatial() throws Exception {
    String wktPolygon = "POLYGON((1 1,5 1,5 5,1 5,1 1))";
    String expectedStr = "1,1,1,5,5,5,5,1,1,1";

    SpatialFilter spatial = new SpatialFilter(wktPolygon);
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatial,
        false,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(expectedStr));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_POLY));
  }

  @Test
  public void populateLatLonRadGeospatial() throws Exception {
    String lat = "43.25";
    String lon = "-123.45";
    String radius = "10000";
    String wktPoint = "POINT(" + lon + " " + lat + ")";

    SpatialDistanceFilter spatial = new SpatialDistanceFilter(wktPoint, radius);
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateGeospatial(
        webClient,
        spatial,
        false,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(lat));
    assertThat(urlStr, containsString(lon));
    assertThat(urlStr, containsString(radius));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_LAT));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_LON));
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_RADIUS));

    try {
      new URL(urlStr);
    } catch (MalformedURLException mue) {
      fail("URL is not valid: " + mue.getMessage());
    }
    LOGGER.info("URL after lat lon geospatial population: {}", urlStr);
  }

  /** Verify that passing in null will still remove the parameters from the URL. */
  @Test
  public void populateNullGeospatial() throws Exception {
    WebClient webClient = WebClient.create(URL.toString());
    openSearchParserImpl.populateGeospatial(
        webClient,
        null,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.GEO_LAT)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.GEO_LON)));
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.GEO_RADIUS)));
  }

  private Subject getMockSubject(String principalName) {
    Subject subject = mock(Subject.class);
    PrincipalCollection principalCollection = mock(PrincipalCollection.class);
    SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
    Principal principal = mock(Principal.class);
    when(securityAssertion.getPrincipal()).thenReturn(principal);
    when(principal.getName()).thenReturn(principalName);
    when(principalCollection.asList()).thenReturn(Collections.singletonList(securityAssertion));
    when(subject.getPrincipals()).thenReturn(principalCollection);
    return subject;
  }
}
