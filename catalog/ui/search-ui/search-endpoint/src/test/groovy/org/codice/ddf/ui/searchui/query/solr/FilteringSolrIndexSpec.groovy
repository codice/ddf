/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.ui.searchui.query.solr

import ddf.catalog.data.Metacard
import ddf.catalog.data.Result
import ddf.catalog.operation.CreateRequest
import ddf.catalog.operation.CreateResponse
import ddf.catalog.operation.QueryRequest
import ddf.catalog.source.solr.SolrCatalogProvider
import spock.lang.Specification

class FilteringSolrIndexSpec extends Specification {

    def provider = Mock(SolrCatalogProvider)

    def index = new FilteringSolrIndex(provider)

    def "Can shutdown Solr index"() {
        when:
        index.shutdown()

        then:
        1 * provider.shutdown()
    }

    def "Can query index"() {
        setup:
        def request = Mock(QueryRequest)

        when:
        index.query(request)

        then:
        1 * provider.query(request)
    }

    def "Can add results to index"() {
        setup:
        def result = Mock(Result) {
            getMetacard() >> Mock(Metacard)
        }
        def results = [result]

        when:
        index.add(results)

        then:
        1 * provider.create(_ as CreateRequest)
    }

    def "Does not add invalid results"() {
        when:
        index.add([result])

        then:
        1 * provider.create(_) >> { CreateRequest request ->
            assert request.getMetacards().size() == 0
            Mock(CreateResponse)
        }

        where:
        result          | _
        Mock(Result)    | _
        null            | _
    }

}
