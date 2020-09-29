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
package ddf.catalog.solr.offlinegazetteer

import ddf.catalog.CatalogFramework
import ddf.catalog.data.Metacard
import ddf.catalog.data.Result
import ddf.catalog.federation.FederationException
import ddf.catalog.filter.AttributeBuilder
import ddf.catalog.filter.ContextualExpressionBuilder
import ddf.catalog.filter.FilterBuilder
import ddf.catalog.operation.QueryRequest
import ddf.catalog.operation.QueryResponse
import ddf.catalog.source.SourceUnavailableException
import ddf.catalog.source.UnsupportedQueryException
import ddf.catalog.util.impl.CatalogQueryException
import org.apache.solr.client.solrj.SolrServerException
import org.codice.solr.client.solrj.SolrClient
import org.codice.solr.client.solrj.UnavailableSolrException
import spock.lang.Specification
import org.opengis.filter.Filter
import spock.lang.Unroll

class SyncCatalogCommandSpec extends Specification {

    private final CatalogFramework mockCatalogFramework = Mock()

    private final SyncCatalogCommand syncCatalogCommand = new SyncCatalogCommand().with {
        catalogFramework = mockCatalogFramework
        filterBuilder = Mock(FilterBuilder) {
            attribute(_) >> Mock(AttributeBuilder) {
                like() >> Mock(ContextualExpressionBuilder) {
                    text(_) >> Mock(Filter)
                }
            }
        }
        return it
    }

    private final SolrClient mockSolrClient = Mock()

    def 'test executeWithSolrClient'() {
        when:
        syncCatalogCommand.executeWithSolrClient(mockSolrClient)

        then:
        1 * mockCatalogFramework.query(_ as QueryRequest) >> Mock(QueryResponse) {
            getResults() >> [Mock(Result) {
                getMetacard() >> Mock(Metacard) {
                    getId() >> "id1"
                }
            }]
            getProperties() >> ["actualResultSize": 1]
            getHits() >> 1
        }

        and:
        1 * mockSolrClient.add(*_)
    }

    @Unroll("test CatalogFramework query fails with #exceptionType")
    def 'test CatalogFramework query fails'() {
        given:
        final mockException = Mock(exceptionType)
        mockCatalogFramework.query(_) >> { throw mockException }

        when:
        syncCatalogCommand.executeWithSolrClient(mockSolrClient)

        then:
        final CatalogQueryException e = thrown()
        e.getCause() == mockException

        where:
        exceptionType << [UnsupportedQueryException, SourceUnavailableException, FederationException]
    }

    def 'test test SolrClient add fails'() {
        setup:
        mockCatalogFramework.query(_ as QueryRequest) >> {
            Mock(QueryResponse) {
                getResults() >> [Mock(Result) {
                    getMetacard() >> Mock(Metacard) {
                        getId() >> "id1"
                    }
                }]
                getProperties() >> ["actualResultSize": 1]
                getHits() >> 1
            }
        }

        and:
        final mockException = Mock(exceptionType)
        mockSolrClient.add(*_) >> { throw mockException }

        when:
        syncCatalogCommand.executeWithSolrClient(mockSolrClient)

        then:
        Exception e = thrown()
        e == mockException

        where:
        exceptionType << [IOException, SolrServerException, UnavailableSolrException]
    }
}
