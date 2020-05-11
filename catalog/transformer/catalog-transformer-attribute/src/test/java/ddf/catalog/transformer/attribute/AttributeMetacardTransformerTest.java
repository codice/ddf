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
package ddf.catalog.transformer.attribute;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.IOException;
import java.util.Date;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Unit tests the {@link AttributeMetacardTransformer} */
public class AttributeMetacardTransformerTest {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AttributeMetacardTransformerTest.class);

  private static MimeType jpegMimeType = null;

  private static MimeType xmlMimeType = null;

  static {
    try {
      jpegMimeType = new MimeType("image/jpeg");
      xmlMimeType = new MimeType("application/xml");
    } catch (MimeTypeParseException e) {
      LOGGER.warn("MimeTypeParseException during static setup", e);
    }
  }

  private static final AttributeMetacardTransformer THUMBNAIL_TRANSFORMER =
      new AttributeMetacardTransformer(Metacard.THUMBNAIL, Metacard.THUMBNAIL, jpegMimeType);

  private static final AttributeMetacardTransformer METADATA_TRANSFORMER =
      new AttributeMetacardTransformer(Metacard.METADATA, Metacard.METADATA, xmlMimeType);

  /**
   * Tests case of null {@link Metacard}
   *
   * @throws CatalogTransformerException
   */
  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacard() throws CatalogTransformerException {
    THUMBNAIL_TRANSFORMER.transform(null, null);
  }

  /**
   * Tests case where metacard's thumbnail is null.
   *
   * @throws CatalogTransformerException
   */
  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardThumbnail() throws CatalogTransformerException {
    Metacard mockMetacard = mock(Metacard.class);

    THUMBNAIL_TRANSFORMER.transform(mockMetacard, null);
  }

  /**
   * Tests case where metacard's metadata is null.
   *
   * @throws CatalogTransformerException
   */
  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardMetadata() throws CatalogTransformerException {
    Metacard mockMetacard = mock(Metacard.class);

    METADATA_TRANSFORMER.transform(mockMetacard, null);
  }

  /**
   * Tests that the bytes returned are correct and MIME Type is correct.
   *
   * @throws CatalogTransformerException
   * @throws IOException
   */
  @Test()
  public void testMetacardThumbnail() throws CatalogTransformerException, IOException {
    byte[] thumbnailBytes = "sample".getBytes();
    simpleBytesTransform(thumbnailBytes);
  }

  /**
   * Tests that the String returned is correct and MIME Type is correct.
   *
   * @throws CatalogTransformerException
   * @throws IOException
   */
  @Test()
  public void testMetacardMetadata() throws CatalogTransformerException, IOException {

    simpleStringTransform("String");
  }

  /**
   * Tests that the bytes are faithfully returned with correct MIME Type.
   *
   * @throws CatalogTransformerException
   * @throws IOException
   */
  @Test()
  public void testEmptyArray() throws CatalogTransformerException, IOException {
    byte[] thumbnailBytes = {};
    simpleBytesTransform(thumbnailBytes);
  }

  /**
   * Tests correct behavior of empty string for metadata
   *
   * @throws CatalogTransformerException
   * @throws IOException
   */
  @Test()
  public void testEmptyString() throws CatalogTransformerException, IOException {
    simpleStringTransform("");
  }

  /**
   * Tests exception thrown for {@code null} string
   *
   * @throws CatalogTransformerException
   * @throws IOException
   */
  @Test(expected = CatalogTransformerException.class)
  public void testNullString() throws CatalogTransformerException, IOException {
    simpleStringTransform(null);
  }

  /**
   * Tests exception thrown for {@code null} string
   *
   * @throws CatalogTransformerException
   * @throws IOException
   */
  @Test(expected = CatalogTransformerException.class)
  public void testNotAString() throws CatalogTransformerException, IOException {

    Metacard mockMetacard = mock(Metacard.class);

    when(mockMetacard.getAttribute(isA(String.class)))
        .thenReturn(new AttributeImpl(Metacard.METADATA, new Date()));

    METADATA_TRANSFORMER.transform(mockMetacard, null);
  }

  /**
   * Tests toString
   *
   * @throws CatalogTransformerException
   * @throws IOException
   */
  @Test()
  public void testThumbnailToString() throws CatalogTransformerException, IOException {

    String transformerToString = THUMBNAIL_TRANSFORMER.toString();

    LOGGER.debug(transformerToString);

    assertEquals(
        MetacardTransformer.class.getName()
            + " {Impl="
            + AttributeMetacardTransformer.class.getName()
            + ", attributeName="
            + Metacard.THUMBNAIL
            + ", id="
            + Metacard.THUMBNAIL
            + ", MIME Type="
            + jpegMimeType
            + "}",
        transformerToString);
  }

  private void simpleBytesTransform(byte[] thumbnailBytes)
      throws CatalogTransformerException, IOException {
    Metacard mockMetacard = mock(Metacard.class);

    when(mockMetacard.getThumbnail()).thenReturn(thumbnailBytes);
    when(mockMetacard.getAttribute(Metacard.THUMBNAIL))
        .thenReturn(new AttributeImpl(Metacard.THUMBNAIL, thumbnailBytes));

    BinaryContent content = THUMBNAIL_TRANSFORMER.transform(mockMetacard, null);

    assertArrayEquals(thumbnailBytes, IOUtils.toByteArray(content.getInputStream()));

    assertEquals("Mime type failed to match.", jpegMimeType, content.getMimeType());
  }

  private void simpleStringTransform(String metadata)
      throws CatalogTransformerException, IOException {
    Metacard mockMetacard = mock(Metacard.class);

    when(mockMetacard.getAttribute(isA(String.class)))
        .thenReturn(new AttributeImpl(Metacard.METADATA, metadata));

    BinaryContent content = METADATA_TRANSFORMER.transform(mockMetacard, null);

    assertEquals(metadata, IOUtils.toString(content.getInputStream()));

    assertEquals("Mime type failed to match.", xmlMimeType, content.getMimeType());
  }
}
