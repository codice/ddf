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
package org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;

class DefaultMetacardMapper implements MetacardMapper {

  @Override
  public String getFeatureType() {
    return "";
  }

  @Override
  public String getSortByTemporalFeatureProperty() {
    return "";
  }

  @Override
  public String getSortByRelevanceFeatureProperty() {
    return "";
  }

  @Override
  public String getSortByDistanceFeatureProperty() {
    return "";
  }

  @Override
  public String getDataUnit() {
    return "MB";
  }

  @Override
  public Optional<Entry> getEntry(Predicate<Entry> p) {
    return Optional.empty();
  }

  @Override
  public Stream<Entry> stream() {
    return Stream.of();
  }
}
