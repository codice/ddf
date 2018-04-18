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
package org.codice.ddf.catalog.ui.metacard.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.codice.ddf.catalog.ui.metacard.internal.StorableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This splitter always returns a stream that contains the exact item passed in. This class will
 * attempt to match all possible mime types.
 */
public class IdentitySplitter extends AbstractSplitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdentitySplitter.class);

  private static final String ID = "identity-splitter";

  private static final Set<MimeType> ALL_MIME_TYPES;

  static {
    try {
      ALL_MIME_TYPES = new HashSet<>();

      MediaTypeRegistry mediaTypeRegistry = MediaTypeRegistry.getDefaultRegistry();

      Set<MediaType> mediaTypes = mediaTypeRegistry.getTypes();
      Set<MediaType> mediaTypeAliases = new HashSet<>();

      for (MediaType mediaType : mediaTypes) {
        addMediaTypeToMimeTypes(mediaType);
        mediaTypeAliases.addAll(mediaTypeRegistry.getAliases(mediaType));
      }

      for (MediaType mediaType : mediaTypeAliases) {
        addMediaTypeToMimeTypes(mediaType);
      }

      ALL_MIME_TYPES.add(new MimeType("image/jp2"));
      ALL_MIME_TYPES.add(new MimeType("application/vnd.ms-visio.viewer"));

      LOGGER.debug("supported mime types: {}", ALL_MIME_TYPES);

    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static void addMediaTypeToMimeTypes(MediaType mediaType) throws MimeTypeParseException {
    String mimeType = mediaType.getType() + "/" + mediaType.getSubtype();
    ALL_MIME_TYPES.add(new MimeType(mimeType));
  }

  public IdentitySplitter() {
    super(ID, ALL_MIME_TYPES);
  }

  @Override
  public Stream<StorableResource> split(
      StorableResource storableResource, Map<String, ? extends Serializable> arguments) {
    return Stream.of(storableResource);
  }

  @Override
  protected Map<String, Object> getAdditionalProperties() {
    return Collections.emptyMap();
  }
}
