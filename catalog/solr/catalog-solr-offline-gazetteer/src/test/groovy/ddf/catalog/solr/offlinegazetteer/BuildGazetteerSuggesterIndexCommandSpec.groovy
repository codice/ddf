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

class BuildGazetteerSuggesterIndexCommandSpec extends Specification {

    private final SolrClient mockSolrClient = Mock(SolrClient)

    private final BuildGazetteerSuggesterIndexCommand buildGazetteerSuggesterIndexCommand = new BuildGazetteerSuggesterIndexCommand(
            clientFactory: Mock(SolrClientFactory) {
                1 * newClient("gazetteer") >> mockSolrClient
            },
            session: Mock(Session) {
                getConsole() >> Mock(PrintStream)
            }
    )

    def 'test execute'() {
        given:
        mockSolrClient.isAvailable() >> true

        when:
        buildGazetteerSuggesterIndexCommand.execute()

        then:
        1 * mockSolrClient.query({
            it.getRequestHandler() == "/gazetteer"
            it.getParams("suggest.q") == ["CatalogSolrGazetteerBuildSuggester"]
            it.getParams("suggest.build") == ["true"]
            it.getParams("suggest.dictionary") == ["gazetteerSuggest"]
        })

        and:
        1 * mockSolrClient.close()
    }

    def 'test SolrClient is not available'() {
        given:
        mockSolrClient.isAvailable() >> false

        when:
        buildGazetteerSuggesterIndexCommand.execute()

        then:
        0 * mockSolrClient.query(_)

        and:
        1 * mockSolrClient.close()
    }

    def 'test SolrClient becomes available'() {
        given:
        mockSolrClient.isAvailable() >> false >> true

        when:
        buildGazetteerSuggesterIndexCommand.execute()

        then:
        1 * mockSolrClient.query({
            it.getRequestHandler() == "/gazetteer"
            it.getParams("suggest.q") == ["CatalogSolrGazetteerBuildSuggester"]
            it.getParams("suggest.build") == ["true"]
            it.getParams("suggest.dictionary") == ["gazetteerSuggest"]
        })

        and:
        1 * mockSolrClient.close()
    }

    @Unroll("test SolrClient query fails with #exceptionType")
    def 'test SolrClient query fails'() {
        given:
        mockSolrClient.isAvailable() >> true
        final Exception mockException = Mock(exceptionType)
        mockSolrClient.query(_) >> { throw mockException }

        when:
        buildGazetteerSuggesterIndexCommand.execute()

        then:
        Exception e = thrown()
        e == mockException

        and:
        1 * mockSolrClient.close()

        where:
        exceptionType << [IOException, SolrServerException, UnavailableSolrException]
    }

    def 'test failure to close SolrClient'() {
        given:
        mockSolrClient.isAvailable() >> true
        final IOException mockIOException = Mock(IOException)
        mockSolrClient.close() >> { throw mockIOException }

        when:
        buildGazetteerSuggesterIndexCommand.execute()

        then:
        1 * mockSolrClient.query({
            it.getRequestHandler() == "/gazetteer"
            it.getParams("suggest.q") == ["CatalogSolrGazetteerBuildSuggester"]
            it.getParams("suggest.build") == ["true"]
            it.getParams("suggest.dictionary") == ["gazetteerSuggest"]
        })

        and:
        Exception e = thrown()
        e == mockIOException
    }
}
