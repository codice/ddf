/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.opensearch.endpoint

import ddf.catalog.CatalogFramework
import ddf.catalog.data.Result
import ddf.catalog.filter.AttributeBuilder
import ddf.catalog.filter.ContextualExpressionBuilder
import ddf.catalog.filter.ExpressionBuilder
import ddf.catalog.filter.FilterBuilder
import org.opengis.filter.sort.SortOrder
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.UriInfo

class OpenSearchEndpointSpec extends Specification {

    private static final String DEFAULT_SORT_FIELD = Result.RELEVANCE

    private static final SortOrder DEFAULT_SORT_ORDER = SortOrder.DESCENDING

    @Unroll
    def 'test parsing sort parameter "#sort"'() {
        given:
        def sortBy
        final catalogFramework = Mock(CatalogFramework)
        final endpoint = new OpenSearchEndpoint(catalogFramework, Mock(FilterBuilder) {
            attribute(_) >> Mock(AttributeBuilder) {
                is() >> Mock(ExpressionBuilder) {
                    like() >> Mock(ContextualExpressionBuilder) {
                        text(_) >> null
                    }
                }
            }
        })

        when:
        endpoint.processQuery(null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sort,
                null,
                null,
                Mock(UriInfo) {
                    getQueryParameters() >> Mock(MultivaluedMap) {
                        get(_) >> null
                    }
                    getRequestUri() >> new URI()
                },
                null,
                null,
                Mock(HttpServletRequest) {
                    getParameterMap() >> [:]
                })

        then:
        1 * catalogFramework.transform({
            sortBy = it.getRequest().getQuery().getSortBy()
        }, _, _)
        sortBy.getPropertyName().getPropertyName() == expectedSortField
        sortBy.getSortOrder() == expectedSortOrder

        where:
        sort                                             || expectedSortField  | expectedSortOrder
        ""                                               || DEFAULT_SORT_FIELD | DEFAULT_SORT_ORDER
        "some string that is not in the expected format" || DEFAULT_SORT_FIELD | DEFAULT_SORT_ORDER
        "date"                                           || Result.TEMPORAL    | DEFAULT_SORT_ORDER
        "date:asc"                                       || Result.TEMPORAL    | SortOrder.ASCENDING
        "date:desc"                                      || Result.TEMPORAL    | SortOrder.DESCENDING
        "relevance"                                      || Result.RELEVANCE   | DEFAULT_SORT_ORDER
        "relevance:desc"                                 || Result.RELEVANCE   | SortOrder.DESCENDING
    }
}