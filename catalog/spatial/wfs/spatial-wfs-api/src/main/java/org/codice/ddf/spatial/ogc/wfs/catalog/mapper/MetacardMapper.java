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
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/** Maps Metacards to WFS Features. */
public interface MetacardMapper {

  /** Gets the feature type for this metacard mapper */
  String getFeatureType();

  /** Gets the feature property used to sort temporally */
  String getSortByTemporalFeatureProperty();

  /** Gets the feature property used to when sorting by relevance */
  String getSortByRelevanceFeatureProperty();

  /** Gets the feature property used to when sorting by distance */
  String getSortByDistanceFeatureProperty();

  /** Gets the unit of the feature property mapped the metacard resource-size */
  String getDataUnit();

  /**
   * @param p - a predicate to identify the entry we're looking for. Passing in 'null' returns
   *     Optional.empty().
   * @return a matching entry found for which 'p.apply()' returns 'true'. It's expected that 'p'
   *     will uniquely identify an entry. When 'p' matches multiple Entry objects, the one selected
   *     is implementation-dependent.
   */
  Optional<Entry> getEntry(@Nullable Predicate<Entry> p);

  /** @return a java.util.Stream of the entries that have been added to this MetacardMapper. */
  Stream<Entry> stream();

  /** Represents a mapping between metacard attribute name and WFS feature name. */
  interface Entry {
    /** @return the name of the WFS feature to be mapped */
    String getFeatureProperty();

    /** @return the name of the metaacard attribute that equates to the WFS feature */
    String getAttributeName();

    /**
     * @return returns a function that, given a map of key/value pairs, returns a value for given
     *     mapping. The mechanism used to determine that value is implementation dependent.
     */
    Function<Map<String, Serializable>, String> getMappingFunction();
  }
}
