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
package org.codice.ddf.catalog.transform;

import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;

/**
 * This represents the basic properties associated with transformers.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface TransformerProperties {

  String MIME_TYPE = "mime-type";

  /** The transformer ID. */
  String getId();

  /** The set of mime-types handled by the transformer. */
  Set<MimeType> getMimeTypes();

  /** All properties associated with the transformer. */
  Map<String, Object> getProperties();
}
