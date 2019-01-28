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

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import org.apache.abdera.Abdera;
import org.apache.abdera.ext.geo.GeoHelper;
import org.apache.abdera.ext.geo.GeoHelper.Encoding;
import org.apache.abdera.ext.geo.Position;
import org.apache.abdera.model.Entry;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.locationtech.jts.io.WKTReader;

public abstract class AbstractTestCompositeGeometry {

  protected static final Abdera ABDERA = new Abdera();

  protected WKTReader reader = new WKTReader();

  @BeforeClass
  public static void setupTestClass() {
    HashMap map = new HashMap();
    map.put("gml", "http://www.opengis.net/gml");
    map.put("georss", "http://www.georss.org/georss");
    NamespaceContext ctx = new SimpleNamespaceContext(map);
    XMLUnit.setXpathNamespaceContext(ctx);
    XMLUnit.setIgnoreWhitespace(true);
  }

  /**
   * Creates an Atom Entry with GeoRSS encoded in GML.
   *
   * @param composite
   * @return Atom entry as text
   * @throws org.locationtech.jts.io.ParseException
   * @throws IOException
   */
  protected String getSampleAtomEntry(CompositeGeometry composite) throws IOException {

    List<Position> positions = composite.toGeoRssPositions();

    Entry sampleEntry = ABDERA.newEntry();

    for (Position pos : positions) {
      GeoHelper.addPosition(sampleEntry, pos, Encoding.GML);
    }

    StringWriter writer = new StringWriter();
    sampleEntry.writeTo(writer);
    return writer.toString();
  }
}
