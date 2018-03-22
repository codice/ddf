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
import ddf.catalog.source.opensearch.OpenSearchParser;
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

  private static final String MAX_RESULTS = "2000";

  private static final String TIMEOUT = "30000";

  private static final String DESCENDING_TEMPORAL_SORT = "date%3Adesc";

  private OpenSearchParser openSearchParser;

  private WebClient webClient;

  @Before
  public void setUp() {
    openSearchParser = new OpenSearchParserImpl();
    webClient = WebClient.create("http://www.example.com");
  }

  @Test
  public void populateSpatialFilterCaseInsensitiveParameters() {
    SpatialFilter spatialFilter = new SpatialFilter("POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))");
    openSearchParser.populateGeospatial(
        webClient,
        spatialFilter,
        true,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,BBOX,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, containsString(OpenSearchParserImpl.GEO_BBOX));
  }

  @Test
  public void populateSpatialFilterEmptyParameters() {
    SpatialFilter spatialFilter = new SpatialFilter("POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))");
    openSearchParser.populateGeospatial(webClient, spatialFilter, true, new ArrayList<>());
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString(OpenSearchParserImpl.GEO_BBOX)));
  }

  @Test
  public void populateContextual() {
    Map<String, String> searchPhraseMap = new HashMap<>();
    searchPhraseMap.put("q", "TestQuery123");

    openSearchParser.populateContextual(
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

    openSearchParser.populateContextual(
        webClient,
        searchPhraseMap,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString(searchPhraseMap.get("z"))));
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

    openSearchParser.populateContextual(
        webClient,
        searchPhraseMap,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString("q=")));
    try {
      new URL(urlStr);
    } catch (MalformedURLException mue) {
      fail("URL is not valid: " + mue.getMessage());
    }
    LOGGER.info("URL after contextual population: {}", urlStr);
  }

  @Test
  public void populateContextualNullMap() {
    openSearchParser.populateContextual(
        webClient,
        null,
        Arrays.asList(
            "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                .split(",")));
    String urlStr = webClient.getCurrentURI().toString();
    assertThat(urlStr, not(containsString("q=")));
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
    openSearchParser.populateContextual(
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
    TemporalFilter temporal = new TemporalFilter(start, end);

    openSearchParser.populateTemporal(
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
      new URL(urlStr);
    } catch (MalformedURLException mue) {
      fail("URL is not valid: " + mue.getMessage());
    }
    LOGGER.info("URL after temporal population: {}", urlStr);
  }

  @Test
  public void populateEmptyTemporal() {
    TemporalFilter temporalFilter = new TemporalFilter(0L);
    temporalFilter.setEndDate(null);
    temporalFilter.setStartDate(null);

    openSearchParser.populateTemporal(
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
    openSearchParser.populateTemporal(
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
  public void populateSearchOptions() {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    openSearchParser.populateSearchOptions(
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
  public void populateSearchOptionsSortAscending() {
    String sort = "date%3Aasc";
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    openSearchParser.populateSearchOptions(
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
  public void populateSearchOptionsSortRelevance() {
    String sort = "sort=relevance%3Adesc";
    SortBy sortBy = new SortByImpl(Result.RELEVANCE, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    openSearchParser.populateSearchOptions(
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
  public void populateSearchOptionsSortRelevanceUnsupported() {
    String sort = "sort=relevance%3Adesc";
    SortBy sortBy = new SortByImpl(Result.DISTANCE, SortOrder.ASCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    openSearchParser.populateSearchOptions(
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
  public void populateSearchOptionsNullSort() {
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, null, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    openSearchParser.populateSearchOptions(
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
  public void populateSearchOptionsNegativePageSize() {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, -1000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    openSearchParser.populateSearchOptions(
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
  public void populateSearchOptionsWithSubject() {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    String principalName = "principalName";
    Subject subject = getMockSubject(principalName);

    openSearchParser.populateSearchOptions(
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
  public void populateSearchOptionsWithNullPrincipalSubject() {
    SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
    Filter filter = mock(Filter.class);
    Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    String principalName = "principalName";
    Subject subject = getMockSubject(principalName);
    when(subject.getPrincipals()).thenReturn(null);
    openSearchParser.populateSearchOptions(
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
    openSearchParser.populateSearchOptions(
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
  public void populatePolyGeospatial() {
    String wktPolygon = "POLYGON((1 1,5 1,5 5,1 5,1 1))";
    String expectedStr = "1,1,1,5,5,5,5,1,1,1";

    SpatialFilter spatial = new SpatialFilter(wktPolygon);
    openSearchParser.populateGeospatial(
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
  public void populateLatLonRadGeospatial() {
    String lat = "43.25";
    String lon = "-123.45";
    String radius = "10000";
    String wktPoint = "POINT (" + lon + " " + lat + ")";

    SpatialDistanceFilter spatial = new SpatialDistanceFilter(wktPoint, radius);
    openSearchParser.populateGeospatial(
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
  public void populateNullGeospatial() {
    openSearchParser.populateGeospatial(
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
