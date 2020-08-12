package ddf.catalog.solr.offlinegazetteer

import ddf.catalog.solr.offlinegazetteer.RemoveAllOfflineSolrGazetteerCommand
import net.jodah.failsafe.FailsafeException
import org.apache.karaf.shell.api.console.Session
import org.apache.solr.client.solrj.SolrServerException
import org.codice.solr.client.solrj.SolrClient
import org.codice.solr.factory.SolrClientFactory
import spock.lang.Specification

class RemoveAllOfflineSolrGazetteerCommandSpec extends Specification {
    RemoveAllOfflineSolrGazetteerCommand testedClass
    SolrClientFactory solrClientFactory
    SolrClient solrClient
    Session session

    void setup() {
        solrClient = Mock(SolrClient) {
            isAvailable() >> true
        }
        solrClientFactory = Mock(SolrClientFactory) {
            newClient(_) >> solrClient
        }
        session = Mock(Session)

        testedClass = new RemoveAllOfflineSolrGazetteerCommand()
        testedClass.clientFactory = solrClientFactory
        testedClass.session = session
        testedClass.force = true
        session.getConsole() >> new PrintStream(new ByteArrayOutputStream(128))
    }

    def "execute nominal"() {
        when:
        testedClass.execute()

        then:
        1 * solrClient.deleteByQuery(*_)
    }

    def "execute solr exception"() {
        setup:
        1 * solrClient.deleteByQuery(*_) >> { throw new RuntimeException("exception") }

        when:
        testedClass.execute()

        then:
        RuntimeException e = thrown()
    }

}
