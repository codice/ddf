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
package ddf.catalog.transformer.output.rtf;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.transformer.output.rtf.model.ExportCategory;
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;

public abstract class BaseTestConfiguration {

  static final String REFERENCE_IMAGE_STRING_FILE = "test-image-string.txt";
  static final String REFERENCE_METACARD_RTF_FILE = "reference-metacard.rtf";
  static final String REFERENCE_METACARD_RTF_WITH_EMPTY_THUMBNAIL_FILE =
      "reference-metacard-with-empty-thumbnail.rtf";
  static final String REFERENCE_SOURCE_RESPONSE_RTF_FILE = "reference-source-response.rtf";

  static final String EMPTY_ATTRIBUTE = "Empty";
  static final String SIMPLE_ATTRIBUTE = "Simple";
  static final String EXTENDED_ATTRIBUTE = "ext.extended-attribute";

  private static final String UNKNOWN_ATTRIBUTE = "Unknown";

  public List<RtfCategory> getCategories() {
    return Stream.of(
            "Associations",
            "Contact",
            "Core",
            "DateTime",
            "Extended",
            "Location",
            "Media",
            "Security",
            "Topic",
            "Validation",
            "Version")
        .map(this::categoryFor)
        .collect(Collectors.toList());
  }

  String getReferenceMetacardRtfFile() throws IOException {
    return inputStreamToString(
        getClass().getClassLoader().getResourceAsStream(REFERENCE_METACARD_RTF_FILE));
  }

  String getReferenceMetacardRtfWithEmptyThumbnailFile() throws IOException {
    return inputStreamToString(
        getClass()
            .getClassLoader()
            .getResourceAsStream(REFERENCE_METACARD_RTF_WITH_EMPTY_THUMBNAIL_FILE));
  }

  String getReferenceSourceResponseRtfFile() throws IOException {
    return inputStreamToString(
        getClass().getClassLoader().getResourceAsStream(REFERENCE_SOURCE_RESPONSE_RTF_FILE));
  }

  String getReferenceImageString() throws IOException {
    return inputStreamToString(
        getClass().getClassLoader().getResourceAsStream(REFERENCE_IMAGE_STRING_FILE));
  }

  String inputStreamToString(InputStream inputStream) throws IOException {
    return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
  }

  Metacard createMockMetacardWithBadImageData(String title) {
    try {
      return createMockMetacard(title, createInvalidMediaAttribute());
    } catch (IOException e) {
      // Will be caught in the test as missing attribute
      return null;
    }
  }

  Metacard createMockMetacard(String title) {
    try {
      return createMockMetacard(title, createMediaAttribute());
    } catch (IOException e) {
      // Will be caught in the test as missing attribute
      return null;
    }
  }

  Metacard createMockMetacard(String title, Attribute mediaAttribute) {
    Metacard metacard = mock(Metacard.class);
    when(metacard.getTitle()).thenReturn(title);

    when(metacard.getId()).thenReturn("mock-id");

    when(metacard.getAttribute(Core.THUMBNAIL)).thenReturn(mediaAttribute);

    Attribute mockEmptyAttribute = mock(Attribute.class);
    when(metacard.getAttribute(EMPTY_ATTRIBUTE)).thenReturn(mockEmptyAttribute);

    Attribute mockSimpleAttribute = createSimpleAttribute();
    when(metacard.getAttribute(SIMPLE_ATTRIBUTE)).thenReturn(mockSimpleAttribute);

    Attribute mockExtendedAttribute = createExtendedAttribute();
    when(metacard.getAttribute(EXTENDED_ATTRIBUTE)).thenReturn(mockExtendedAttribute);

    return metacard;
  }

  Attribute createMediaAttribute() throws IOException {
    Attribute mockAttribute = mock(Attribute.class);
    byte[] image = Base64.getDecoder().decode(getReferenceImageString());
    when(mockAttribute.getValue()).thenReturn(image);

    return mockAttribute;
  }

  Attribute createInvalidMediaAttribute() throws IOException {
    Attribute mockAttribute = mock(Attribute.class);
    byte[] image = Base64.getDecoder().decode(getReferenceImageString().substring(0, 12));
    when(mockAttribute.getValue()).thenReturn(image);

    return mockAttribute;
  }

  Attribute createExtendedAttribute() {
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn("Extended Value");

    return mockAttribute;
  }

  Attribute createSimpleAttribute() {
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn("Simple value");

    return mockAttribute;
  }

  RtfCategory categoryFor(String name) {
    RtfCategory category = new ExportCategory();
    category.setAttributes(
        Arrays.asList(
            EMPTY_ATTRIBUTE,
            SIMPLE_ATTRIBUTE,
            Core.THUMBNAIL,
            UNKNOWN_ATTRIBUTE,
            EXTENDED_ATTRIBUTE));
    category.setTitle(name);

    return category;
  }
}
