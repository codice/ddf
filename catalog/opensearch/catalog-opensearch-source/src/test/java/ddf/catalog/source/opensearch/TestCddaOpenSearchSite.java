/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package ddf.catalog.source.opensearch;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.filter.FilterTransformer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.SourceResponse;


public class TestCddaOpenSearchSite
{
    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(TestCddaOpenSearchSite.class));

    @Rule
    public MethodRule watchman = new TestWatchman()
    {
        public void starting( FrameworkMethod method )
        {
            logger.debug("***************************  STARTING: {}  **************************", method.getName());
        }

        public void finished( FrameworkMethod method )
        {
            logger.debug("***************************  END: {}  **************************", method.getName());
        }
    };

    private static String DEFAULT_OS_URL = "https://example.com?q=REPLACE_ME&src=&mr=&count=10&mt=30000&dn=&lat=&lon=&radius=&bbox=&polygon=&dtstart=&dtend=&dateName=&filter=&sort=relevance:desc";
    private static SecureRemoteConnection connection = new SecureRemoteConnectionImpl();

    /**
     * This test would have thrown an exception (cannot connect to service) if
     * the url was being called because no search phrase specified in
     * query.
     */
    @Test
    public void testNoSearchPhrase()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

            // Query has temporal filter, but no contextual filter with
            // non-empty search phrase
            TemporalFilter temporalFilter = new TemporalFilter(360000);
            query.addTemporalFilter(temporalFilter);

            Filter filter = query.getFilter();
            FilterTransformer transform = new FilterTransformer();
            transform.setIndentation(2);
            String filterXml = transform.transform(filter);
            logger.debug(filterXml);

            QueryRequest queryRequest = new QueryRequestImpl(query);

            SourceResponse responses = site.query(queryRequest);

            assertEquals(0, responses.getHits());

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * This test would have thrown an exception (cannot connect to service) if
     * the url was being called, but searchPhrase is empty string.
     */
    @Test
    public void testEmptySearchPhrase()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            String searchTerm = "";
            String selector = null;

            MockQuery query = new MockQuery();
            query.addContextualFilter(searchTerm, selector);

            Filter filter = query.getFilter();
            FilterTransformer transform = new FilterTransformer();
            transform.setIndentation(2);
            String filterXml = transform.transform(filter);
            logger.debug(filterXml);
            QueryRequest queryRequest = new QueryRequestImpl(query);

            SourceResponse responses = site.query(queryRequest);

            // should be zero
            assertEquals(0, responses.getHits());

        }
        catch (Exception e)
        {
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * This test makes sure that the if the site is not in a correct environment
     * it will be marked as not available.
     */
    @Test
    public void testSiteNotAvailable()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
            assertFalse(site.isAvailable());
        }
        catch (Exception e)
        {
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testContextualSearch_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            String searchTerm = "cat";
            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);
            query.addContextualFilter(searchTerm, selector);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", searchTerm);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with multiple words with no operators. Used to
     * test DDF-2139.
     */
    @Test
    public void testContextualSearchMultiWords()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            String searchTerm = "cat dog";
            String output = "cat+dog";
            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);
            query.addContextualFilter(searchTerm, selector);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", output);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with multiple words in quotes. Used to test
     * DDF-2139.
     */
    @Test
    public void testContextualSearchQuotedWords()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            String searchTerm = "\"cat dog\"";
            String output = URLEncoder.encode(searchTerm, "UTF-8");
            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);
            query.addContextualFilter(searchTerm, selector);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", output);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with two words AND'ed together. Used to test
     * DDF-2139.
     */
    @Test
    public void testSearchPhraseSingleAND()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            String searchTerm = "This";
            String searchTerm2 = "That";
            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);
            query.addContextualFilter(searchTerm, selector);
            query.addContextualFilter(searchTerm2, selector);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", searchTerm + "+AND+" + searchTerm2);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with multiple words AND'ed together. Used to
     * test DDF-2139.
     */
    @Test
    public void testSearchPhraseMultipleAND()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            String searchTerm[] = new String[]
            {
                    "Who", "What", "When", "Where", "Why"
            };
            String output = "Who+AND+What+AND+When+AND+Where+AND+Why";

            String selector = null;

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);
            for ( int i = 0; i < searchTerm.length; i++ )
            {
                query.addContextualFilter(searchTerm[i], selector);
            }

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", output);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with two words OR'ed together. Used to test
     * DDF-2139.
     */
    @Test
    public void testSearchPhraseSingleOR()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            String searchTerm = "This";
            String searchTerm2 = "That";

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);
            query.filter = MockQuery.filterFactory.or(
                MockQuery.filterFactory.like(MockQuery.filterFactory.property(Metacard.ANY_TEXT), searchTerm),
                MockQuery.filterFactory.like(MockQuery.filterFactory.property(Metacard.ANY_TEXT), searchTerm2));
            // query.addContextualFilter(searchTerm, selector);
            // query.filterFactory
            // uery.addContextualFilter(searchTerm2, selector);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", searchTerm + "+OR+" + searchTerm2);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * Tests output when dealing with multiple words OR'ed together. Used to
     * test DDF-2139.
     */
    @Test
    public void testSearchPhraseMultipleOr()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            String searchTerm[] = new String[]
            {
                    "Who", "What", "When", "Where", "Why"
            };
            List<Filter> properties = new ArrayList<Filter>(searchTerm.length);
            String output = "Who+OR+What+OR+When+OR+Where+OR+Why";

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);
            for ( int i = 0; i < searchTerm.length; i++ )
            {
                properties.add(MockQuery.filterFactory.like(MockQuery.filterFactory.property(Metacard.ANY_TEXT),
                    searchTerm[i]));
            }
            query.filter = MockQuery.filterFactory.or(properties);
            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", output);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testAbsoluteTemporalSearch_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            Calendar calendar = Calendar.getInstance();
            Date startDate = calendar.getTime();
            calendar.add(Calendar.DAY_OF_YEAR, +1);
            Date endDate = calendar.getTime();
            String start = reformatDate(startDate);
            String end = reformatDate(endDate);
            logger.debug("start = " + start + ",   end = " + end);
            query.addTemporalFilter(start, end, null);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", searchTerm);
            expectedUrl = expectedUrl.replaceAll("dtstart=", "dtstart=" + start);
            expectedUrl = expectedUrl.replaceAll("dtend=", "dtend=" + end);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testModifiedTemporalSearch_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            TemporalFilter temporalFilter = new TemporalFilter(360000);
            query.addTemporalFilter(temporalFilter);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", searchTerm);
            expectedUrl = expectedUrl.replaceAll("dtstart=", "dtstart=" + reformatDate(temporalFilter.getStartDate()));
            expectedUrl = expectedUrl.replaceAll("dtend=", "dtend=" + reformatDate(temporalFilter.getEndDate()));
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSpatialDistanceSearch_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String lon = "10";
            String lat = "20";
            String radius = "123456";
            query.addSpatialDistanceFilter(lon, lat, radius);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", searchTerm);
            expectedUrl = expectedUrl.replaceAll("lat=", "lat=" + Double.parseDouble(lat));
            expectedUrl = expectedUrl.replaceAll("lon=", "lon=" + Double.parseDouble(lon));
            expectedUrl = expectedUrl.replaceAll("radius=", "radius=" + Double.parseDouble(radius));
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSpatialSearch_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String polygonCoords = "0 10,0 30,20 30,20 10,0 10";
            String geometryWkt = "POLYGON((" + polygonCoords + "))";
            query.addSpatialFilter(geometryWkt);

            String url = site.createUrl(query, null);

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", searchTerm);

            // Convert WKT polygon coords, which are lon lat pairs,
            // into lat,lon data
            String polygonData = "";
            String[] lonLatPairs = polygonCoords.split(",");
            for ( int i = 0; i < lonLatPairs.length; i++ )
            {
                String[] coord = lonLatPairs[i].split(" ");
                double lon = Double.parseDouble(coord[0]);
                double lat = Double.parseDouble(coord[1]);
                polygonData += lat + "," + lon;
                if (i != (lonLatPairs.length - 1))
                {
                    polygonData += ",";
                }
            }
            expectedUrl = expectedUrl.replaceAll("polygon=", "polygon=" + polygonData);

            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    /**
     * This test verifies that POINT geometry is not supported by the site
     * for spatial searches. So despite a spatial filter being created in the
     * query, the site generates a URL without any geometry parameters
     * because spatial searches only support POLYGON, not POINT, WKTs.
     */
    @Test
    public void testGeometryPointSearch_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String geometryWkt = "POINT(10 20)";
            query.addSpatialFilter(geometryWkt);

            String url = site.createUrl(query, null);

            Map<String, String> urlParams = urlToMap(url);
            assertNull(urlParams.get("geometry"));

            String expectedUrl = DEFAULT_OS_URL.replaceAll("REPLACE_ME", searchTerm);
            assertEquals(expectedUrl, url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSpatialDistanceSearch_BBoxConversion_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
            site.setShouldConvertToBBox(true);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String lon = "10";
            String lat = "20";
            String radius = "123456";
            query.addSpatialDistanceFilter(lon, lat, radius);

            String url = site.createUrl(query, null);

            Map<String, String> urlParams = urlToMap(url);
            assertNull(urlParams.get("lat"));
            assertNull(urlParams.get("lon"));
            assertNull(urlParams.get("radius"));
            assertNull(urlParams.get("polygon"));
            assertNotNull(urlParams.get("bbox"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSpatialSearch_BBoxConversion_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
            site.setShouldConvertToBBox(true);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

            String searchTerm = "Iran";
            query.addContextualFilter(searchTerm, null);

            String polygonCoords = "0 10,0 30,20 30,20 10,0 10";
            String geometryWkt = "POLYGON((" + polygonCoords + "))";
            query.addSpatialFilter(geometryWkt);

            String url = site.createUrl(query, null);

            Map<String, String> urlParams = urlToMap(url);
            assertNull(urlParams.get("lat"));
            assertNull(urlParams.get("lon"));
            assertNull(urlParams.get("radius"));
            assertNull(urlParams.get("polygon"));
            assertNotNull(urlParams.get("bbox"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testContextualTemporalSpatialSearch_Url()
    {
        try
        {
            CddaOpenSearchSite site = new CddaOpenSearchSite(connection);

            MockQuery query = new MockQuery(null, 0, 10, "relevance", SortOrder.DESCENDING, (long) 30000);

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
            logger.debug("start = " + start + ",   end = " + end);
            query.addTemporalFilter(start, end, null);

            String url = site.createUrl(query, null);

            // Map<String, String> urlParams = urlToMap( url );
            // assertNull( urlParams.get( "lat" ) );
            // assertNull( urlParams.get( "lon" ) );
            // assertNull( urlParams.get( "radius" ) );
            // assertNull( urlParams.get( "polygon" ) );
            // assertNotNull( urlParams.get( "bbox" ) );
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Got an exception: " + e.getMessage());
        }
    }

    @Test
    public void testEndpointUrl_SrcDefault() throws Exception
    {
        CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
        String endpointUrl = "http://example.com?q={searchTerms}&src={fs:routeTo?}&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}";
        site.setLocalQueryOnly(true);
        site.setEndpointUrl(endpointUrl);
        site.configureEndpointUrl();
        String updatedEndpointUrl = site.getEndpointUrl();
        assertTrue(updatedEndpointUrl.contains("src=local"));
    }

    @Test
    public void testEndpointUrl_SrcLocal() throws Exception
    {
        CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
        String endpointUrl = "http://example.com?q={searchTerms}&src=local&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}";
        site.setLocalQueryOnly(true);
        site.setEndpointUrl(endpointUrl);
        site.configureEndpointUrl();
        String updatedEndpointUrl = site.getEndpointUrl();
        assertTrue(updatedEndpointUrl.contains("src=local"));
    }

    @Test
    public void testEndpointUrl_SrcSiteName() throws Exception
    {
        CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
        String endpointUrl = "http://example.com?q={searchTerms}&src=ABC&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}";
        site.setLocalQueryOnly(true);
        site.setEndpointUrl(endpointUrl);
        site.configureEndpointUrl();
        String updatedEndpointUrl = site.getEndpointUrl();
        assertTrue(updatedEndpointUrl.contains("src=local"));
    }

    @Test
    public void testEndpointUrl_SrcEmpty() throws Exception
    {
        CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
        String endpointUrl = "http://example.com?q={searchTerms}&src=&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}";
        site.setLocalQueryOnly(true);
        site.setEndpointUrl(endpointUrl);
        site.configureEndpointUrl();
        String updatedEndpointUrl = site.getEndpointUrl();
        assertTrue(updatedEndpointUrl.contains("src=local"));
    }

    @Test
    public void testEndpointUrl_SrcNotSpecified() throws Exception
    {
        CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
        String endpointUrl = "http://example.com?q={searchTerms}&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}";
        site.setLocalQueryOnly(true);
        site.setEndpointUrl(endpointUrl);
        site.configureEndpointUrl();
        String updatedEndpointUrl = site.getEndpointUrl();
        assertTrue(updatedEndpointUrl.contains("src=local"));
    }

    @Test
    public void testEndpointUrl_NotLocalQuery_SrcLocal() throws Exception
    {
        CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
        String endpointUrl = "http://example.com?q={searchTerms}&src=local&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}";
        System.out.println("endpointUrl = " + endpointUrl);
        site.setLocalQueryOnly(false);
        site.setEndpointUrl(endpointUrl);
        site.configureEndpointUrl();
        String updatedEndpointUrl = site.getEndpointUrl();
        System.out.println("updatedEndpointUrl = " + updatedEndpointUrl);
        assertTrue(updatedEndpointUrl.contains("src={fs:routeTo?}"));
    }

    @Test
    public void testEndpointUrl_NotLocalQuery_SrcEmpty() throws Exception
    {
        CddaOpenSearchSite site = new CddaOpenSearchSite(connection);
        String endpointUrl = "http://example.com?q={searchTerms}&src=&mr={fs:maxResults?}&count={count?}&mt={fs:maxTimeout?}&dn={idn:userDN?}&lat={geo:lat?}&lon={geo:lon?}&radius={geo:radius?}&bbox={geo:box?}&polygon={geo:polygon?}&dtstart={time:start?}&dtend={time:end?}&dateName={cat:dateName?}&filter={fsa:filter?}&sort={fsa:sort?}";
        site.setLocalQueryOnly(false);
        site.setEndpointUrl(endpointUrl);
        site.configureEndpointUrl();
        String updatedEndpointUrl = site.getEndpointUrl();
        assertTrue(updatedEndpointUrl.contains("src=&"));
    }

    private String reformatDate( Date date )
    {
        // Reformat date into original format that was used to create filter
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
        String formattedDate = dateFormatter.format(date);

        // Add colon in GMT offset, e.g., -07:00 vs. -0700
        StringBuffer sb = new StringBuffer(formattedDate);
        sb.insert(formattedDate.length() - 2, ":");
        formattedDate = sb.toString();

        logger.debug("formattedDate = " + formattedDate.toString());

        return formattedDate;
    }

    private Map<String, String> urlToMap( String url )
    {
        HashMap<String, String> map = new HashMap<String, String>();

        String[] urlParts = url.split("\\?");
        String[] urlParams = urlParts[1].split("\\&");
        for ( String param : urlParams )
        {
            String[] paramParts = param.split("=");
            if (paramParts.length > 1)
            {
                // logger.debug( "param name:   " + paramParts[0] +
                // ",   value = " + paramParts[1] );
                map.put(paramParts[0], paramParts[1]);
            }
            else
            {
                // logger.debug( "param name:   " + paramParts[0] );
                map.put(paramParts[0], null);
            }
        }

        return map;
    }
}
