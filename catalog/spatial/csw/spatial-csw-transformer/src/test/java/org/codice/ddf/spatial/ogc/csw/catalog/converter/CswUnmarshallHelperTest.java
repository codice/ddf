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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import ddf.catalog.data.AttributeType;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.hamcrest.Matcher;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class CswUnmarshallHelperTest {

  private static final String TEST_BOUNDING_BOX =
      "<ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
          + " <ows:LowerCorner>-6.171 44.792</ows:LowerCorner>\n"
          + " <ows:UpperCorner>-2.228 51.126</ows:UpperCorner>\n"
          + "</ows:BoundingBox>";

  private Map<AttributeType.AttributeFormat, Matcher> matcherMap;

  private Map<AttributeType.AttributeFormat, String> valueMap;

  private DateFormat dateFormat;

  @Before
  public void setUp() {
    dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    matcherMap = new HashMap<>();
    matcherMap.put(AttributeType.AttributeFormat.BOOLEAN, is(Boolean.class));
    matcherMap.put(AttributeType.AttributeFormat.DOUBLE, is(Double.class));
    matcherMap.put(AttributeType.AttributeFormat.FLOAT, is(Float.class));
    matcherMap.put(AttributeType.AttributeFormat.INTEGER, is(Integer.class));
    matcherMap.put(AttributeType.AttributeFormat.LONG, is(Long.class));
    matcherMap.put(AttributeType.AttributeFormat.SHORT, is(Short.class));
    matcherMap.put(AttributeType.AttributeFormat.XML, is(String.class));
    matcherMap.put(AttributeType.AttributeFormat.STRING, is(String.class));
    matcherMap.put(AttributeType.AttributeFormat.DATE, is(Date.class));
    matcherMap.put(AttributeType.AttributeFormat.OBJECT, nullValue());

    valueMap = new HashMap<>();
    valueMap.put(AttributeType.AttributeFormat.BOOLEAN, "true");
    valueMap.put(AttributeType.AttributeFormat.DOUBLE, "232.212332443523");
    valueMap.put(AttributeType.AttributeFormat.FLOAT, "342344.23445");
    valueMap.put(AttributeType.AttributeFormat.INTEGER, "101");
    valueMap.put(AttributeType.AttributeFormat.LONG, "101");
    valueMap.put(AttributeType.AttributeFormat.SHORT, "2");
    valueMap.put(AttributeType.AttributeFormat.XML, "<testXml>test</testXml>");
    valueMap.put(AttributeType.AttributeFormat.STRING, "ABCDEFG");
    valueMap.put(AttributeType.AttributeFormat.DATE, "2015-01-01T13:00:00.001");
    valueMap.put(AttributeType.AttributeFormat.OBJECT, null);
  }

  @Test
  public void testConvertStringValueToMetacardValue() {
    valueMap.put(AttributeType.AttributeFormat.BINARY, null);
    valueMap.put(AttributeType.AttributeFormat.GEOMETRY, null);

    matcherMap.put(AttributeType.AttributeFormat.BINARY, nullValue());
    matcherMap.put(AttributeType.AttributeFormat.GEOMETRY, nullValue());

    Serializable ser = CswUnmarshallHelper.convertStringValueToMetacardValue(null, "XYZ");
    assertThat(ser, nullValue());

    AttributeType.AttributeFormat[] attributeFormats = AttributeType.AttributeFormat.values();

    for (AttributeType.AttributeFormat attributeFormat : attributeFormats) {
      Matcher m = matcherMap.get(attributeFormat);
      String value = valueMap.get(attributeFormat);
      ser = CswUnmarshallHelper.convertStringValueToMetacardValue(attributeFormat, value);
      assertThat(ser, m);
    }
  }

  @Test
  public void testConvertRecordPropertyToMetacardAttribute() {
    valueMap.put(AttributeType.AttributeFormat.BINARY, "TEST_BINARY");
    valueMap.put(AttributeType.AttributeFormat.GEOMETRY, TEST_BOUNDING_BOX);

    matcherMap.put(AttributeType.AttributeFormat.BINARY, notNullValue());
    matcherMap.put(
        AttributeType.AttributeFormat.GEOMETRY,
        is(
            "POLYGON ((44.792 -6.171, 44.792 -2.228, 51.126 -2.228, 51.126 -6.171, 44.792 -6.171))"));

    AttributeType.AttributeFormat[] attributeFormats = AttributeType.AttributeFormat.values();

    for (AttributeType.AttributeFormat attributeFormat : attributeFormats) {
      HierarchicalStreamReader reader = getReader(attributeFormat);

      Serializable ser =
          CswUnmarshallHelper.convertRecordPropertyToMetacardAttribute(
              attributeFormat, reader, CswAxisOrder.LAT_LON);

      Matcher m = matcherMap.get(attributeFormat);
      assertThat(ser, m);
    }
  }

  private HierarchicalStreamReader getReader(AttributeType.AttributeFormat attributeFormat) {
    HierarchicalStreamReader reader = mock(HierarchicalStreamReader.class);

    if (attributeFormat.equals(AttributeType.AttributeFormat.GEOMETRY)) {
      Stack<String> boundingBoxNodes = new Stack<>();

      boundingBoxNodes.push(valueMap.get(attributeFormat));
      boundingBoxNodes.push("-2.228 51.126");
      boundingBoxNodes.push("UpperCorner");
      boundingBoxNodes.push("-6.171 44.792");
      boundingBoxNodes.push("LowerCorner");
      boundingBoxNodes.push("BoundingBox");
      boundingBoxNodes.push("BoundingBox");
      boundingBoxNodes.push("BoundingBox");

      Answer<String> answer = invocationOnMock -> boundingBoxNodes.pop();

      when(reader.getNodeName()).thenAnswer(answer);
      when(reader.getValue()).thenAnswer(answer);
    } else {
      when(reader.getValue()).thenReturn(valueMap.get(attributeFormat));
    }
    return reader;
  }

  @Test
  public void testConvertToDate() {
    LocalDate localDate = new LocalDate();
    Date testDate = localDate.toDate();
    String dateString = DateFormat.getDateInstance().format(testDate);
    Date result = CswUnmarshallHelper.convertToDate(dateString);
    assertThat(result, is(testDate));

    dateString = dateFormat.format(testDate);
    LocalDate localResult = LocalDate.fromDateFields(CswUnmarshallHelper.convertToDate(dateString));
    assertThat(localResult, is(localDate));

    dateString = ISODateTimeFormat.basicOrdinalDateTime().print(localDate);
    localResult = LocalDate.fromDateFields(CswUnmarshallHelper.convertToDate(dateString));
    assertThat(localResult, is(localDate));
  }

  @Test
  public void testConvertGYearToDate() throws Exception {
    String gYear = "2011";
    String testDate = "2011-01-01";
    Date expectedDate = dateFormat.parse(testDate);
    Date result = CswUnmarshallHelper.convertToDate(gYear);
    assertThat(result, is(expectedDate));
  }

  @Test
  public void testConvertGYearMonthToDate() throws Exception {
    String gYearMonth = "2009-09";
    String testDate = "2009-09-01";
    Date expectedDate = dateFormat.parse(testDate);
    Date result = CswUnmarshallHelper.convertToDate(gYearMonth);
    assertThat(result, is(expectedDate));
  }
}
