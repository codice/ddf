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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.InputStream;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

public class KmlToJtsConverterTest {

  @Test
  public void testConvertGiantKml() {
    InputStream stream = KmlToJtsConverterTest.class.getResourceAsStream("/sampleKml.kml");

    Kml kml = Kml.unmarshal(stream);

    assertThat(kml, notNullValue());

    Geometry jtsGeometry = KmlToJtsConverter.from(kml);
    assertThat(jtsGeometry, notNullValue());
    assertThat(jtsGeometry.toString(), not(containsString("EMPTY")));
  }

  @Test
  public void testConvertKml() {
    InputStream stream = KmlToJtsConverterTest.class.getResourceAsStream("/kmlPoint.kml");

    Kml kml = Kml.unmarshal(stream);

    assertThat(kml, notNullValue());

    Geometry jtsGeometry = KmlToJtsConverter.from(kml);
    KmlFeatureToJtsGeometryConverterTest.assertFeature(kml.getFeature(), jtsGeometry);
  }

  @Test
  public void testConvertNullKmlReturnsNullGeometry() {
    Geometry jtsGeometry = KmlToJtsConverter.from(null);
    assertThat(jtsGeometry, nullValue());
  }
}
