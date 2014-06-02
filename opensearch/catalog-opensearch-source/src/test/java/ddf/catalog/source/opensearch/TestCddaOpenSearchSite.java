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

package ddf.catalog.source.opensearch;

import ddf.catalog.data.Metacard;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.geotools.filter.FilterTransformer;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestCddaOpenSearchSite {
    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(TestCddaOpenSearchSite.class));

    private static final String UNEXPECTED_EXCEPTION_MSG = "unexpected expception";

    @Rule
    public MethodRule watchman = new TestWatchman() {
        public void starting(FrameworkMethod method) {
            LOGGER.debug("***************************  STARTING: {}  **************************",
                    method.getName());
        }

        public void finished(FrameworkMethod method) {
            LOGGER.debug("***************************  END: {}  **************************",
                    method.getName());
        }
    };

    /**
     * This test would have thrown an exception (cannot connect to service) if the url was being
     * called because no search phrase specified in query.
     */
    @Test
    public void testNoSearchPhrase() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            // Query has temporal filter, but no contextual filter with
            // non-empty search phrase
            TemporalFilter temporalFilter = new TemporalFilter(360000);
            query.addTemporalFilter(temporalFilter);

            Filter filter = query.getFilter();
            FilterTransformer transform = new FilterTransformer();
            transform.setIndentation(2);
            String filterXml = transform.transform(filter);
            LOGGER.debug(filterXml);

            QueryRequest queryRequest = new QueryRequestImpl(query);

            SourceResponse responses = site.query(queryRequest);

            assertEquals(0, responses.getHits());

        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * This test would have thrown an exception (cannot connect to service) if the url was being
     * called, but searchPhrase is empty string.
     */
    @Test
    public void testEmptySearchPhrase() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            String searchTerm = "";
            String selector = null;

            MockQuery query = new MockQuery();
            query.addContextualFilter(searchTerm, selector);

            Filter filter = query.getFilter();
            FilterTransformer transform = new FilterTransformer();
            transform.setIndentation(2);
            String filterXml = transform.transform(filter);
            LOGGER.debug(filterXml);
            QueryRequest queryRequest = new QueryRequestImpl(query);

            SourceResponse responses = site.query(queryRequest);

            // should be zero
            assertEquals(0, responses.getHits());

        } catch (Exception e) {
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testContextualSearch_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            String searchTerm = "cat";
            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);
            query.addContextualFilter(searchTerm, selector);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains(URLEncoder.encode(searchTerm, "UTF-8")));
        } catch (Exception e) {
            LOGGER.error("unexpected exception", e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    private CddaOpenSearchSite getCddaOpenSearchSite() throws UnsupportedQueryException {
        String endpointUrl = "http://example.com";
        CddaOpenSearchSite site = new CddaOpenSearchSite();
        OpenSearchConnection connection = new OpenSearchConnection(endpointUrl, null, null, null, null, null, null, null, null);
        site.openSearchConnection = connection;
        site.setParameters(Arrays.asList(
                "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                        .split(",")
        ));
        return site;
    }

    /**
     * Tests output when dealing with multiple words with no operators. Used to test DDF-2139.
     */
    @Test
    public void testContextualSearchMultiWords() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            String searchTerm = "cat dog";
            String output = "cat+dog";
            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);
            query.addContextualFilter(searchTerm, selector);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains(URLEncoder.encode(output, "UTF-8")));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with multiple words in quotes. Used to test DDF-2139.
     */
    @Test
    public void testContextualSearchQuotedWords() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            String searchTerm = "\"cat dog\"";
            String queryTerm = "\"cat+dog\"";
            String output = URLEncoder.encode(queryTerm, "UTF-8");
            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);
            query.addContextualFilter(searchTerm, selector);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains(output));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with two words AND'ed together. Used to test DDF-2139.
     */
    @Test
    public void testSearchPhraseSingleAND() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            String searchTerm = "This";
            String searchTerm2 = "That";
            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);
            query.addContextualFilter(searchTerm, selector);
            query.addContextualFilter(searchTerm2, selector);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains(URLEncoder.encode(searchTerm + "+AND+"+ searchTerm2, "UTF-8")));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with multiple words AND'ed together. Used to test DDF-2139.
     */
    @Test
    public void testSearchPhraseMultipleAND() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            String searchTerm[] = new String[] {"Who", "What", "When", "Where", "Why"};
            String output = "Who+AND+What+AND+When+AND+Where+AND+Why";

            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);
            for (int i = 0; i < searchTerm.length; i++) {
                query.addContextualFilter(searchTerm[i], selector);
            }

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains(URLEncoder.encode(output, "UTF-8")));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with two words OR'ed together. Used to test DDF-2139.
     */
    @Test
    public void testSearchPhraseSingleOR() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            String searchTerm = "This";
            String searchTerm2 = "That";

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);
            query.filter = MockQuery.filterFactory.or(
                    MockQuery.filterFactory.like(
                            MockQuery.filterFactory.property(Metacard.ANY_TEXT), searchTerm),
                    MockQuery.filterFactory.like(
                            MockQuery.filterFactory.property(Metacard.ANY_TEXT), searchTerm2));

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains(URLEncoder.encode(searchTerm + "+OR+"+ searchTerm2, "UTF-8")));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with multiple words OR'ed together. Used to test DDF-2139.
     */
    @Test
    public void testSearchPhraseMultipleOr() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            String searchTerm[] = new String[] {"Who", "What", "When", "Where", "Why"};
            List<Filter> properties = new ArrayList<Filter>(searchTerm.length);
            String output = "Who+OR+What+OR+When+OR+Where+OR+Why";

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);
            for (int i = 0; i < searchTerm.length; i++) {
                properties.add(MockQuery.filterFactory.like(
                        MockQuery.filterFactory.property(Metacard.ANY_TEXT), searchTerm[i]));
            }
            query.filter = MockQuery.filterFactory.or(properties);
            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains(URLEncoder.encode(output, "UTF-8")));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testAbsoluteTemporalSearch_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            Calendar calendar = Calendar.getInstance();
            Date startDate = calendar.getTime();
            calendar.add(Calendar.DAY_OF_YEAR, +1);
            Date endDate = calendar.getTime();
            String start = reformatDate(startDate);
            String end = reformatDate(endDate);
            LOGGER.debug("start = " + start + ",   end = " + end);
            query.addTemporalFilter(start, end, null);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains("dtstart"));
            assertTrue(urlStr.contains("dtend"));

            assertTrue(urlStr.contains(URLEncoder.encode(start, "UTF-8")));
            assertTrue(urlStr.contains(URLEncoder.encode(end, "UTF-8")));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testModifiedTemporalSearch_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            TemporalFilter temporalFilter = new TemporalFilter(360000);
            query.addTemporalFilter(temporalFilter);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains("dtstart"));
            assertTrue(urlStr.contains("dtend"));

            assertTrue(urlStr.contains(URLEncoder.encode(
                    reformatDate(temporalFilter.getStartDate()), "UTF-8")));
            assertTrue(urlStr.contains(URLEncoder.encode(reformatDate(temporalFilter.getEndDate()),
                    "UTF-8")));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSpatialDistanceSearch_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String lon = "10";
            String lat = "20";
            String radius = "123456";
            query.addSpatialDistanceFilter(lon, lat, radius);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(urlStr.contains("lat"));
            assertTrue(urlStr.contains("lon"));
            assertTrue(urlStr.contains("radius"));

            assertTrue(urlStr.contains(String.valueOf(Double.parseDouble(lat))));
            assertTrue(urlStr.contains(String.valueOf(Double.parseDouble(lon))));
            assertTrue(urlStr.contains(String.valueOf(Double.parseDouble(radius))));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSpatialSearch_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String polygonCoords = "0 10,0 30,20 30,20 10,0 10";
            String geometryWkt = "POLYGON((" + polygonCoords + "))";
            query.addSpatialFilter(geometryWkt);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            // Convert WKT polygon coords, which are lon lat pairs,
            // into lat,lon data
            String polygonData = "";
            String[] lonLatPairs = polygonCoords.split(",");
            for (int i = 0; i < lonLatPairs.length; i++) {
                String[] coord = lonLatPairs[i].split(" ");
                double lon = Double.parseDouble(coord[0]);
                double lat = Double.parseDouble(coord[1]);
                polygonData += lat + "," + lon;
                if (i != (lonLatPairs.length - 1)) {
                    polygonData += ",";
                }
            }

            assertTrue(urlStr.contains("polygon=" + polygonData));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * This test verifies that POINT geometry is not supported by the site for spatial searches. So
     * despite a spatial filter being created in the query, the site generates a URL without any
     * geometry parameters because spatial searches only support POLYGON, not POINT, WKTs.
     */
    @Test
    public void testGeometryPointSearch_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();
            site.setShouldConvertToBBox(true);

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String geometryWkt = "POINT(10 20)";
            query.addSpatialFilter(geometryWkt);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(!urlStr.contains("geometry"));
            assertTrue(!urlStr.contains("lat"));
            assertTrue(!urlStr.contains("lon"));
            assertTrue(!urlStr.contains("polygon"));
            assertTrue(!urlStr.contains("radius"));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSpatialDistanceSearch_BBoxConversion_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();
            site.setShouldConvertToBBox(true);

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String lon = "10";
            String lat = "20";
            String radius = "123456";
            query.addSpatialDistanceFilter(lon, lat, radius);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(!urlStr.contains("lat"));
            assertTrue(!urlStr.contains("lon"));
            assertTrue(!urlStr.contains("radius"));
            assertTrue(!urlStr.contains("polygon"));
            assertTrue(urlStr.contains("bbox"));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSpatialSearch_BBoxConversion_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();
            site.setShouldConvertToBBox(true);

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String polygonCoords = "0 10,0 30,20 30,20 10,0 10";
            String geometryWkt = "POLYGON((" + polygonCoords + "))";
            query.addSpatialFilter(geometryWkt);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(!urlStr.contains("lat"));
            assertTrue(!urlStr.contains("lon"));
            assertTrue(!urlStr.contains("radius"));
            assertTrue(!urlStr.contains("polygon"));
            assertTrue(urlStr.contains("bbox"));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testContextualTemporalSpatialSearch_Url() {
        try {
            CddaOpenSearchSite site = getCddaOpenSearchSite();
            site.setShouldConvertToBBox(false);

            WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                    (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String polygonCoords = "0 10,0 30,20 30,20 10,0 10";
            String geometryWkt = "POLYGON((" + polygonCoords + "))";
            query.addSpatialFilter(geometryWkt);

            Calendar calendar = Calendar.getInstance();
            Date startDate = calendar.getTime();
            calendar.add(Calendar.DAY_OF_YEAR, +1);
            Date endDate = calendar.getTime();
            String start = reformatDate(startDate);
            String end = reformatDate(endDate);
            LOGGER.debug("start = " + start + ",   end = " + end);
            query.addTemporalFilter(start, end, null);

            site.setOpenSearchParameters(query, null, webClient);
            String urlStr = webClient.getCurrentURI().toString();

            assertTrue(!urlStr.contains("lat"));
            assertTrue(!urlStr.contains("lon"));
            assertTrue(!urlStr.contains("radius"));
            assertTrue(urlStr.contains("polygon"));
            assertTrue(!urlStr.contains("bbox"));
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION_MSG, e);
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testEndpointUrl_SrcDefault() throws Exception {
        CddaOpenSearchSite site = getCddaOpenSearchSite();
        site.setShouldConvertToBBox(false);

        WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();
        site.setLocalQueryOnly(true);

        MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                (long) 30000);

        String searchTerm = "Iran";
        query.addContextualFilter(searchTerm, null);

        site.setOpenSearchParameters(query, null, webClient);

        String updatedEndpointUrl = webClient.getCurrentURI().toString();
        assertTrue(updatedEndpointUrl.contains("src=local"));
    }

    @Test
    public void testEndpointUrl_SrcEmpty() throws Exception {
        String endpointUrl = "http://example.com";
        CddaOpenSearchSite site = new CddaOpenSearchSite();
        OpenSearchConnection connection = new OpenSearchConnection(endpointUrl, null, null, null, null, null, null, null, null);
        site.openSearchConnection = connection;
        site.setParameters(Arrays.asList(
                "q,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                        .split(",")
        ));
        site.setShouldConvertToBBox(false);

        WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();        site.setLocalQueryOnly(true);

        MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                (long) 30000);

        String searchTerm = "Iran";
        query.addContextualFilter(searchTerm, null);

        site.setOpenSearchParameters(query, null, webClient);

        String updatedEndpointUrl = webClient.getCurrentURI().toString();
        assertTrue(updatedEndpointUrl.contains("src=local"));
    }

    @Test
    public void testEndpointUrl_NotLocalQuery_SrcEmpty() throws Exception {
        String endpointUrl = "http://example.com";
        CddaOpenSearchSite site = new CddaOpenSearchSite();
        OpenSearchConnection connection = new OpenSearchConnection(endpointUrl, null, null, null, null, null, null, null, null);
        site.openSearchConnection = connection;
        site.setParameters(Arrays.asList(
                "q,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                        .split(",")
        ));
        site.setShouldConvertToBBox(false);

        WebClient webClient = site.openSearchConnection.getOpenSearchWebClient();        site.setLocalQueryOnly(
                true);

        MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING,
                (long) 30000);

        String searchTerm = "Iran";
        query.addContextualFilter(searchTerm, null);

        site.setLocalQueryOnly(false);
        site.setOpenSearchParameters(query, null, webClient);

        String updatedEndpointUrl = webClient.getCurrentURI().toString();
        assertTrue(updatedEndpointUrl.contains("src"));
    }

    private String reformatDate(Date date) {
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String formattedDate = fmt.print(date.getTime());

        LOGGER.debug("formattedDate = {}", formattedDate);
        return formattedDate;
    }

    private Map<String, String> urlToMap(String url) {
        HashMap<String, String> map = new HashMap<String, String>();

        String[] urlParts = url.split("\\?");
        String[] urlParams = urlParts[1].split("\\&");
        for (String param : urlParams) {
            String[] paramParts = param.split("=");
            if (paramParts.length > 1) {
                // logger.debug( "param name:   " + paramParts[0] +
                // ",   value = " + paramParts[1] );
                map.put(paramParts[0], paramParts[1]);
            } else {
                // logger.debug( "param name:   " + paramParts[0] );
                map.put(paramParts[0], null);
            }
        }

        return map;
    }
}
