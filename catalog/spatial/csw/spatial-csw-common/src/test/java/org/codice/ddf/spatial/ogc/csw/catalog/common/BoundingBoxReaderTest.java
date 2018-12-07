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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.DomReader;
import java.io.IOException;
import java.util.Stack;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class BoundingBoxReaderTest {

  private static final transient Logger LOGGER =
      LoggerFactory.getLogger(BoundingBoxReaderTest.class);

  private static final String POLYGON_CONTROL_WKT_IN_LON_LAT =
      "POLYGON ((65.6272038662182 33.305863417212, 65.6272038662182 33.6653407061501, 65.7733371981862 33.6653407061501, 65.7733371981862 33.305863417212, 65.6272038662182 33.305863417212))";

  private static final String NON_JTS_FORMATTED_POLYGON_CONTROL_WKT_IN_LON_LAT =
      "POLYGON ((65.6272038662182 33.305863417212, 65.7733371981862 33.305863417212, 65.7733371981862 33.6653407061501, 65.6272038662182 33.6653407061501, 65.6272038662182 33.305863417212))";

  private static final String POLYGON_UTM_LON_LAT =
      "POLYGON ((29.999988636853967 9.999983148395557, 30.49995986771138 10.004117795867893, 30.499959660281664 10.00414490359446, 29.999988388080407 10.000010244696764, 29.999988636853967 9.999983148395557))";

  private static final String POINT_CONTROL_WKT_IN_LON_LAT =
      "POINT (65.6272038662182 33.305863417212)";

  /**
   * Verify that if given a BoundingBox with coords in LON/LAT that the resulting WKT is in LON/LAT.
   */
  @Test
  public void testGetWktBoundingBoxInLonLat()
      throws ParserConfigurationException, SAXException, IOException, CswException {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLonLat.xml");
    HierarchicalStreamReader hReader = new DomReader(doc);
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LON_LAT);

    // Perform Test
    String wktInLonLat = boundingBoxReader.getWkt();
    LOGGER.debug("WKT: {}", wktInLonLat);

    // Verify
    assertThat(wktInLonLat, is(NON_JTS_FORMATTED_POLYGON_CONTROL_WKT_IN_LON_LAT));
  }

  /**
   * Verify that if given a BoundingBox with coords in LAT/LON that the resulting WKT is in LON/LAT
   * (i.e., the coords are reversed).
   */
  @Test
  public void testGetWktBoundingBoxInLatLon()
      throws ParserConfigurationException, SAXException, IOException, CswException {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLatLon.xml");
    HierarchicalStreamReader hReader = new DomReader(doc);
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LAT_LON);

    // Perform Test
    String wktInLonLat = boundingBoxReader.getWkt();
    LOGGER.debug("WKT: {}", wktInLonLat);

    // Verify
    assertThat(wktInLonLat, is(POLYGON_CONTROL_WKT_IN_LON_LAT));
  }

  /**
   * Verify that if given a BoundingBox with coords in LON/LAT for a Point, i.e., both corners have
   * same exact Lon/Lat, that the resulting WKT is a POINT in LON/LAT.
   */
  @Test
  public void testGetWktBoundingBoxInLonLatForPoint()
      throws ParserConfigurationException, SAXException, IOException, CswException {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLonLatForPoint.xml");
    HierarchicalStreamReader hReader = new DomReader(doc);
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LON_LAT);

    // Perform Test
    String wktInLonLat = boundingBoxReader.getWkt();
    LOGGER.debug("WKT: {}", wktInLonLat);

    // Verify
    assertThat(wktInLonLat, is(POINT_CONTROL_WKT_IN_LON_LAT));
  }

  /**
   * Verify that if the reader is given something that isn't a BoundingBox, then an exception is
   * raised.
   */
  @Test(expected = CswException.class)
  public void testNonBoundingBox() throws CswException {
    HierarchicalStreamReader hReader = mock(HierarchicalStreamReader.class);
    when(hReader.getNodeName()).thenReturn("NOT_A_BOUNDING_BOX");
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LON_LAT);
    boundingBoxReader.getWkt();
  }

  /**
   * Verify that if the XML is missing the lower corner, the BoundingBoxReader will throw an
   * exception.
   */
  @Test(expected = CswException.class)
  public void testMissingLowerCorner() throws CswException {
    HierarchicalStreamReader reader = mock(HierarchicalStreamReader.class);
    Stack<String> boundingBoxNodes = new Stack<>();

    boundingBoxNodes.push("-2.228 51.126");
    boundingBoxNodes.push("UpperCorner");
    boundingBoxNodes.push("-6.171 44.792");
    boundingBoxNodes.push("MISSING LOWER CORNER");
    boundingBoxNodes.push("BoundingBox");
    boundingBoxNodes.push("BoundingBox");

    Answer<String> answer = invocationOnMock -> boundingBoxNodes.pop();

    when(reader.getNodeName()).thenAnswer(answer);
    when(reader.getValue()).thenAnswer(answer);

    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(reader, CswAxisOrder.LON_LAT);
    boundingBoxReader.getWkt();
  }

  /**
   * Verify that if the XML is missing the upper corner, the BoundingBoxReader will throw an
   * exception.
   */
  @Test(expected = CswException.class)
  public void testMissingUpperCorner() throws CswException {
    HierarchicalStreamReader reader = mock(HierarchicalStreamReader.class);
    Stack<String> boundingBoxNodes = new Stack<>();

    boundingBoxNodes.push("-2.228 51.126");
    boundingBoxNodes.push("MISSING UPPER CORNER");
    boundingBoxNodes.push("-6.171 44.792");
    boundingBoxNodes.push("LowerCorner");
    boundingBoxNodes.push("BoundingBox");
    boundingBoxNodes.push("BoundingBox");

    Answer<String> answer = invocationOnMock -> boundingBoxNodes.pop();

    when(reader.getNodeName()).thenAnswer(answer);
    when(reader.getValue()).thenAnswer(answer);

    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(reader, CswAxisOrder.LON_LAT);
    boundingBoxReader.getWkt();
  }

  @Test
  public void testJTSConverterEPSG4326LatLon() throws Exception {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLatLonEPSG4326.xml");
    HierarchicalStreamReader hReader = new DomReader(doc);
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LAT_LON);

    // Perform Test
    String wktInLonLat = boundingBoxReader.getWkt();
    LOGGER.debug("WKT: {}", wktInLonLat);

    // Verify
    assertThat(wktInLonLat, is(POLYGON_CONTROL_WKT_IN_LON_LAT));
  }

  @Test
  public void testJTSConverterEPSG4326LonLat() throws Exception {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse("src/test/resources/BoundingBoxInLonLatEPSG4326.xml");
    HierarchicalStreamReader hReader = new DomReader(doc);
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LON_LAT);

    // Perform Test
    String wktInLonLat = boundingBoxReader.getWkt();
    LOGGER.debug("WKT: {}", wktInLonLat);

    // Verify
    assertThat(wktInLonLat, is(NON_JTS_FORMATTED_POLYGON_CONTROL_WKT_IN_LON_LAT));
  }

  @Ignore // TODO Java 11: Remove this ignore when we've fixed the precision issues globally
  @Test
  public void testJTSConverterEPSG32636() throws Exception {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse("src/test/resources/BoundingBoxEPSG32636.xml");
    HierarchicalStreamReader hReader = new DomReader(doc);
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LAT_LON);

    // Perform Test
    String wktInLonLat = boundingBoxReader.getWkt();
    LOGGER.debug("WKT: {}", wktInLonLat);

    // Verify
    assertThat(wktInLonLat, is(POLYGON_UTM_LON_LAT));
  }

  @Test(expected = CswException.class)
  public void testJTSConverterBadCrs() throws Exception {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse("src/test/resources/BoundingBoxBadCrs.xml");
    HierarchicalStreamReader hReader = new DomReader(doc);
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LAT_LON);
    boundingBoxReader.getWkt();
  }

  @Test(expected = CswException.class)
  public void testJTSConverterBadLat() throws Exception {
    // Setup
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse("src/test/resources/BoundingBoxBadCoordinates.xml");
    HierarchicalStreamReader hReader = new DomReader(doc);
    BoundingBoxReader boundingBoxReader = new BoundingBoxReader(hReader, CswAxisOrder.LAT_LON);

    // Perform Test
    String wktInLonLat = boundingBoxReader.getWkt();
    LOGGER.debug("WKT: {}", wktInLonLat);

    // Verify
    assertThat(wktInLonLat, is(NON_JTS_FORMATTED_POLYGON_CONTROL_WKT_IN_LON_LAT));
  }
}
