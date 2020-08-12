package ddf.catalog.solr.offlinegazetteer

import ddf.catalog.CatalogFramework
import ddf.catalog.data.Metacard
import ddf.catalog.data.Result
import ddf.catalog.filter.AttributeBuilder
import ddf.catalog.filter.ContextualExpressionBuilder
import ddf.catalog.filter.FilterBuilder
import ddf.catalog.operation.QueryRequest
import ddf.catalog.operation.QueryResponse
import ddf.catalog.solr.offlinegazetteer.SyncCatalogCommand
import ddf.catalog.source.SourceUnavailableException
import ddf.catalog.util.impl.CatalogQueryException
import org.apache.karaf.shell.api.console.Session
import org.apache.solr.client.solrj.SolrServerException
import org.codice.ddf.security.Security
import org.codice.solr.client.solrj.SolrClient
import org.codice.solr.factory.SolrClientFactory
import spock.lang.Specification

class SyncCatalogCommandSpec extends Specification {
    SyncCatalogCommand testedClass
    SolrClientFactory solrClientFactory
    SolrClient solrClient
    Session session
    CatalogFramework catalogFramework
    FilterBuilder filterBuilder
    Security security

    void setup() {
        solrClient = Mock(SolrClient) {
            isAvailable() >> true
        }
        solrClientFactory = Mock(SolrClientFactory) {
            newClient(_) >> solrClient
        }

        session = Mock(Session)
        catalogFramework = Mock(CatalogFramework)
        filterBuilder = Mock(FilterBuilder)
        security = Mock(Security)

        testedClass = new SyncCatalogCommand()
        testedClass.clientFactory = solrClientFactory
        testedClass.session = session
        testedClass.catalogFramework = catalogFramework
        testedClass.filterBuilder = filterBuilder
        testedClass.security = security

        filterBuilder.attribute(_) >> Mock(AttributeBuilder) {
            like() >> Mock(ContextualExpressionBuilder) {
                text(_) >> Mock(org.opengis.filter.Filter)
            }
        }

        session.getConsole() >> new PrintStream(new ByteArrayOutputStream(128))
    }

    def "executeWithSubject nominal"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
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


        when:
        testedClass.executeWithSubject()

        then:
        1 * solrClient.add(*_)

    }

    def "executeWithSubject catalog framework exception"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >>
                { throw new SourceUnavailableException("exception") }

        when:
        testedClass.executeWithSubject()

        then:
        CatalogQueryException e = thrown()

    }

    def "executeWithSubject solr exception "() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
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
        solrClient.add(*_) >> { throw new SolrServerException("exception") }


        when:
        testedClass.executeWithSubject()

        then:
        SolrServerException e = thrown()

    }
}
