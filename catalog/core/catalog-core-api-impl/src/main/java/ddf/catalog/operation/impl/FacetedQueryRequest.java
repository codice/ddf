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

import static ddf.catalog.Constants.EXPERIMENTAL_FACET_PROPERTIES_KEY;

import ddf.catalog.operation.Query;
import ddf.catalog.operation.TermFacetProperties;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FacetedQueryRequest extends QueryRequestImpl {

  /**
   * Instantiates a FacetedQueryRequest to facet on the provided attributes.
   *
   * @param query The query to be sent to the data source
   * @param facetAttributes A list of attributes for which to return text faceting counts
   */
  public FacetedQueryRequest(Query query, Set<String> facetAttributes) {
    this(
        query,
        false,
        Collections.emptySet(),
        Collections.emptyMap(),
        new TermFacetPropertiesImpl(facetAttributes));
  }

  /**
   * Instantiates a FacetedQueryRequest to facet using the provided TermFacetPropertiesImpl.
   *
   * @param query The query to be sent to the data source.
   * @param termFacetProperties Properties describing the faceting parameters.
   */
  public FacetedQueryRequest(Query query, TermFacetProperties termFacetProperties) {
    this(query, false, Collections.emptySet(), Collections.emptyMap(), termFacetProperties);
  }

  /**
   * Instantiates a FacetedQueryRequest to facet on the provided attributes.
   *
   * @param query The query to be sent to the data source
   * @param isEnterprise Specifies if this FacetedQueryRequest is an enterprise query
   * @param sourceIds A list of sources to query
   * @param properties Properties supplied to this query for auth, transactions, etc
   * @param facetProperties A structure describing the desired faceting constraints
   */
  public FacetedQueryRequest(
      Query query,
      boolean isEnterprise,
      Collection<String> sourceIds,
      Map<String, Serializable> properties,
      TermFacetProperties facetProperties) {
    super(query, isEnterprise, sourceIds, new HashMap<>(properties));

    this.getProperties().put(EXPERIMENTAL_FACET_PROPERTIES_KEY, facetProperties);
  }
}
