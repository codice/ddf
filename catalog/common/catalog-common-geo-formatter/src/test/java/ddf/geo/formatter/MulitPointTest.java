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

public class MulitPointTest extends AbstractTestCompositeGeometry {

  @Test
  public void testGeoRssConversion() throws ParseException, IOException, SAXException {

    String entryXmlText =
        getSampleAtomEntry(new MultiPoint(reader.read("MULTIPOINT (1 2, 3 4, 5 6, 7 8)")));

    String control =
        "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:georss=\"http://www.georss.org/georss\">\r\n"
            + "    <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
            + "        <gml:Point>\r\n"
            + "            <gml:pos>2.0 1.0</gml:pos>"
            + "        </gml:Point>\r\n"
            + "    </georss:where>\r\n"
            + "    <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
            + "        <gml:Point>\r\n"
            + "            <gml:pos>4.0 3.0</gml:pos>"
            + "        </gml:Point>\r\n"
            + "    </georss:where>\r\n"
            + "    <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
            + "        <gml:Point>\r\n"
            + "            <gml:pos>6.0 5.0</gml:pos>"
            + "        </gml:Point>\r\n"
            + "    </georss:where>\r\n"
            + "    <georss:where xmlns:gml=\"http://www.opengis.net/gml\">\r\n"
            + "        <gml:Point>\r\n"
            + "            <gml:pos>8.0 7.0</gml:pos>"
            + "        </gml:Point>\r\n"
            + "    </georss:where>\r\n"
            + "</entry>";

    assertXMLEqual(control, entryXmlText);
  }
}
