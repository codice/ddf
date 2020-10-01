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

import org.apache.karaf.shell.api.console.Session
import org.apache.solr.client.solrj.SolrServerException
import org.codice.solr.client.solrj.SolrClient
import org.codice.solr.client.solrj.UnavailableSolrException
import org.codice.solr.factory.SolrClientFactory
import spock.lang.Specification
import spock.lang.Unroll

class AbstractSolrClientCommandSpec extends Specification {

    private final AbstractSolrClientCommand spyAbstractOfflineSolrGazetteerCommand = Spy(AbstractSolrClientCommand)

    def 'test force with \"no\" user input'() {
        given:
        spyAbstractOfflineSolrGazetteerCommand.session = Mock(Session) {
            readLine("Are you sure you want to continue? (y/n): ", null) >> answer
        }

        when:
        spyAbstractOfflineSolrGazetteerCommand.executeWithSubject()

        then:
        0 * spyAbstractOfflineSolrGazetteerCommand.executeWithSolrClient(_) >> {}

        where:
        answer << ["n", "no", "N", "No", "NO", "not no"]
    }

    def 'test force with \"yes\" user input'() {
        given:
        final SolrClient mockSolrClient = Mock() {
            isAvailable() >> true
        }
        with(spyAbstractOfflineSolrGazetteerCommand) {
            clientFactory = Mock(SolrClientFactory) {
                1 * newClient("gazetteer") >> mockSolrClient
            }
            session = Mock(Session) {
                readLine("Are you sure you want to continue? (y/n): ", null) >> answer
            }
        }

        when:
        spyAbstractOfflineSolrGazetteerCommand.executeWithSubject()

        then:
        1 * spyAbstractOfflineSolrGazetteerCommand.executeWithSolrClient(mockSolrClient) >> {}

        and:
        1 * mockSolrClient.close()

        where:
        answer << ["y", "yes", "Y", "Yes", "YEs"]
    }

    def 'test executeWithSolrClient'() {
        given:
        final SolrClient mockSolrClient = Mock() {
            isAvailable() >> true
        }
        spyAbstractOfflineSolrGazetteerCommand.with {
            clientFactory = Mock(SolrClientFactory) {
                1 * newClient("gazetteer") >> mockSolrClient
            }
            force = true;
        }

        when:
        spyAbstractOfflineSolrGazetteerCommand.executeWithSubject()

        then:
        1 * spyAbstractOfflineSolrGazetteerCommand.executeWithSolrClient(mockSolrClient) >> {}

        and:
        1 * mockSolrClient.close()
    }

    def 'test SolrClient is not available'() {
        given:
        final SolrClient mockSolrClient = Mock() {
            isAvailable() >> false
        }
        spyAbstractOfflineSolrGazetteerCommand.with {
            clientFactory = Mock(SolrClientFactory) {
                1 * newClient("gazetteer") >> mockSolrClient
            }
            force = true;
        }

        when:
        spyAbstractOfflineSolrGazetteerCommand.executeWithSubject()

        then:
        0 * spyAbstractOfflineSolrGazetteerCommand.executeWithSolrClient(_)

        and:
        1 * mockSolrClient.close()
    }

    def 'test SolrClient becomes available'() {
        given:
        final SolrClient mockSolrClient = Mock() {
            isAvailable() >> false >> true
        }
        spyAbstractOfflineSolrGazetteerCommand.with {
            clientFactory = Mock(SolrClientFactory) {
                1 * newClient("gazetteer") >> mockSolrClient
            }
            force = true;
        }

        when:
        spyAbstractOfflineSolrGazetteerCommand.executeWithSubject()

        then:
        1 * spyAbstractOfflineSolrGazetteerCommand.executeWithSolrClient(mockSolrClient) >> {}

        and:
        1 * mockSolrClient.close()
    }

    @Unroll("test executeWithSolrClient fails with #exceptionType")
    def 'test executeWithSolrClient fails'() {
        given:
        final SolrClient mockSolrClient = Mock() {
            isAvailable() >> true
        }
        spyAbstractOfflineSolrGazetteerCommand.with {
            clientFactory = Mock(SolrClientFactory) {
                1 * newClient("gazetteer") >> mockSolrClient
            }
            force = true;
        }
        final mockException = Mock(exceptionType)
        spyAbstractOfflineSolrGazetteerCommand.executeWithSolrClient(mockSolrClient) >> { throw mockException }

        when:
        spyAbstractOfflineSolrGazetteerCommand.executeWithSubject()

        then:
        Exception e = thrown()
        e == mockException

        and:
        1 * mockSolrClient.close()

        where:
        exceptionType << [IOException, SolrServerException, UnavailableSolrException, RuntimeException, InterruptedException]
    }

    def 'test failure to close SolrClient'() {
        given:
        final IOException mockIOException = Mock()
        final SolrClient mockSolrClient = Mock() {
            isAvailable() >> true
            close() >> { throw mockIOException }
        }
        spyAbstractOfflineSolrGazetteerCommand.with {
            clientFactory = Mock(SolrClientFactory) {
                1 * newClient("gazetteer") >> mockSolrClient
            }
            force = true;
        }

        when:
        spyAbstractOfflineSolrGazetteerCommand.executeWithSubject()

        then:
        1 * spyAbstractOfflineSolrGazetteerCommand.executeWithSolrClient(mockSolrClient) >> {}

        and:
        Exception e = thrown()
        e == mockIOException
    }
}
