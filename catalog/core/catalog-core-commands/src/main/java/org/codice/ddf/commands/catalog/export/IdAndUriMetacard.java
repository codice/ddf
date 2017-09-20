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
package org.codice.ddf.commands.catalog.export;

import ddf.catalog.data.impl.MetacardImpl;
import java.net.URI;
import javax.annotation.concurrent.Immutable;

@Immutable
public class IdAndUriMetacard extends MetacardImpl {
  private final String id;

  private final URI uri;

  public IdAndUriMetacard(String id, URI uri) {
    this.id = id;
    this.uri = uri;
  }

  @Override
  public URI getResourceURI() {
    return this.uri;
  }

  @Override
  public String getId() {
    return this.id;
  }
}
