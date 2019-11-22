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
package ddf.catalog.content.data.impl;

import com.google.common.io.ByteSource;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentItemImpl implements ContentItem {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentItemImpl.class);

  private String id;

  protected URI uri;

  private String filename;

  private String mimeTypeRawData;

  private MimeType mimeType;

  private ByteSource byteSource;

  private long size;

  private Metacard metacard;

  protected String qualifier;

  /**
   * An incoming content item where the item's GUID should be known.
   *
   * @param id the {@link ContentItem}'s GUID - can be null
   * @param byteSource the {@link ContentItem}'s input stream containing its actual data
   * @param mimeTypeRawData the {@link ContentItem}'s mime type
   * @param metacard the {@link ContentItem}'s associated metacard
   */
  public ContentItemImpl(
      String id, ByteSource byteSource, String mimeTypeRawData, Metacard metacard) {
    this(id, byteSource, mimeTypeRawData, null, 0, metacard);
  }

  /**
   * An incoming content item where the item's GUID and size should be known.
   *
   * @param id the {@link ContentItem}'s GUID - can be null
   * @param qualifier the {@link ContentItem}'s qualifier - can be null; blank qualifiers are
   *     treated as null
   * @param byteSource the {@link ContentItem}'s input stream containing its actual data
   * @param mimeTypeRawData the {@link ContentItem}'s mime type
   * @param metacard the {@link ContentItem}'s associated metacard
   */
  public ContentItemImpl(
      String id,
      String qualifier,
      ByteSource byteSource,
      String mimeTypeRawData,
      Metacard metacard) {
    this(id, qualifier, byteSource, mimeTypeRawData, null, 0, metacard);
  }

  /**
   * An incoming content item where the item's GUID and size should be known.
   *
   * @param id the {@link ContentItem}'s GUID - can be null
   * @param byteSource the {@link ContentItem}'s input stream containing its actual data
   * @param mimeTypeRawData the {@link ContentItem}'s mime type
   * @param filename the {@link ContentItem}'s file name - can be null
   * @param size the {@link ContentItem}'s file size
   * @param metacard the {@link ContentItem}'s associated metacard
   */
  public ContentItemImpl(
      String id,
      ByteSource byteSource,
      String mimeTypeRawData,
      String filename,
      long size,
      Metacard metacard) {
    this(id, null, byteSource, mimeTypeRawData, filename, size, metacard);
  }

  /**
   * An incoming content item where the item's GUID and size should be known.
   *
   * @param id the {@link ContentItem}'s GUID - can be null
   * @param qualifier the {@link ContentItem}'s qualifier - can be null; blank qualifiers are
   *     treated as null
   * @param byteSource the {@link ContentItem}'s input stream containing its actual data
   * @param mimeTypeRawData the {@link ContentItem}'s mime type
   * @param filename the {@link ContentItem}'s file name - can be null
   * @param size the {@link ContentItem}'s file size
   * @param metacard the {@link ContentItem}'s associated metacard
   */
  public ContentItemImpl(
      String id,
      String qualifier,
      ByteSource byteSource,
      String mimeTypeRawData,
      String filename,
      long size,
      Metacard metacard) {
    this.byteSource = byteSource;
    this.id = id;
    if (StringUtils.isNotBlank(qualifier)) {
      this.qualifier = qualifier;
    } else {
      this.qualifier = null;
    }
    this.mimeType = null;
    this.size = size;
    if (filename != null) {
      this.filename = filename;
    } else {
      this.filename = DEFAULT_FILE_NAME;
    }
    this.metacard = metacard;
    this.mimeTypeRawData = DEFAULT_MIME_TYPE;
    if (StringUtils.isNotBlank(mimeTypeRawData)) {
      this.mimeTypeRawData = mimeTypeRawData;
    }
    try {
      this.mimeType = new MimeType(this.mimeTypeRawData);
    } catch (MimeTypeParseException e) {
      LOGGER.debug(
          "Unable to create MimeType from raw data {}", LogSanitizer.sanitize(mimeTypeRawData));
    }
    try {
      uri = new URI(CONTENT_SCHEME, this.id, this.qualifier);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to create content URI.", e);
    }
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getUri() {
    return uri.toString();
  }

  @Override
  public String getQualifier() {
    return qualifier;
  }

  @Override
  public String getFilename() {
    return filename;
  }

  @Override
  public MimeType getMimeType() {
    return mimeType;
  }

  @Override
  public String getMimeTypeRawData() {
    return mimeTypeRawData;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return byteSource.openStream();
  }

  @Override
  public long getSize() throws IOException {
    return size;
  }

  @Override
  public Metacard getMetacard() {
    return metacard;
  }

  @Override
  public String toString() {
    return String.format(
        "ContentItemImpl{id='%s', uri=%s, filename='%s', mimeTypeRawData='%s', mimeType=%s, byteSource=%s, size=%d, metacard=%s, qualifier='%s'}",
        id, uri, filename, mimeTypeRawData, mimeType, byteSource, size, metacard, qualifier);
  }
}
