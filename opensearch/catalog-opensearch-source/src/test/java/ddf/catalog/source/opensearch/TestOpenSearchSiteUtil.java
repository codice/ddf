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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.Result;
import ddf.catalog.filter.SortByImpl;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryImpl;

public class TestOpenSearchSiteUtil
{
    private final StringBuilder url = new StringBuilder(
        "http://localhost:8080/services/catalog/query?q={searchTerms}&amp;src={fs:routeTo?}&amp;mr={fs:maxResults?}&amp;count={count?}&amp;mt={fs:maxTimeout?}&amp;dn={idn:userDN?}&amp;lat={geo:lat?}&amp;lon={geo:lon?}&amp;radius={geo:radius?}&amp;bbox={geo:box?}&amp;polygon={geo:polygon?}&amp;dtstart={time:start?}&amp;dtend={time:end?}&amp;dateName={cat:dateName?}&amp;filter={fsa:filter?}&amp;sort={fsa:sort?}" );
    private Logger logger = Logger.getLogger( TestOpenSearchSiteUtil.class );


    @Test
    public void populateContextual()
    {
        String searchPhrase = "TestQuery123";
        StringBuilder resultStr = new StringBuilder( url );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.SEARCH_TERMS ) != -1 );
        resultStr = OpenSearchSiteUtil.populateContextual( resultStr, searchPhrase );
        assertTrue( resultStr.indexOf( searchPhrase ) != -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.SEARCH_TERMS ) == -1 );
        try
        {
            new URL( resultStr.toString() );
        }
        catch ( MalformedURLException mue )
        {
            fail( "URL is not valid: " + mue.getMessage() );
        }
        logger.info( "URL after contextual population: " + resultStr.toString() );
    }


    /**
     * Verify that passing in null will still remove the parameters from the
     * URL.
     */
    @Test
    public void populateNullContextual()
    {
        StringBuilder resultStr = new StringBuilder( url );
        resultStr = OpenSearchSiteUtil.populateContextual( resultStr, null );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.SEARCH_TERMS ) == -1 );
    }


    @Test
    public void populateTemporal()
    {
        // TODO have actual set time strings to compare to
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        Date start = new Date( System.currentTimeMillis() - 10000000 );
        Date end = new Date( System.currentTimeMillis() );
        StringBuilder resultStr = new StringBuilder( url );
        TemporalFilter temporal = new TemporalFilter(start, end);
        resultStr = OpenSearchSiteUtil.populateTemporal( resultStr, temporal );
        assertTrue( resultStr.indexOf( fmt.print( start.getTime() ) ) != -1 );
        assertTrue( resultStr.indexOf( fmt.print( end.getTime() ) ) != -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.TIME_START ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.TIME_END ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.TIME_NAME ) == -1 );
        try
        {
            new URL( resultStr.toString() );
        }
        catch ( MalformedURLException mue )
        {
            fail( "URL is not valid: " + mue.getMessage() );
        }
        logger.info( "URL after temporal population: " + resultStr.toString() );
    }


    /**
     * Verify that passing in null will still remove the parameters from the
     * URL.
     */
    @Test
    public void populateNullTemporal()
    {
        StringBuilder resultStr = new StringBuilder( url );
        resultStr = OpenSearchSiteUtil.populateTemporal( resultStr, null );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.TIME_START ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.TIME_END ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.TIME_NAME ) == -1 );
    }


    @Test
    public void populateSearchOptions() throws UnsupportedEncodingException
    {
        StringBuilder resultStr = new StringBuilder( url );
        String maxResults = "2000";
        String timeout = "30000";
        String userDN = "CN=Shaun Morris,OU=ISGS,O=LMCO,C=US";
        Subject user = new Subject();
        user.getPrincipals().add( new X500Principal( userDN ) );
        String userDNEncoded = URLEncoder.encode( userDN, "UTF-8" );
        String sort = "date:desc";
        SortBy sortBy = new SortByImpl(Result.TEMPORAL, SortOrder.DESCENDING);
        Filter filter = mock(Filter.class);
        Query query = new QueryImpl(filter, 0, 2000, sortBy, true, 30000); 

        resultStr = OpenSearchSiteUtil.populateSearchOptions( resultStr, query, user);
        assertTrue( resultStr.indexOf( maxResults ) != -1 );
        assertTrue( resultStr.indexOf( timeout ) != -1 );
        assertThat(resultStr.toString(), containsString(userDNEncoded));
        assertThat(resultStr.toString(), containsString(sort));

        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.COUNT ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.MAX_RESULTS ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.SRC ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.USER_DN ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.MAX_TIMEOUT ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.FILTER ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.SORT ) == -1 );
    }


    /**
     * Verify that passing in null will still remove the parameters from the
     * URL.
     */
    @Test
    public void populateNullSearchOptions()
    {
        StringBuilder resultStr = new StringBuilder( url );
        resultStr = OpenSearchSiteUtil.populateSearchOptions( resultStr, null, null );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.COUNT ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.MAX_RESULTS ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.SRC ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.USER_DN ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.MAX_TIMEOUT ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.FILTER ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.SORT ) == -1 );
    }


    @Test
    public void convertWKTtoLatLonAry()
    {
        String lat = "43.25";
        String lon = "-123.45";
        String wktPoint = "POINT(" + lon + " " + lat + ")";
        String[] latLonAry = OpenSearchSiteUtil.createLatLonAryFromWKT( wktPoint );
        assertEquals( 2, latLonAry.length );
        assertEquals( lon, latLonAry[0] );
        assertEquals( lat, latLonAry[1] );
    }


    @Test
    public void convertWKTtoPolyAry()
    {
        String lat[] = new String[]
        {
                "1", "1", "5", "5", "1"
        };
        String lon[] = new String[]
        {
                "1", "5", "5", "1", "1"
        };
        // TODO make the poly string programatically from the above arrays
        String wktPolygon = "POLYGON((1 1,5 1,5 5,1 5,1 1))";
        String[] polyAry = OpenSearchSiteUtil.createPolyAryFromWKT( wktPolygon );
        assertEquals( lat.length + lon.length, polyAry.length );
        for( int i = 0; i < polyAry.length; i++ )
        {
            if ( i % 2 == 0 )
            {
                assertEquals( lon[i / 2], polyAry[i] );
            }
            else
            {
                assertEquals( lat[(int) Math.round( (double) i / 2.0 ) - 1], polyAry[i] );
            }
        }
    }


    @Test
    public void convertWKTtoPolyArySpaces()
    {
        String lat[] = new String[]
        {
                "10", "30", "30", "10", "-10", "10"
        };
        String lon[] = new String[]
        {
                "0", "0", "20", "20", "10", "0"
        };
        String wktPolygon = "POLYGON((0 10, 0 30, 20 30, 20 10, 10 -10, 0 10))";
        String[] polyAry = OpenSearchSiteUtil.createPolyAryFromWKT( wktPolygon );
        assertEquals( lat.length + lon.length, polyAry.length );
        for( int i = 0; i < polyAry.length; i++ )
        {
            if ( i % 2 == 0 )
            {
                assertEquals( lon[i / 2], polyAry[i] );
            }
            else
            {
                assertEquals( lat[(int) Math.round( (double) i / 2.0 ) - 1], polyAry[i] );
            }
        }

    }


    @Test
    public void populatePolyGeospatial() throws Exception
    {
        String wktPolygon = "POLYGON((1 1,5 1,5 5,1 5,1 1))";
        String expectedStr = "1,1,1,5,5,5,5,1,1,1";
        StringBuilder resultStr = new StringBuilder( url );
        SpatialFilter spatial = new SpatialFilter(wktPolygon);
        resultStr = OpenSearchSiteUtil.populateGeospatial( resultStr, spatial, false );
        assertTrue( resultStr.indexOf( expectedStr ) != -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.GEO_POLY ) == -1 );
    }


    @Test
    public void populateLatLonRadGeospatial() throws Exception
    {
        String lat = "43.25";
        String lon = "-123.45";
        String radius = "10000";
        String wktPoint = "POINT(" + lon + " " + lat + ")";
        StringBuilder resultStr = new StringBuilder( url );

        SpatialDistanceFilter spatial = new SpatialDistanceFilter( wktPoint, radius );
        resultStr = OpenSearchSiteUtil.populateGeospatial( resultStr, spatial, false );
        assertTrue( resultStr.indexOf( lat ) != -1 );
        assertTrue( resultStr.indexOf( lon ) != -1 );
        assertTrue( resultStr.indexOf( radius ) != -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.GEO_LAT ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.GEO_LON ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.GEO_RADIUS ) == -1 );
        try
        {
            new URL( resultStr.toString() );
        }
        catch ( MalformedURLException mue )
        {
            fail( "URL is not valid: " + mue.getMessage() );
        }
        logger.info( "URL after lat lon geospatial population: " + resultStr.toString() );
     
    }


    /**
     * Verify that passing in null will still remove the parameters from the
     * URL.
     */
    @Test
    public void populateNullGeospatial() throws Exception
    {
        StringBuilder resultStr = new StringBuilder( url );
        SpatialDistanceFilter spatial = null;
        resultStr = OpenSearchSiteUtil.populateGeospatial( resultStr, spatial, true );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.GEO_LAT ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.GEO_LON ) == -1 );
        assertTrue( resultStr.indexOf( OpenSearchSiteUtil.GEO_RADIUS ) == -1 );
     
    }


    /**
     * Run all utility classes and verify that all of the parameters are gone.
     */
    @Test
    @Ignore
    public void populateAllNull() throws Exception
    {
        StringBuilder resultStr = new StringBuilder( url );
        SpatialDistanceFilter spatial = null;
        TemporalFilter temporal = null;
        resultStr = OpenSearchSiteUtil.populateContextual( resultStr, null );
        resultStr = OpenSearchSiteUtil.populateGeospatial( resultStr, spatial, true );
        resultStr = OpenSearchSiteUtil.populateTemporal( resultStr, temporal );
        resultStr = OpenSearchSiteUtil.populateSearchOptions(resultStr, null, null);
        assertTrue( resultStr.indexOf( "{" ) == -1 );
        assertTrue( resultStr.indexOf( "}" ) == -1 );
    }


    @Test
    public void convertPointRadiusToBBox()
    {
        double lat = 30;
        double lon = 15;
        double radius = 200000;
        double[] bbox = OpenSearchSiteUtil.createBBoxFromPointRadius( lon, lat, radius );
        System.out.println( "minX = " + bbox[0] );
        assertEquals( 3.3531, bbox[0], 0.0001 );
        System.out.println( "minY = " + bbox[1] );
        assertEquals( 28.2034, bbox[1], 0.0001 );
        System.out.println( "maxX = " + bbox[2] );
        assertEquals( 26.6468, bbox[2], 0.0001 );
        System.out.println( "maxY = " + bbox[3] );
        assertEquals( 31.7965, bbox[3], 0.0001 );
    }

}
