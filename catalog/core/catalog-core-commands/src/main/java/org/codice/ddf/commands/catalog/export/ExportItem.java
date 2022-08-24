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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ExportItem {
  private String id = "";

  private URI resourceURI;

  private List<String> derivedUris;

  public ExportItem(String id, URI resourceURI, List<String> derivedUris) {
    this.id = id;
    this.resourceURI = resourceURI;
    this.derivedUris = Optional.ofNullable(derivedUris).orElseGet(Collections::emptyList);
  }

  public String getId() {
    return id;
  }

  public URI getResourceUri() {
    return resourceURI;
  }

  public List<String> getDerivedUris() {
    return derivedUris;
  }

  @Override
  public String toString() {
    return String.format("ExportItem{id='%s', resourceURI='%s'}", id, resourceURI);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExportItem that = (ExportItem) o;
    return Objects.equals(id, that.id) && Objects.equals(resourceURI, that.resourceURI);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
