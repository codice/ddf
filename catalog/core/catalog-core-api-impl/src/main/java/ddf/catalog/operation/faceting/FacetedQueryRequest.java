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

import static ddf.catalog.Constants.EXPERIMENTAL_FACET_FIELDS_KEY;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ddf.catalog.operation.Query;
import ddf.catalog.operation.impl.QueryRequestImpl;

public class FacetedQueryRequest extends QueryRequestImpl {

    /**
     * Instantiates a FacetedQueryRequest to facet on the provided fields.
     *
     * @param query The query to be sent to the data source
     * @param facetFields A list of fields for which to return text faceting counts
     */
    public FacetedQueryRequest(Query query, Set<String> facetFields) {
        this(query, false, null, null, new FacetProperties(facetFields));
    }

    /**
     * Instantiates a FacetedQueryRequest to facet using the provided FacetProperties.
     *
     * @param query The query to be sent to the data source.
     * @param facetProperties Properties describing the faceting parameters.
     */
    public FacetedQueryRequest(Query query, FacetProperties facetProperties) {
        this(query, false, null, null, facetProperties);
    }

    /**
     * Instantiates a FacetedQueryRequest to facet on the provided fields.
     *
     * @param query The query to be sent to the data source
     * @param isEnterprise Specifies if this FacetedQueryRequest is an enterprise query
     * @param sourceIds A list of sources to query
     * @param properties Properties supplied to this query for auth, transactions, etc
     * @param facetProperties A structure describing the desired faceting constraints
     */
    public FacetedQueryRequest(Query query, boolean isEnterprise, Collection<String> sourceIds,
            Map<String, Serializable> properties, FacetProperties facetProperties) {
        super(query, isEnterprise, sourceIds, properties);

        this.properties = new HashMap<>(this.properties);
        this.properties.put(EXPERIMENTAL_FACET_FIELDS_KEY, facetProperties);
    }

}
