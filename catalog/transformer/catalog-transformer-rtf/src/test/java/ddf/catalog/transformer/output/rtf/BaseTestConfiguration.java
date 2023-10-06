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
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.types.Core;
import ddf.catalog.transformer.output.rtf.model.ExportCategory;
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;

public abstract class BaseTestConfiguration {

  static final String REFERENCE_IMAGE_STRING_FILE = "test-image-string.txt";
  static final String REFERENCE_GIF_STRING_FILE = "test-gif-image-string.txt";
  static final String REFERENCE_METACARD_RTF_FILE = "reference-metacard.rtf";
  static final String REFERENCE_METACARD_WITH_ARGS_RTF_FILE = "reference-metacard-with-args.rtf";
  static final String REFERENCE_METACARD_WITH_GIF_RTF_FILE = "reference-metacard-with-gif.rtf";
  static final String REFERENCE_METACARD_RTF_WITH_EMPTY_THUMBNAIL_FILE =
      "reference-metacard-with-empty-thumbnail.rtf";
  static final String REFERENCE_SOURCE_RESPONSE_RTF_FILE = "reference-source-response.rtf";
  static final String REFERENCE_SOURCE_RESPONSE_WITH_ARGS_RTF_FILE =
      "reference-source-response-with-args.rtf";

  static final String EMPTY_ATTRIBUTE = "Empty";
  static final String SIMPLE_ATTRIBUTE = "Simple";
  static final String EXTENDED_ATTRIBUTE = "ext.extended-attribute";

  private static final String UNKNOWN_ATTRIBUTE = "Unknown";

  static final List<String> COLUMN_ORDER =
      Arrays.asList(UNKNOWN_ATTRIBUTE, EXTENDED_ATTRIBUTE, SIMPLE_ATTRIBUTE, EMPTY_ATTRIBUTE);

  static final Map<String, String> COLUMN_ALIASES;

  static {
    Map<String, String> map = new HashMap<>();
    map.put(EMPTY_ATTRIBUTE, "nothing");
    map.put(SIMPLE_ATTRIBUTE, "easy");
    map.put(EXTENDED_ATTRIBUTE, "extended");
    map.put(UNKNOWN_ATTRIBUTE, "not_Known");
    COLUMN_ALIASES = Collections.unmodifiableMap(map);
  }

  public static final List<RtfCategory> MOCK_CATEGORY =
      Stream.of(
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
          .map(BaseTestConfiguration::categoryFor)
          .collect(Collectors.toList());

  private static RtfCategory categoryFor(String name) {
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

  String getReferenceGifString() throws IOException {
    return inputStreamToString(
        getClass().getClassLoader().getResourceAsStream(REFERENCE_GIF_STRING_FILE));
  }

  String getReferenceMetacardWithArgsRtfFile() throws IOException {
    return inputStreamToString(
        getClass().getClassLoader().getResourceAsStream(REFERENCE_METACARD_WITH_ARGS_RTF_FILE));
  }

  String getReferenceMetacardWithGifRtfFile() throws IOException {
    return inputStreamToString(
        getClass().getClassLoader().getResourceAsStream(REFERENCE_METACARD_WITH_GIF_RTF_FILE));
  }

  String getReferenceSourceResponseWithArgsRtfFile() throws IOException {
    return inputStreamToString(
        getClass()
            .getClassLoader()
            .getResourceAsStream(REFERENCE_SOURCE_RESPONSE_WITH_ARGS_RTF_FILE));
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

  Metacard createMockMetacardWithGifImageData(String title) {
    try {
      return createMockMetacard(title, createGifThumbnailAttribute());
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
    MetacardType mockMetacardType = createMockMetacardType();
    when(metacard.getMetacardType()).thenReturn(mockMetacardType);
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

  Attribute createGifThumbnailAttribute() throws IOException {
    Attribute mockAttribute = mock(Attribute.class);
    byte[] image = Base64.getDecoder().decode(getReferenceGifString());
    when(mockAttribute.getValue()).thenReturn(image);

    return mockAttribute;
  }

  Attribute createExtendedAttribute() {
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn("Extended Value");
    when(mockAttribute.getValues()).thenReturn(Collections.singletonList("Extended Value"));
    return mockAttribute;
  }

  Attribute createSimpleAttribute() {
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn("Simple value");
    when(mockAttribute.getValues()).thenReturn(Collections.singletonList("Simple value"));
    return mockAttribute;
  }

  MetacardType createMockMetacardType() {
    MetacardType mockType = mock(MetacardType.class);
    AttributeDescriptor mockThumbnailDesc =
        createMockAttributeDescriptor(Core.THUMBNAIL, BasicTypes.BINARY_TYPE);
    when(mockType.getAttributeDescriptor(Core.THUMBNAIL)).thenReturn(mockThumbnailDesc);
    AttributeDescriptor mockEmptyDesc =
        createMockAttributeDescriptor(EMPTY_ATTRIBUTE, BasicTypes.STRING_TYPE);
    when(mockType.getAttributeDescriptor(EMPTY_ATTRIBUTE)).thenReturn(mockEmptyDesc);
    AttributeDescriptor mockSimpleDesc =
        createMockAttributeDescriptor(SIMPLE_ATTRIBUTE, BasicTypes.STRING_TYPE);
    when(mockType.getAttributeDescriptor(SIMPLE_ATTRIBUTE)).thenReturn(mockSimpleDesc);
    AttributeDescriptor mockExtendedDesc =
        createMockAttributeDescriptor(EXTENDED_ATTRIBUTE, BasicTypes.STRING_TYPE);
    when(mockType.getAttributeDescriptor(EXTENDED_ATTRIBUTE)).thenReturn(mockExtendedDesc);
    AttributeDescriptor mockUnknownDesc =
        createMockAttributeDescriptor(UNKNOWN_ATTRIBUTE, BasicTypes.INTEGER_TYPE);
    when(mockType.getAttributeDescriptor(UNKNOWN_ATTRIBUTE)).thenReturn(mockUnknownDesc);
    return mockType;
  }

  AttributeDescriptor createMockAttributeDescriptor(String name, AttributeType type) {
    AttributeDescriptor mockDescriptor = mock(AttributeDescriptor.class);
    when(mockDescriptor.getType()).thenReturn(type);
    return mockDescriptor;
  }
}
