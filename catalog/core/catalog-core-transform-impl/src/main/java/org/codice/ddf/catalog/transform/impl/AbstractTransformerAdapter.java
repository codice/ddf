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
package org.codice.ddf.catalog.transform.impl;

import ddf.catalog.Constants;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.codice.ddf.catalog.transform.TransformerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransformerAdapter implements TransformerProperties {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTransformerAdapter.class);

  private static final String NOT_AVAILABLE = "N/A";

  private Map<String, Object> properties;

  private String id;

  private Set<MimeType> mimeTypes;

  public AbstractTransformerAdapter(Map<String, Object> properties) {
    this.properties = new HashMap<>(properties);
    this.id = properties.getOrDefault(Constants.SERVICE_ID, NOT_AVAILABLE).toString();
    this.mimeTypes = getTransformerMimeTypes(properties);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Set<MimeType> getMimeTypes() {
    return mimeTypes;
  }

  @Override
  public Map<String, Object> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  public static Set<MimeType> getTransformerMimeTypes(Map<String, Object> properties) {

    Object propertyValue = properties.get(MIME_TYPE);

    if (propertyValue instanceof List) {
      return ((List<?>) propertyValue)
          .stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .map(AbstractTransformerAdapter::createMimeType)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    }

    return Stream.of(propertyValue)
        .filter(Objects::nonNull)
        .map(String.class::cast)
        .map(AbstractTransformerAdapter::createMimeType)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static MimeType createMimeType(String mimeType) {
    try {
      return new MimeType(mimeType);
    } catch (MimeTypeParseException e) {
      LOGGER.debug("failed to create mime-type: type={}", mimeType, e);
    }
    return null;
  }
}
