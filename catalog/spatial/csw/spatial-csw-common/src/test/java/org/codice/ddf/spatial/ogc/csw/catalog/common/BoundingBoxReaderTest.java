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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.DomReader;

public class BoundingBoxReaderTest {

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(BoundingBoxReaderTest.class);

    private static final String POLYGON_CONTROL_WKT_IN_LAT_LON = "POLYGON((33.305863417212 65.6272038662182, 33.6653407061501 65.6272038662182, 33.6653407061501 65.7733371981862, 33.305863417212 65.7733371981862, 33.305863417212 65.6272038662182))";
    private static final String POINT_CONTROL_WKT_IN_LAT_LON = "POINT(33.305863417212 65.6272038662182)";

    /**
     * Verify that if given a BoundingBox with coords in LAT/LON that the resulting WKT is in
     * LAT/LON.
     */
    @Test
    public void testGetWktBoundingBoxInLatLon() throws ParserConfigurationException, SAXException,
            IOException {
        // Setup
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLatLon.xml");
        HierarchicalStreamReader hReader = new DomReader(doc);
        BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, false);

        // Perform Test
        String wktInLatLon = boundingBoxReader.getWkt();
        LOGGER.debug("WKT: {}", wktInLatLon);

        // Verify
        assertThat(wktInLatLon, is(POLYGON_CONTROL_WKT_IN_LAT_LON));
    }

    /**
     * Verify that if given a BoundingBox with coords in LON/LAT that the resulting WKT is in
     * LAT/LON (i.e., the coords are reversed).
     */
    @Test
    public void testGetWktBoundingBoxInLonLat() throws ParserConfigurationException, SAXException,
            IOException {
        // Setup
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLonLat.xml");
        HierarchicalStreamReader hReader = new DomReader(doc);
        BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, true);

        // Perform Test
        String wktInLatLon = boundingBoxReader.getWkt();
        LOGGER.debug("WKT: {}", wktInLatLon);

        // Verify
        assertThat(wktInLatLon, is(POLYGON_CONTROL_WKT_IN_LAT_LON));
    }

    /**
     * Verify that if given a BoundingBox with coords in LON/LAT for a Point, i.e., both corners
     * have same exact LON/LAT, that the resulting WKT is a POINT in LAT/LON.
     */
    @Test
    public void testGetWktBoundingBoxInLatLonForPoint1() throws ParserConfigurationException,
            SAXException, IOException {
        // Setup
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLonLatForPoint.xml");
        HierarchicalStreamReader hReader = new DomReader(doc);
        BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, true);

        // Perform Test
        String wktInLatLon = boundingBoxReader.getWkt();
        LOGGER.debug("WKT: {}", wktInLatLon);

        // Verify
        assertThat(wktInLatLon, is(POINT_CONTROL_WKT_IN_LAT_LON));
    }

    /**
     * Verify that if given a BoundingBox with coords in LAT/LON for a Point, i.e., both corners
     * have same exact LON/LAT, that the resulting WKT is a POINT in LAT/LON.
     */
    @Test
    public void testGetWktBoundingBoxInLatLonForPoint2() throws ParserConfigurationException,
            SAXException, IOException {
        // Setup
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLatLonForPoint.xml");
        HierarchicalStreamReader hReader = new DomReader(doc);
        BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, false);

        // Perform Test
        String wktInLatLon = boundingBoxReader.getWkt();
        LOGGER.debug("WKT: {}", wktInLatLon);

        // Verify
        assertThat(wktInLatLon, is(POINT_CONTROL_WKT_IN_LAT_LON));
    }

}
