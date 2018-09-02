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
package org.codice.ddf.catalog.async.data.api.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 *
 * <p>A {@code ProcessResource} represents a local or remote resource that will be used during
 * processing by the {@link ProcessingFramework}.
 */
public interface ProcessResource {

  int UNKNOWN_SIZE = -1;

  /**
   * Gets a URI that represents the resource in the catalog.
   *
   * @return the URI of the {@link ProcessResource}
   */
  URI getUri();

  /**
   * A human friendly readable name.
   *
   * @return name of the {@link ProcessResource}
   */
  String getName();

  /**
   * An optional field to provide additional information about a {@link ProcessResource}, that if
   * present, indicates that this {@link ProcessResource} is derived from another {@link
   * ddf.catalog.resource.Resource}. The parent content of this {@link ProcessResource} is this
   * {@link ProcessResource}'s {@link URI} without the qualifier.
   *
   * @return the qualifier of the {@link ProcessResource}
   */
  String getQualifier();

  /**
   * Return the mime type raw data for the {@link ProcessResource}, e.g., image/nitf or
   * application/json;id=geojson
   *
   * @return the mime type raw data for the {@link ProcessResource}
   */
  String getMimeType();

  /**
   * Returns a new, independent input stream with each call containing the {@link ProcessResource}'s
   * actual data content. {@link #close()} should be called once new input streams are no longer
   * needed.
   *
   * @return a new, independent input stream containing the {@link ProcessResource}'s data content.
   * @throws IOException if the input stream is not available
   */
  InputStream getInputStream() throws IOException;

  /**
   * Return the total number of bytes in the {@link ProcessResource}'s input stream.
   *
   * @return returns the total number of bytes that can be read from the input stream, or {@link
   *     #UNKNOWN_SIZE} if the total number of bytes is unknown or not applicable (e.g., in the case
   *     of a live stream)
   */
  long getSize();

  /**
   * Determines if the {@code ProcessResource} has been modified or created by any of the {@link
   * PostProcessPlugin}s during processing by the {@link ProcessingFramework}. This is used to
   * determine whether or not a {@link ddf.catalog.content.operation.UpdateStorageRequest} for this
   * {@code ProcessResource} needs to be made back to the {@link ddf.catalog.CatalogFramework}.
   *
   * @return {@code true} if modified, {@code false} otherwise
   */
  boolean isModified();

  /**
   * close the source of the {@link ProcessResource}'s input stream data. Once this is closed {@link
   * #getInputStream()} will no longer return valid input streams and any existing input streams
   * retrieved from this {@link ProcessResource} will no longer be usable.
   */
  void close();
}
