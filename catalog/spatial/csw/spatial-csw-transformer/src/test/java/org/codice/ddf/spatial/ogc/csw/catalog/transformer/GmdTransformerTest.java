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
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.TopicAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.DateTime;
import ddf.catalog.data.types.Location;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class GmdTransformerTest {

  private static final String XML_DECLARATION =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

  private static GmdTransformer gmdTransformer;

  private static MetacardType gmdMetacardType;

  @BeforeClass
  public static void setUp() {
    gmdMetacardType = getGmdMetacardType();
    gmdTransformer = new GmdTransformer(gmdMetacardType);
  }

  private static MetacardType getGmdMetacardType() {
    return new MetacardTypeImpl(
        "gmdMetacardType",
        Arrays.asList(
            new DateTimeAttributes(),
            new ValidationAttributes(),
            new ContactAttributes(),
            new LocationAttributes(),
            new MediaAttributes(),
            new TopicAttributes(),
            new AssociationsAttributes()));
  }

  private void assertGmdMetacardType(Metacard metacard) {
    assertThat(metacard.getMetacardType().getName(), is(gmdMetacardType.getName()));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testBadInputStream() throws Exception {
    InputStream is = Mockito.mock(InputStream.class);
    doThrow(new IOException()).when(is).read(any());
    gmdTransformer.transform(is);
  }

  @Test
  public void testDatasetToMetacard() throws Exception {
    Metacard metacard = transform("/gmd/dataset.xml");

    assertGmdMetacardType(metacard);

    assertThat(metacard.getTitle(), is("VMAPLV0"));

    assertThat(metacard.getId(), is("550e8400-e29b-41d4-a716-48957441234"));

    assertThat(metacard.getAttribute(Core.LANGUAGE).getValues(), hasItem("eng"));

    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is("dataset"));

    assertThat(metacard.getContentTypeName(), is("dataset"));

    assertThat(
        metacard.getAttribute(Core.DESCRIPTION).getValues(),
        hasItem("Vector Map: a general purpose database design to support GIS applications"));

    assertThat(metacard.getAttribute(Media.FORMAT).getValue(), is("gzip"));

    assertThat(metacard.getAttribute(Media.FORMAT_VERSION).getValue(), is("1.2.4"));

    assertThat(
        convertDate((Date) metacard.getAttribute(Core.CREATED).getValue()),
        is("2004-03-10 00:00:00 UTC"));

    assertThat(
        convertDate((Date) metacard.getAttribute(Core.MODIFIED).getValue()),
        is("2004-03-14 12:00:00 UTC"));

    assertThat(
        convertDate((Date) metacard.getAttribute(Core.EXPIRATION).getValue()),
        is("2004-03-04 00:00:00 UTC"));

    assertThat(metacard.getAttribute(Topic.KEYWORD).getValues(), hasItems("Geologie", "World"));

    assertThat(
        metacard.getAttribute(Topic.CATEGORY).getValues(),
        hasItems(
            "elevation",
            "inlandWaters",
            "oceans",
            "society",
            "structure",
            "transportation",
            "utilitiesCommunication"));

    assertThat(
        metacard.getAttribute(Associations.RELATED).getValues(),
        hasItems("00000000-0000-0000-0000-000000000002"));

    URI uri = new URI("http:/example.com/linkage");
    assertThat(metacard.getAttribute(Core.RESOURCE_URI).getValue(), is(uri.toString()));

    assertThat(
        metacard.getAttribute(Contact.PUBLISHER_ADDRESS).getValues(),
        hasItem("10 Downing Street London Westminster SW1A 2AA United Kingdom"));

    assertThat(
        metacard.getAttribute(Contact.PUBLISHER_EMAIL).getValues(), hasItem("theresa.may@gov.uk"));

    assertThat(
        metacard.getAttribute(Contact.PUBLISHER_NAME).getValues(), hasItem("Codice Foundation"));

    assertThat(
        metacard.getAttribute(Contact.PUBLISHER_PHONE).getValues(), hasItems("12345", "56789"));

    assertThat(metacard.getAttribute(Location.ALTITUDE).getValues(), hasItem(312.0));

    assertThat(metacard.getAttribute(GmdConstants.RESOURCE_STATUS).getValue(), is("completed"));

    assertThat(metacard.getAttribute(Location.COUNTRY_CODE).getValues(), hasItem("FRA"));

    assertThat(
        metacard.getAttribute(Location.COORDINATE_REFERENCE_SYSTEM_CODE).getValues(),
        hasItems("EPSG:4326", "World Geodetic System:WGS 84"));

    assertThat(
        metacard.getAttribute(Contact.POINT_OF_CONTACT_ADDRESS).getValues(),
        hasItem("4600 Example Rd Example Town CO 12345 United States"));
    assertThat(
        metacard.getAttribute(Contact.POINT_OF_CONTACT_EMAIL).getValues(),
        hasItem("example@email.com"));
    assertThat(
        metacard.getAttribute(Contact.POINT_OF_CONTACT_NAME).getValues(),
        hasItem("example organization"));
    assertThat(
        metacard.getAttribute(Contact.POINT_OF_CONTACT_PHONE).getValues(),
        hasItems("12345", "56789"));

    assertThat(metacard.getContentTypeName(), is("dataset"));

    assertThat(
        metacard.getAttribute(Core.DESCRIPTION).getValue(),
        is("Vector Map: a general purpose database design to support GIS applications"));

    assertThat(
        metacard.getLocation(),
        is("POLYGON ((6.9 -44.94, 6.9 61.61, 70.35 61.61, 70.35 -44.94, 6.9 -44.94))"));

    List<Serializable> temporals = metacard.getAttribute(DateTime.START).getValues();
    temporals.addAll(metacard.getAttribute(DateTime.END).getValues());
    List<String> dates = new ArrayList<>();
    for (Serializable serializable : temporals) {
      Date temporal = (Date) serializable;
      dates.add(convertDate(temporal));
    }

    assertThat(dates, hasItems("2000-01-01 00:00:00 UTC", "2007-05-07 00:00:00 UTC"));
  }

  @Test
  public void testDatasetWithFormatToMetacard() throws Exception {
    Metacard metacard = transform("/gmd/dataset2.xml");
    assertGmdMetacardType(metacard);

    assertThat(metacard.getAttribute(Media.FORMAT).getValue(), is("shapefile"));
  }

  @Test
  public void testAllGmdMetadataToMetacard() throws Exception {
    InputStream input = getClass().getResourceAsStream("/gmd/");
    try (BufferedReader rdr = new BufferedReader(new InputStreamReader(input))) {
      String line;
      while ((line = rdr.readLine()) != null) {

        if (StringUtils.isNotEmpty(line) && line.endsWith(".xml")) {
          Metacard metacard = transform("/gmd/" + line);
          assertGmdMetacardType(metacard);
        }
      }
    }
  }

  @Test
  public void testMetacardTransform() throws IOException, CatalogTransformerException {
    Metacard metacard = getTestMetacard();

    Map<String, Serializable> args = new HashMap<>();
    args.put(CswConstants.OMIT_XML_DECLARATION, false);

    BinaryContent content = new GmdTransformer(gmdMetacardType).transform(metacard, args);

    String xml = IOUtils.toString(content.getInputStream());
    assertThat(xml, startsWith(XML_DECLARATION));
  }

  @Test
  public void testMetacardTransformNoDeclaration() throws IOException, CatalogTransformerException {
    Metacard metacard = getTestMetacard();

    Map<String, Serializable> args = new HashMap<>();
    args.put(CswConstants.OMIT_XML_DECLARATION, true);

    BinaryContent content = new GmdTransformer(gmdMetacardType).transform(metacard, args);

    String xml = IOUtils.toString(content.getInputStream());
    assertThat(xml, not(startsWith(XML_DECLARATION)));
  }

  @Test
  public void testMetacardTransformNullArgs() throws IOException, CatalogTransformerException {
    Metacard metacard = getTestMetacard();

    BinaryContent content = new GmdTransformer(gmdMetacardType).transform(metacard, null);

    String xml = IOUtils.toString(content.getInputStream());
    assertThat(xml, startsWith(XML_DECLARATION));
  }

  @Test
  public void testMetacardTransformNullMetacard() throws IOException, CatalogTransformerException {
    BinaryContent content = new GmdTransformer(gmdMetacardType).transform((Metacard) null, null);

    String xml = IOUtils.toString(content.getInputStream());

    assertThat(xml.trim(), is(XML_DECLARATION));
  }

  private Metacard getTestMetacard() {
    return new MetacardImpl(getGmdMetacardType());
  }

  private MetacardImpl transform(String path) throws Exception {
    return (MetacardImpl) gmdTransformer.transform(getClass().getResourceAsStream(path));
  }

  private String convertDate(Date date) {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    return df.format(date);
  }
}
