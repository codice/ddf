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
package org.codice.ddf.catalog.ui.metacard.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Call {@link #close()} if {@link #isError()} returns {@code false}. */
public interface StorableResource extends AutoCloseable {

  /**
   * The caller is not responsible for calling {@link InputStream#close()} on the returned stream.
   * However, the caller is responsible for calling {@link AutoCloseable#close()}.
   *
   * @return
   * @throws IOException
   */
  InputStream getInputStream() throws IOException;

  Optional<String> getMimeType();

  String getFilename();

  boolean isError();

  String getErrorMessage();
}
