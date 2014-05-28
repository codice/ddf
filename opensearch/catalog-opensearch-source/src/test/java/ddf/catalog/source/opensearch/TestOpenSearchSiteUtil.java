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

import ddf.catalog.data.Result;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.impl.QueryImpl;
import org.apache.cxf.jaxrs.client.WebClient;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;


public class TestOpenSearchSiteUtil {
    private final StringBuilder url = new StringBuilder(
            "http://localhost:8080/services/catalog/query");

    private static final Logger logger = LoggerFactory.getLogger(TestOpenSearchSiteUtil.class);

    @Test
    public void populateContextual() {
        String searchPhrase = "TestQuery123";
        assertTrue(url.indexOf(OpenSearchSiteUtil.SEARCH_TERMS) != -1);
        WebClient webClient = WebClient.create(url.toString());
        OpenSearchSiteUtil.populateContextual(webClient, searchPhrase, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        String urlStr = webClient.getCurrentURI().toString();
        assertTrue(urlStr.indexOf(searchPhrase) != -1);
        assertTrue(urlStr.indexOf("?"+OpenSearchSiteUtil.SEARCH_TERMS) != -1);
        try {
            new URL(urlStr.toString());
        } catch (MalformedURLException mue) {
            fail("URL is not valid: " + mue.getMessage());
        }
        logger.info("URL after contextual population: {}", urlStr.toString());
    }

    /**
     * Verify that passing in null will still remove the parameters from the URL.
     */
    @Test
    public void populateNullContextual() {
        WebClient webClient = WebClient.create(url.toString());
        OpenSearchSiteUtil.populateContextual(webClient, null, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        String urlStr = webClient.getCurrentURI().toString();
        assertTrue(urlStr.indexOf("?"+OpenSearchSiteUtil.SEARCH_TERMS) == -1);
    }

    @Test
    public void populateTemporal() {
        // TODO have actual set time strings to compare to
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        Date start = new Date(System.currentTimeMillis() - 10000000);
        Date end = new Date(System.currentTimeMillis());
        StringBuilder resultStr = new StringBuilder(url);
        TemporalFilter temporal = new TemporalFilter(start, end);

        WebClient webClient = WebClient.create(url.toString());
        OpenSearchSiteUtil.populateTemporal(webClient, temporal, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        String urlStr = webClient.getCurrentURI().toString();
        String startT = fmt.print(start.getTime());
        try {
            assertTrue(urlStr.indexOf(URLEncoder.encode(startT, "UTF-8")) != -1);
        } catch (UnsupportedEncodingException e) {
            fail(e.getMessage());
        }
        String endT = fmt.print(end.getTime());
        try {
            assertTrue(urlStr.indexOf(URLEncoder.encode(endT, "UTF-8")) != -1);
        } catch (UnsupportedEncodingException e) {
            fail(e.getMessage());
        }
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.TIME_START) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.TIME_END) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.TIME_NAME) == -1);
        try {
            new URL(resultStr.toString());
        } catch (MalformedURLException mue) {
            fail("URL is not valid: " + mue.getMessage());
        }
        logger.info("URL after temporal population: {}", resultStr.toString());
    }

    /**
     * Verify that passing in null will still remove the parameters from the URL.
     */
    @Test
    public void populateNullTemporal() {
        StringBuilder resultStr = new StringBuilder(url);
        WebClient webClient = WebClient.create(url.toString());
        OpenSearchSiteUtil.populateTemporal(webClient, null, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        String urlStr = webClient.getCurrentURI().toString();
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.TIME_START) == -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.TIME_END) == -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.TIME_NAME) == -1);
    }

    @Test
    public void populateSearchOptions() throws UnsupportedEncodingException {
        String maxResults = "2000";
        String timeout = "30000";
        //this wasn't url encoded in the previous test, should have been
        String sort = "date%3Adesc";
        SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
        Filter filter = mock(Filter.class);
        Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000);

        WebClient webClient = WebClient.create(url.toString());

        OpenSearchSiteUtil.populateSearchOptions(webClient, query, null, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        String urlStr = webClient.getCurrentURI().toString();
        assertTrue(urlStr.indexOf(maxResults) != -1);
        assertTrue(urlStr.indexOf(timeout) != -1);
        assertThat(urlStr, containsString(sort));

        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.COUNT) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.MAX_RESULTS) != -1);
        //src is handled when the params are added to the url
//        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.SRC) != -1);
//        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.USER_DN) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.MAX_TIMEOUT) != -1);
        //filter isn't even used, removed from test
