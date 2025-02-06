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
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.DateTime;
import ddf.catalog.data.types.Location;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class GmdConverterTest {

  private static final transient Logger LOGGER = LoggerFactory.getLogger(GmdConverterTest.class);

  private static GregorianCalendar createdDate;

  private static GregorianCalendar modifiedDate;

  private static GregorianCalendar effectiveDate;

  private static final String RESOURCE_DOWNLOAD_URL =
      "http://example.com/source/id?transform=resource";

  private static final String RESOURCE_URI = "content:123";

  private static final String POLYGON_LOCATION =
      "POLYGON ((117.6552810668945 -30.92013931274414, 117.661361694336 -30.92383384704589, 117.6666412353516 -30.93005561828613, "
          + "117.6663589477539 -30.93280601501464, 117.6594467163086 -30.93186187744141, 117.6541137695312 -30.93780517578125, "
          + "117.6519470214844 -30.94397163391114, 117.6455535888672 -30.94255638122559, 117.6336364746094 -30.93402862548828, "
          + "117.6355285644531 -30.92874908447266, 117.6326370239258 -30.92138862609864, 117.6395568847656 -30.92236137390137, "
          + "117.6433029174805 -30.91708374023438, 117.6454467773437 -30.91711044311523, 117.6484985351563 -30.92061042785645, "
          + "117.6504135131836 -30.92061042785645, 117.6504440307617 -30.91638946533203, 117.6552810668945 -30.92013931274414))";

  @BeforeClass
  public static void setup() {
    XMLUnit.setIgnoreWhitespace(true);
    TimeZone zone = TimeZone.getTimeZone("UTC");
    createdDate = new GregorianCalendar(2014, 10, 1);
    createdDate.setTimeZone(zone);

    modifiedDate = new GregorianCalendar(2016, 10, 1);
    modifiedDate.setTimeZone(zone);

    effectiveDate = new GregorianCalendar(2015, 10, 1);
    effectiveDate.setTimeZone(zone);
  }

  private String convert(Object object, boolean writeNamespaces) {
    GmdConverter converter = new GmdConverter();
    StringWriter stringWriter = new StringWriter();

    PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter, new NoNameCoder());
    MarshallingContext context = new TreeMarshaller(writer, null, null);

    context.put(CswConstants.WRITE_NAMESPACES, writeNamespaces);

    converter.marshal(object, writer, context);

    return stringWriter.toString();
  }

  @Test
  public void testConvertNullMetacard() {
    String xml = convert(null, true);
    assertThat(xml, isEmptyString());
  }

  @Test
  public void testConvertNonMetacard() {
    String xml = convert("", true);
    assertThat(xml, isEmptyString());
  }

  @Test
  public void testMarshal() throws IOException, SAXException {
    assertMetacard(getTestMetacard(POLYGON_LOCATION), "/gmd/metacard-as-GMD.xml");
  }

  @Test
  public void testMarshalSparseMetacard() throws IOException, SAXException {
    assertMetacard(getSparseMetacard(), "/gmd/metacard-as-GMD-sparse.xml");
  }

  private void assertMetacard(Metacard metacard, String xmlPath) throws IOException, SAXException {
    String compareString;
    try (InputStream input = getClass().getResourceAsStream(xmlPath)) {
      compareString = IOUtils.toString(input, StandardCharsets.UTF_8);
    }

    String xml = convert(metacard, true);
    Diff diff = new Diff(compareString, xml);

    LOGGER.info("diff: {}", diff.toString());
    System.out.println(xml);
    System.out.println(compareString);
    assertThat(diff.identical(), is(true));
  }

  private MetacardImpl getSparseMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(Core.ID, "ID");
    metacard.setAttribute(Core.CREATED, createdDate.getTime());
    metacard.setAttribute(Core.MODIFIED, modifiedDate.getTime());

    /* Backwards Compatibility */
    metacard.setContentTypeName("jpeg");
    metacard.setContentTypeVersion("1.0.0");
    metacard.setAttribute(Metacard.EFFECTIVE, effectiveDate.getTime());
    return metacard;
  }

  private MetacardImpl getTestMetacard(String wkt) {
    MetacardImpl metacard = getSparseMetacard();
    metacard.setAttribute(Core.CREATED, createdDate.getTime());
    metacard.setAttribute(Core.MODIFIED, modifiedDate.getTime());
    metacard.setAttribute(Core.RESOURCE_DOWNLOAD_URL, RESOURCE_DOWNLOAD_URL);
    metacard.setAttribute(Core.DESCRIPTION, "example description");
    metacard.setAttribute(Core.LOCATION, wkt);
    metacard.setAttribute(Core.METADATA, "</xml>");
    metacard.setAttribute(Core.RESOURCE_SIZE, "123TB");
    metacard.setAttribute(Core.LANGUAGE, (Serializable) Arrays.asList("ger", "eng"));
    metacard.setAttribute(Core.SOURCE_ID, "sourceID");
    metacard.setAttribute(Core.TITLE, "example title");
    metacard.setAttribute(Core.ID, "1234");
    metacard.setAttribute(Core.METACARD_CREATED, modifiedDate.getTime());
    metacard.setAttribute(Core.METACARD_MODIFIED, modifiedDate.getTime());
    metacard.setAttribute(Contact.POINT_OF_CONTACT_NAME, "John Doe");
    metacard.setAttribute(Contact.POINT_OF_CONTACT_EMAIL, "john.doe@example.com");
    metacard.setAttribute(Contact.POINT_OF_CONTACT_PHONE, "12345");
    metacard.setAttribute(
        Contact.POINT_OF_CONTACT_ADDRESS,
        "10 Downing Street London Westminster SW1A 2AA United Kingdom");
    metacard.setAttribute(Contact.PUBLISHER_NAME, "Jane Doe");
    metacard.setAttribute(Contact.PUBLISHER_EMAIL, "jane.doe@example.com");
    metacard.setAttribute(Contact.PUBLISHER_PHONE, "6789");
    metacard.setAttribute(Contact.POINT_OF_CONTACT_ADDRESS, "123 Fake Street Springfield MO 12345");
    metacard.setAttribute(Location.COORDINATE_REFERENCE_SYSTEM_CODE, "EPSG:4326");
    metacard.setAttribute(Location.COUNTRY_CODE, "FRA");
    metacard.setAttribute(Location.ALTITUDE, 123.0);
    metacard.setAttribute(DateTime.START, effectiveDate.getTime());
    metacard.setAttribute(DateTime.END, effectiveDate.getTime());
    metacard.setAttribute(Media.FORMAT, "gzip");
    metacard.setAttribute(Media.FORMAT_VERSION, "2.0");
    metacard.setAttribute(Topic.KEYWORD, "keyword");
    metacard.setAttribute(Topic.CATEGORY, "category");
    metacard.setAttribute(Core.RESOURCE_DOWNLOAD_URL, RESOURCE_DOWNLOAD_URL);

    try {
      metacard.setAttribute(Core.RESOURCE_URI, new URI(RESOURCE_URI));
    } catch (URISyntaxException e) {
      LOGGER.debug("URISyntaxException", e);
    }
    return metacard;
  }
}
