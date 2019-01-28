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
package ddf.geo.formatter;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;
import org.xml.sax.SAXException;

public class PointTest extends AbstractTestCompositeGeometry {

  @Test
  public void testGeoRssConversion() throws ParseException, IOException, SAXException {

    String entryXmlText = getSampleAtomEntry(new Point(reader.read("POINT (1 2)")));

    String control =
        "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:georss=\"http://www.georss.org/georss\">\r\n"
            + "    <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
            + "        <gml:Point>\r\n"
            + "            <gml:pos>2.0 1.0</gml:pos>"
            + "        </gml:Point>\r\n"
            + "    </georss:where>\r\n"
            + "</entry>";

    assertXMLEqual(control, entryXmlText);
  }

  @Test
  public void testToCompositeGeometryWithString() {
    verifyCoordinates(Arrays.asList("1", "2"));
    verifyCoordinates(Arrays.asList("1.0000", "2"));
  }

  @Test
  public void testToCompositeGeometryWithInteger() {
    verifyCoordinates(Arrays.asList(1, 2));
  }

  @Test
  public void testToCompositeGeometryWithDouble() {
    verifyCoordinates(Arrays.asList(1.0, 2.0));
  }

  @Test
  public void testToCompositeGeometryWithLong() {
    verifyCoordinates(Arrays.asList(1L, 2L));
  }

  private void verifyCoordinates(List list) {
    CompositeGeometry compositeGeo = Point.toCompositeGeometry(list);

    assertThat(compositeGeo.getGeometry().getCoordinate().x, is(1.0));

    assertThat(compositeGeo.getGeometry().getCoordinate().y, is(2.0));
  }
}
