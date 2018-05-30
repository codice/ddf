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

import com.google.common.collect.ImmutableMap;
import ddf.catalog.Constants;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;
import org.codice.ddf.catalog.ui.metacard.internal.ServicePropertiesConstants;
import org.codice.ddf.catalog.ui.metacard.internal.Splitter;

public abstract class AbstractSplitter implements Splitter {

  private final String id;

  private final Set<MimeType> mimeTypes;

  protected AbstractSplitter(String id, Set<MimeType> mimeTypes) {
    this.id = id;
    this.mimeTypes = mimeTypes;
  }

  @Override
  public final Map<String, Object> getProperties() {
    return new ImmutableMap.Builder<String, Object>()
        .put(Constants.SERVICE_ID, getId())
        .put(ServicePropertiesConstants.MIME_TYPE, new ArrayList<>(getMimeTypes()))
        .putAll(getAdditionalProperties())
        .build();
  }

  /** Do not include the service identifier or the mime-types. */
  protected abstract Map<String, Object> getAdditionalProperties();

  @Override
  public final String getId() {
    return id;
  }

  @Override
  public final Set<MimeType> getMimeTypes() {
    return mimeTypes;
  }
}
