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
package ddf.catalog.pubsub;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeospatialEvaluatorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeospatialEvaluatorTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBuildGeometry() {
        // String gmlText =
        // "<gml:Polygon xmlns:gml=\"http://www.opengis.net/gml\" gml:id=\"BGE-1\" srsName=\"http://metadata.dod.mil/mdr/ns/GSIP/crs/WGS84E_2D\">\n"
        // +
        // "<gml:exterior>\n" +
        // "<gml:LinearRing>\n" +
        // "<gml:pos>34.0 44.0</gml:pos>\n" +
        // "<gml:pos>33.0 44.0</gml:pos>\n" +
        // "<gml:pos>33.0 45.0</gml:pos>\n" +
        // "<gml:pos>34.0 45.0</gml:pos>\n" +
        // "<gml:pos>34.0 44.0</gml:pos>\n" +
        // "</gml:LinearRing>\n" +
        // "</gml:exterior>\n" +
        // "</gml:Polygon>";
        //
        // try
        // {
        // Geometry geometry = GeospatialEvaluator.buildGeometry( gmlText );
        // assertNotNull( geometry );
        // }
        // catch ( IOException e )
        // {
        // LOGGER.error( e.getMessage(), e );
        // fail( "Test failed with IOException" );
        // }
        // catch ( SAXException e )
        // {
        // LOGGER.error( e.getMessage(), e );
        // fail( "Test failed with SAXException" );
        // }
        // catch ( ParserConfigurationException e )
        // {
        // LOGGER.error( e.getMessage(), e );
        // fail( "Test failed with ParserConfigurationException" );
        // }
    }

}
