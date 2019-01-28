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

public class PolygonTest extends AbstractTestCompositeGeometry {

  @Test
  public void testGeoRssConversion() throws ParseException, IOException, SAXException {

    String entryXmlText =
        getSampleAtomEntry(
            new Polygon(reader.read("POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))")));

    String control =
        "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:georss=\"http://www.georss.org/georss\">\r\n"
            + "    <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
            + "        <gml:Polygon>\r\n"
            + "            <gml:exterior>\r\n"
            + "                <gml:LinearRing>\r\n"
            + "                    <gml:posList>10.0 30.0 20.0 10.0 40.0 20.0 40.0 40.0 10.0 30.0</gml:posList>\r\n"
            + "                </gml:LinearRing>\r\n"
            + "            </gml:exterior>\r\n"
            + "        </gml:Polygon>\r\n"
            + "    </georss:where>\r\n"
            + "</entry>";

    assertXMLEqual(control, entryXmlText);
  }
}