//        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.FILTER) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.SORT) != -1);
    }

    /**
     * Verify that passing in null will still remove the parameters from the URL.
     */
    @Test
    public void populateNullSearchOptions() {
        WebClient webClient = WebClient.create(url.toString());
        OpenSearchSiteUtil.populateSearchOptions(webClient, null, null, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        String urlStr = webClient.getCurrentURI().toString();
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.COUNT) == -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.MAX_RESULTS) == -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.SRC) == -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.USER_DN) == -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.MAX_TIMEOUT) == -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.FILTER) == -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.SORT) == -1);
    }

    @Test
    public void convertWKTtoLatLonAry() {
        String lat = "43.25";
        String lon = "-123.45";
        String wktPoint = "POINT(" + lon + " " + lat + ")";
        String[] latLonAry = OpenSearchSiteUtil.createLatLonAryFromWKT(wktPoint);
        assertEquals(2, latLonAry.length);
        assertEquals(lon, latLonAry[0]);
        assertEquals(lat, latLonAry[1]);
    }

    @Test
    public void convertWKTtoPolyAry() {
        String lat[] = new String[] {"1", "1", "5", "5", "1"};
        String lon[] = new String[] {"1", "5", "5", "1", "1"};
        // TODO make the poly string programatically from the above arrays
        String wktPolygon = "POLYGON((1 1,5 1,5 5,1 5,1 1))";
        String[] polyAry = OpenSearchSiteUtil.createPolyAryFromWKT(wktPolygon);
        assertEquals(lat.length + lon.length, polyAry.length);
        for (int i = 0; i < polyAry.length; i++) {
            if (i % 2 == 0) {
                assertEquals(lon[i / 2], polyAry[i]);
            } else {
                assertEquals(lat[(int) Math.round((double) i / 2.0) - 1], polyAry[i]);
            }
        }
    }

    @Test
    public void convertWKTtoPolyArySpaces() {
        String lat[] = new String[] {"10", "30", "30", "10", "-10", "10"};
        String lon[] = new String[] {"0", "0", "20", "20", "10", "0"};
        String wktPolygon = "POLYGON((0 10, 0 30, 20 30, 20 10, 10 -10, 0 10))";
        String[] polyAry = OpenSearchSiteUtil.createPolyAryFromWKT(wktPolygon);
        assertEquals(lat.length + lon.length, polyAry.length);
        for (int i = 0; i < polyAry.length; i++) {
            if (i % 2 == 0) {
                assertEquals(lon[i / 2], polyAry[i]);
            } else {
                assertEquals(lat[(int) Math.round((double) i / 2.0) - 1], polyAry[i]);
            }
        }

    }

    @Test
    public void populatePolyGeospatial() throws Exception {
        String wktPolygon = "POLYGON((1 1,5 1,5 5,1 5,1 1))";
        String expectedStr = "1,1,1,5,5,5,5,1,1,1";

        SpatialFilter spatial = new SpatialFilter(wktPolygon);
        WebClient webClient = WebClient.create(url.toString());
        OpenSearchSiteUtil.populateGeospatial(webClient, spatial, false, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        String urlStr = webClient.getCurrentURI().toString();

        assertTrue(urlStr.indexOf(expectedStr) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.GEO_POLY) != -1);
    }

    @Test
    public void populateLatLonRadGeospatial() throws Exception {
        String lat = "43.25";
        String lon = "-123.45";
        String radius = "10000";
        String wktPoint = "POINT(" + lon + " " + lat + ")";

        SpatialDistanceFilter spatial = new SpatialDistanceFilter(wktPoint, radius);
        WebClient webClient = WebClient.create(url.toString());
        OpenSearchSiteUtil.populateGeospatial(webClient, spatial, false, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        String urlStr = webClient.getCurrentURI().toString();
        assertTrue(urlStr.indexOf(lat) != -1);
        assertTrue(urlStr.indexOf(lon) != -1);
        assertTrue(urlStr.indexOf(radius) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.GEO_LAT) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.GEO_LON) != -1);
        assertTrue(urlStr.indexOf(OpenSearchSiteUtil.GEO_RADIUS) != -1);
        try {
            new URL(urlStr.toString());
        } catch (MalformedURLException mue) {
            fail("URL is not valid: " + mue.getMessage());
        }
        logger.info("URL after lat lon geospatial population: {}", urlStr.toString());

    }

    /**
     * Verify that passing in null will still remove the parameters from the URL.
     */
    @Test
    public void populateNullGeospatial() throws Exception {
        SpatialDistanceFilter spatial = null;
        WebClient webClient = WebClient.create(url.toString());
        OpenSearchSiteUtil.populateGeospatial(webClient, spatial, true, Arrays.asList("q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort".split(",")));
        URI urlStr = webClient.getCurrentURI();
        assertTrue(urlStr.toString().indexOf(OpenSearchSiteUtil.GEO_LAT) == -1);
        assertTrue(urlStr.toString().indexOf(OpenSearchSiteUtil.GEO_LON) == -1);
        assertTrue(urlStr.toString().indexOf(OpenSearchSiteUtil.GEO_RADIUS) == -1);

    }

    @Test
    public void convertPointRadiusToBBox() {
        double lat = 30;
        double lon = 15;
        double radius = 200000;
        double[] bbox = OpenSearchSiteUtil.createBBoxFromPointRadius(lon, lat, radius);
        logger.info("minX = " + bbox[0]);
        assertEquals(3.3531, bbox[0], 0.0001);
        logger.info("minY = " + bbox[1]);
        assertEquals(28.2034, bbox[1], 0.0001);
        logger.info("maxX = " + bbox[2]);
        assertEquals(26.6468, bbox[2], 0.0001);
        logger.info("maxY = " + bbox[3]);
        assertEquals(31.7965, bbox[3], 0.0001);
    }

}
