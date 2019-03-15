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
package org.codice.ddf.spatial.kml.transformer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KmzTransformerTest {

  private KmzTransformer kmzTransformer;

  private static final String KML_EXTENSION = ".kml";

  private MimeType kmzMimetype;

  @Mock private KMLTransformerImpl kmlTransformer;

  @Mock private Metacard mockMetacard;

  @Mock private SourceResponse mockSourceResponse;

  @Before
  public void setup() throws IOException, MimeTypeParseException {
    kmzTransformer = new KmzTransformer(kmlTransformer);
    kmzMimetype = new MimeType("application/vnd.google-earth.kmz");
  }

  @Test
  public void testTransformKmlToKmz() throws IOException {
    final InputStream resourceAsStream = this.getClass().getResourceAsStream("/kmlPoint.kml");
    BinaryContent inputKmlFile = new BinaryContentImpl(resourceAsStream);
    BinaryContent kmz = kmzTransformer.transformKmlToKmz(inputKmlFile);
    assertThat(kmz.getMimeType().match(kmzMimetype), is(true));

    String outputKml = getOutputFromBinaryContent(kmz);
    assertThat(outputKml, is(resourceToString("/kmlPoint.kml")));
  }

  @Test
  public void testKmzMetacardTransform() throws CatalogTransformerException, IOException {
    final InputStream resourceAsStream = this.getClass().getResourceAsStream("/kmlPoint.kml");
    BinaryContent inputKmlFile = new BinaryContentImpl(resourceAsStream);
    when(kmlTransformer.transform(any(Metacard.class), anyMap())).thenReturn(inputKmlFile);

    BinaryContent kmz = kmzTransformer.transform(mockMetacard, new HashMap<>());
    assertThat(kmz.getMimeType().match(kmzMimetype), is(true));

    String outputKml = getOutputFromBinaryContent(kmz);
    assertThat(outputKml, is(resourceToString("/kmlPoint.kml")));
  }

  @Test
  public void testKmzSourceResponseTransform() throws CatalogTransformerException, IOException {
    final InputStream resourceAsStream = this.getClass().getResourceAsStream("/multiPlacemark.kml");
    BinaryContent inputKmlFile = new BinaryContentImpl(resourceAsStream);
    when(kmlTransformer.transform(any(SourceResponse.class), anyMap())).thenReturn(inputKmlFile);

    BinaryContent kmz = kmzTransformer.transform(mockSourceResponse, new HashMap<>());
    assertThat(kmz.getMimeType().match(kmzMimetype), is(true));

    String outputKml = getOutputFromBinaryContent(kmz);
    assertThat(outputKml, is(resourceToString("/multiPlacemark.kml")));
  }

  private InputStream getResourceAsStream(String resourcePath) {
    return this.getClass().getResourceAsStream(resourcePath);
  }

  private String getOutputFromBinaryContent(BinaryContent binaryContent) throws IOException {
    // BC is a kmz zip file containing a single kml file called doc.kml.
    // Optionally, relative file links will exist in folder called files
    String outputKml;
    try (ZipInputStream zipInputStream = new ZipInputStream(binaryContent.getInputStream())) {

      ZipEntry entry;
      outputKml = "";
      while ((entry = zipInputStream.getNextEntry()) != null) {

        // According to Google, a .kmz should only contain a single .kml file
        // so we stop at the first one we find.
        final String fileName = entry.getName();
        if (fileName.endsWith(KML_EXTENSION)) {
          assertThat(fileName, is("doc.kml"));
          outputKml = readContentsFromZipInputStream(zipInputStream);
          break;
        }
      }
    }
    return outputKml;
  }

  private String resourceToString(String resourceName) throws IOException {
    try (final InputStream inputStream = getResourceAsStream(resourceName)) {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    }
  }

  private String readContentsFromZipInputStream(ZipInputStream zipInputStream) throws IOException {
    String kmlDocument = IOUtils.toString(zipInputStream, StandardCharsets.UTF_8.name());
    IOUtils.closeQuietly(zipInputStream);
    return kmlDocument;
  }
}
