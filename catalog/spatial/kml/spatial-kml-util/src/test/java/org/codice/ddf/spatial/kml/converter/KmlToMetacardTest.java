/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.kml.converter;

public class KmlToMetacardTest {

  //  @Test
  //  public void testConvertKmlMultiGeometryToMetacardWithBbox() {
  //    InputStream stream =
  // KmlToJtsConverterTest.class.getResourceAsStream("/kmlMultiGeometry.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //
  //    assertThat(kml, notNullValue());
  //
  //    Metacard metacard = KmlToMetacard.from(new MetacardImpl(), kml);
  //    assertThat(metacard, notNullValue());
  //
  //    MultiGeometry kmlMultiGeometry = ((MultiGeometry) ((Placemark)
  // kml.getFeature()).getGeometry());
  //    assertThat(kmlMultiGeometry, notNullValue());
  //
  //    Geometry jtsGeometryCollectionGeometry = KmlToJtsGeometryConverter.from(kmlMultiGeometry);
  //    assertThat(jtsGeometryCollectionGeometry, notNullValue());
  //
  //    String wktBbox = jtsGeometryCollectionGeometry.getEnvelope().toText();
  //    assertThat(
  //        metacard.getAttribute(Metacard.GEOGRAPHY).getValue().toString(),
  //        is(equalToIgnoringWhiteSpace(wktBbox)));
  //  }
  //
  //  @Test
  //  public void testConvertBadKmlReturnsNullMetacard() {
  //    InputStream stream = KmlToJtsConverterTest.class.getResourceAsStream("/notKml.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //
  //    Metacard metacard = KmlToMetacard.from(new MetacardImpl(), kml);
  //    assertThat(metacard, nullValue());
  //  }
  //
  //  @Test
  //  public void testKmlWithNoGeometry() {
  //    InputStream stream =
  // KmlToJtsConverterTest.class.getResourceAsStream("/kmlWithNoGeometry.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //
  //    Metacard metacard = KmlToMetacard.from(new MetacardImpl(), kml);
  //    assertThat(metacard, notNullValue());
  //  }
  //
  //  @Test
  //  public void testKmlWithTimeSpan() {
  //    InputStream stream =
  // KmlToJtsConverterTest.class.getResourceAsStream("/kmlWithTimeSpan.kml");
  //
  //    Kml kml = Kml.unmarshal(stream);
  //
  //    Metacard metacard = KmlToMetacard.from(new MetacardImpl(), kml);
  //    assertThat(metacard, notNullValue());
  //  }
}
