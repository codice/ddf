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
package org.codice.ddf.spatial.kml.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import net.opengis.kml.v_2_2_0.AbstractFeatureType;
import net.opengis.kml.v_2_2_0.KmlType;
import net.opengis.kml.v_2_2_0.ObjectFactory;
import net.opengis.kml.v_2_2_0.PlacemarkType;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class KmlMarshallerTest {

  private KmlMarshaller kmlMarshaller;

  private static final ObjectFactory KML220_OBJECT_FACTORY = new ObjectFactory();

  @Before
  public void setup() {
    setupXpath();
    kmlMarshaller = new KmlMarshaller();
  }

  void setupXpath() {
    NamespaceContext ctx =
        new SimpleNamespaceContext(singletonMap("m", "http://www.opengis.net/kml/2.2"));
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  @Test
  public void marshall() throws SAXException, IOException, XpathException {
    PlacemarkType placemark = KML220_OBJECT_FACTORY.createPlacemarkType();
    placemark.setName("a");

    KmlType kmlType = KML220_OBJECT_FACTORY.createKmlType();
    kmlType.setAbstractFeatureGroup(KML220_OBJECT_FACTORY.createPlacemark(placemark));

    final String kmlString = kmlMarshaller.marshal(KML220_OBJECT_FACTORY.createKml(kmlType));

    assertXpathExists("/m:kml", kmlString);
    assertXpathEvaluatesTo("a", "//m:Placemark/m:name", kmlString);
  }

  @Test(expected = IllegalArgumentException.class)
  public void marshallNull() {
    kmlMarshaller.marshal(null);
  }

  @Test
  public void unmarshall() {
    final InputStream resourceAsStream = this.getClass().getResourceAsStream("/kmlPoint.kml");
    final KmlType kml = kmlMarshaller.unmarshal(resourceAsStream).get();

    final AbstractFeatureType feature = kml.getAbstractFeatureGroup().getValue();

    assertThat(feature.getName(), is("Simple placemark"));
  }

  @Test(expected = NoSuchElementException.class)
  public void unmarshallNullStream() {
    final KmlType kml = kmlMarshaller.unmarshal(null).get();
  }

  @Test(expected = NoSuchElementException.class)
  public void unmarshallEmptyStream() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes(UTF_8));
    final KmlType kml = kmlMarshaller.unmarshal(inputStream).get();
  }
}
