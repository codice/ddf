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
package ddf.catalog.operation.impl;

import ddf.catalog.operation.TermFacetProperties;
import java.util.HashSet;
import java.util.Set;

public class TermFacetPropertiesImpl implements TermFacetProperties {

  public static final int DEFAULT_FACET_LIMIT = 100;

  public static final int DEFAULT_MIN_FACET_COUNT = 1;

  public static final SortFacetsBy DEFAULT_SORT_KEY = SortFacetsBy.COUNT;

  private Set<String> facetAttributes;

  private SortFacetsBy sortKey;

  private int facetLimit;

  private int minFacetCount;

  /**
   * Creates a TermFacetPropertiesImpl object using default parameters to facet on the provided
   * attributes.
   *
   * @param facetAttributes A set of attributes to facet on
   */
  public TermFacetPropertiesImpl(Set<String> facetAttributes) {
    this(facetAttributes, DEFAULT_SORT_KEY);
  }

  /**
   * Creates a TermFacetPropertiesImpl object using default parameters to facet on the provided
   * attributes, returning results sorted by the provided key. Valid sortKey values are INDEX and
   * COUNT.
   *
   * @param facetAttributes A set of fields to facet on
   * @param sortKey The key used to sort results - INDEX or COUNT
   */
  public TermFacetPropertiesImpl(Set<String> facetAttributes, SortFacetsBy sortKey) {
    this(facetAttributes, sortKey, DEFAULT_FACET_LIMIT, DEFAULT_MIN_FACET_COUNT);
  }

  /**
   * Creates a TermFacetPropertiesImpl object using the supplied parameters to facet on the provided
   * attributes, returning results sorted by the provided key.
   *
   * @param facetAttributes A set of fields to facet on
   * @param sortKey The key used to sort results - INDEX or COUNT
   * @param facetLimit The maximum number of returned facet values (Default is 100)
   * @param minFacetCount The minimum count required for a facet value to be included in results
   *     (Default is 0)
   */
  public TermFacetPropertiesImpl(
      Set<String> facetAttributes, SortFacetsBy sortKey, int facetLimit, int minFacetCount) {
    this.facetAttributes = facetAttributes == null ? new HashSet<>() : facetAttributes;
    this.sortKey = sortKey;
    this.facetLimit = facetLimit;
    this.minFacetCount = minFacetCount;
  }

  @Override
  public Set<String> getFacetAttributes() {
    return new HashSet<>(facetAttributes);
  }

  @Override
  public SortFacetsBy getSortKey() {
    return sortKey;
  }

  @Override
  public int getFacetLimit() {
    return facetLimit;
  }

  @Override
  public int getMinFacetCount() {
    return minFacetCount;
  }
}
