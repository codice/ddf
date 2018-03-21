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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.TreeUnmarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.XppReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class XStreamAttributeCopierTest {
  private static final String INSERT_XML =
      "    <csw:Insert typeName=\"csw:Record\">\n"
          + "        <csw:Record\n"
          + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
          + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
          + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
          + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
          + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
          + "            <dc:identifier>123</dc:identifier>\n"
          + "            <dc:title>Aliquam fermentum purus quis arcu</dc:title>\n"
          + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
          + "            <dc:subject>Hydrography--Dictionaries</dc:subject>\n"
          + "            <dc:format>application/pdf</dc:format>\n"
          + "            <dc:date>2006-05-12</dc:date>\n"
          + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
          + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
          + "                <ows:LowerCorner>44.792 -6.171</ows:LowerCorner>\n"
          + "                <ows:UpperCorner>51.126 -2.228</ows:UpperCorner>\n"
          + "            </ows:BoundingBox>\n"
          + "        </csw:Record>\n"
          + "    </csw:Insert>\n";

  private static final String DELETE_XML =
      "  <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
          + "    <csw:Constraint version=\"2.0.0\">\n"
          + "      <ogc:Filter>\n"
          + "        <ogc:PropertyIsEqualTo>\n"
          + "            <ogc:PropertyName>title</ogc:PropertyName>\n"
          + "            <ogc:Literal>Test Title</ogc:Literal>\n"
          + "        </ogc:PropertyIsEqualTo>\n"
          + "      </ogc:Filter>\n"
          + "    </csw:Constraint>\n"
          + "  </csw:Delete>\n";

  private static final String TEST_XML =
      "<csw:Transaction service=\"CSW\"\n"
          + "   version=\"2.0.2\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
          + "   xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
          + DELETE_XML
          + INSERT_XML
          + "</csw:Transaction>";

  private HierarchicalStreamReader reader;

  @Before
  public void setUp() throws IOException, XmlPullParserException {
    reader =
        new XppReader(
            new InputStreamReader(IOUtils.toInputStream(TEST_XML, StandardCharsets.UTF_8.name())),
            XmlPullParserFactory.newInstance().newPullParser());
  }

  @Test
  public void testCopyXml() throws IOException {
    // Move to the <Delete> element.
    reader.moveDown();

    StringWriter sw = new StringWriter();

    Map<String, String> attributeMap = new HashMap<>();
    attributeMap.put("foo", "bar");

    HierarchicalStreamReader deleteReader =
        XStreamAttributeCopier.copyXml(reader, sw, attributeMap);

    // Verify that the reader is at the end of <Delete>.
    assertThat(reader.hasMoreChildren(), is(false));

    // Move back up to <Transaction>.
    reader.moveUp();
    // Move down to <Insert>.
    reader.moveDown();

    // Verify that the reader is now at <Insert>.
    assertThat(reader.getNodeName(), is("csw:Insert"));

    assertThat(deleteReader.getNodeName(), is("csw:Delete"));
    assertThat(deleteReader.getAttributeCount(), is(3));

    // Verify that the attribute was copied to the element.
    assertThat(deleteReader.getAttribute("foo"), is("bar"));

    // Verify that the XML was copied correctly.
    assertThat(deleteReader.getAttribute(CswConstants.TYPE_NAME_PARAMETER), is("csw:Record"));
    assertThat(deleteReader.getAttribute(CswConstants.HANDLE_PARAMETER), is("something"));

    deleteReader.moveDown();
    assertThat(deleteReader.getNodeName(), is("csw:Constraint"));
    assertThat(deleteReader.getAttribute(CswConstants.VERSION), is("2.0.0"));

    deleteReader.moveDown();
    assertThat(deleteReader.getNodeName(), is("ogc:Filter"));

    deleteReader.moveDown();
    assertThat(deleteReader.getNodeName(), is("ogc:PropertyIsEqualTo"));

    deleteReader.moveDown();
    assertThat(deleteReader.getNodeName(), is("ogc:PropertyName"));
    assertThat(deleteReader.getValue(), is("title"));
    deleteReader.moveUp();

    deleteReader.moveDown();
    assertThat(deleteReader.getNodeName(), is("ogc:Literal"));
    assertThat(deleteReader.getValue(), is("Test Title"));
    deleteReader.moveUp();

    // Verify that PropertyIsEqualTo has no more children.
    assertThat(deleteReader.hasMoreChildren(), is(false));

    deleteReader.moveUp();
    // Verify that Filter has no more children.
    assertThat(deleteReader.hasMoreChildren(), is(false));

    deleteReader.moveUp();
    // Verify that Constraint has no more children.
    assertThat(deleteReader.hasMoreChildren(), is(false));

    deleteReader.moveUp();
    // Verify that Delete has no more children.
    assertThat(deleteReader.hasMoreChildren(), is(false));
  }

  @Test
  public void testCopyXmlNamespaceDeclarationsIntoContext() {
    UnmarshallingContext context = new TreeUnmarshaller(null, null, null, null);
    XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);

    @SuppressWarnings("unchecked")
    Map<String, String> attributeMap =
        (Map<String, String>) context.get(CswConstants.NAMESPACE_DECLARATIONS);

    assertThat(attributeMap.size(), is(2));

    assertThat(attributeMap.get("xmlns:csw"), is(CswConstants.CSW_OUTPUT_SCHEMA));
    assertThat(attributeMap.get("xmlns:ogc"), is(CswConstants.OGC_SCHEMA));
  }
}
