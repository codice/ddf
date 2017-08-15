/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.operation.faceting;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class FacetProperties implements Serializable {

    public enum SortFacetsBy {INDEX, COUNT}

    private static final int DEFAULT_FACET_LIMIT = 100;

    private static final int DEFAULT_MIN_FACET_COUNT = 0;

    private Set<String> facetAttributes;

    private SortFacetsBy sortKey;

    private int facetLimit;

    private int minFacetCount;

    /**
     * Creates a FacetProperties object using default parameters to facet on the provided attributes.
     *
     * @param facetAttributes A set of attributes to facet on
     */
    public FacetProperties(Set<String> facetAttributes) {
        this(facetAttributes, SortFacetsBy.COUNT);
    }

    /**
     * Creates a FacetProperties object using default parameters to facet on the provided attributes,
     * returning results sorted by the provided key. Valid sortKey values are INDEX and COUNT.
     *
     * @param facetAttributes A set of fields to facet on
     * @param sortKey     The key used to sort results - INDEX or COUNT
     */
    public FacetProperties(Set<String> facetAttributes, SortFacetsBy sortKey) {
        this(facetAttributes, sortKey, DEFAULT_FACET_LIMIT, DEFAULT_MIN_FACET_COUNT);
    }

    /**
     * Creates a FacetProperties object using the supplied parameters to facet on the provided
     * attributes, returning results sorted by the provided key.
     *
     * @param facetAttributes A set of fields to facet on
     * @param sortKey         The key used to sort results - INDEX or COUNT
     * @param facetLimit      The maximum number of returned facet values (Default is 100)
     * @param minFacetCount   The minimum count required for a facet value to be included in results (Default is 0)
     */
    public FacetProperties(Set<String> facetAttributes, SortFacetsBy sortKey, int facetLimit,
            int minFacetCount) {
        this.facetAttributes = facetAttributes == null ?
                new HashSet<>() : facetAttributes;
        this.sortKey = sortKey;
        this.facetLimit = facetLimit;
        this.minFacetCount = minFacetCount;
    }

    public Set<String> getFacetAttributes() {
        return new HashSet<>(facetAttributes);

    }

    public SortFacetsBy getSortKey() {
        return sortKey;
    }

    public int getFacetLimit() {
        return facetLimit;
    }

    public int getMinFacetCount() {
        return minFacetCount;
    }
}
