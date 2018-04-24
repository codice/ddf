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
package org.codice.ddf.catalog.harvest.common;

import ddf.catalog.resource.impl.ResourceImpl;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.codice.ddf.catalog.harvest.HarvestedResource;

// TODO javadoc
public class HarvestedFile extends ResourceImpl implements HarvestedResource {

  private final URI uri;

  public HarvestedFile(InputStream is, String name, String uri) {
    super(is, name);
    try {
      this.uri = new URI(uri);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(String.format("Invalid URI [%s] received.", uri));
    }
  }

  @Override
  public URI getUri() {
    return uri;
  }
}
