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

import java.io.IOException;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;
import org.xml.sax.SAXException;

public class MultiLineStringTest extends AbstractTestCompositeGeometry {

  @Test
  public void testGeoRssConversion() throws ParseException, IOException, SAXException {

    String entryXmlText =
        getSampleAtomEntry(
            new MultiLineString(
                reader.read(
                    "MULTILINESTRING ((1 2, 3 4, 5 7),\r\n" + "(8 9, 10 12, 13 14, 15 16))")));

    String control =
        "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:georss=\"http://www.georss.org/georss\">\r\n"
            + "    <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
            + "        <gml:LineString>\r\n"
            + "            <gml:posList>2.0 1.0 4.0 3.0 7.0 5.0</gml:posList>"
            + "        </gml:LineString>\r\n"
            + "    </georss:where>\r\n"
            + "    <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
            + "        <gml:LineString>\r\n"
            + "            <gml:posList>9.0 8.0 12.0 10.0 14.0 13.0 16.0 15.0</gml:posList>"
            + "        </gml:LineString>\r\n"
            + "    </georss:where>\r\n"
            + "</entry>";

    assertXMLEqual(control, entryXmlText);
  }
}
