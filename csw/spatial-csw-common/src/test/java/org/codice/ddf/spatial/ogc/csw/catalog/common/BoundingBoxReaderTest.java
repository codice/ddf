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

    private static final String CONTROL_WKT_IN_LON_LAT = "POLYGON((65.6272038662182 33.305863417212, 65.7733371981862 33.305863417212, 65.7733371981862 33.6653407061501, 65.6272038662182 33.6653407061501, 65.6272038662182 33.305863417212))";

    /**
     * Verify that if given a BoundingBox with coords in LON/LAT that the resulting WKT is in
     * LON/LAT.
     */
    @Test
    public void testGetWkt_BoundingBoxInLonLat() throws ParserConfigurationException, SAXException,
        IOException {
        // Setup
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLonLat.xml");
        HierarchicalStreamReader hReader = new DomReader(doc);
        BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader,
                true);

        // Perform Test
        String wktInLonLat = boundingBoxReader.getWkt();
        LOGGER.debug("WKT: {}", wktInLonLat);

        // Verify
        assertThat(wktInLonLat, is(CONTROL_WKT_IN_LON_LAT));
    }

    /**
     * Verify that if given a BoundingBox with coords in LAT/LON that the resulting WKT is in
     * LON/LAT (ie. the coords are reversed).
     */
    @Test
    public void testGetWkt_BoundingBoxInLatLon() throws ParserConfigurationException, SAXException,
        IOException {
        // Setup
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLatLon.xml");
        HierarchicalStreamReader hReader = new DomReader(doc);
      BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader,
      false);

        // Perform Test
        String wktInLonLat = boundingBoxReader.getWkt();
        LOGGER.debug("WKT: {}", wktInLonLat);

        // Verify
        assertThat(wktInLonLat, is(CONTROL_WKT_IN_LON_LAT));
    }
}
