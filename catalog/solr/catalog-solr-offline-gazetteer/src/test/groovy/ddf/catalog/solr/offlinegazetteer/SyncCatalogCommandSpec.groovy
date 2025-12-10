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
import org.apache.solr.client.solrj.SolrClient
import org.geotools.api.filter.Filter
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.Specification
import spock.lang.Unroll

@RunWith(JUnitPlatform.class)
class SyncCatalogCommandSpec extends Specification {

    private CatalogFramework mockCatalogFramework = Mock()

    private SyncCatalogCommand syncCatalogCommand = new SyncCatalogCommand().with {
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

    private SolrClient mockSolrClient = Mock()

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
        def mockException = Mock(exceptionType)
        mockCatalogFramework.query(_) >> { throw mockException }

        when:
        syncCatalogCommand.executeWithSolrClient(mockSolrClient)

        then:
        def CatalogQueryException e = thrown()
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
        def mockException = Mock(exceptionType)
        mockSolrClient.add(*_) >> { throw mockException }

        when:
        syncCatalogCommand.executeWithSolrClient(mockSolrClient)

        then:
        Exception e = thrown()
        e == mockException

        where:
        exceptionType << [IOException, SolrServerException]
    }
}
