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

import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.SolrClient
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.Specification
import spock.lang.Unroll

@RunWith(JUnitPlatform.class)
class BuildGazetteerSuggesterIndexCommandSpec extends Specification {

    private BuildGazetteerSuggesterIndexCommand buildGazetteerSuggesterIndexCommand = new BuildGazetteerSuggesterIndexCommand()

    private SolrClient mockSolrClient = Mock()

    def 'test executeWithSolrClient'() {
        when:
        buildGazetteerSuggesterIndexCommand.executeWithSolrClient(mockSolrClient)

        then:
        1 * mockSolrClient.query({
            it.getRequestHandler() == "/gazetteer"
            it.getParams("suggest.q") == ["CatalogSolrGazetteerBuildSuggester"]
            it.getParams("suggest.build") == ["true"]
            it.getParams("suggest.dictionary") == ["gazetteerSuggest"]
        })
    }

    @Unroll("test SolrClient query fails with #exceptionType")
    def 'test SolrClient query fails'() {
        given:
        def mockException = Mock(exceptionType)
        mockSolrClient.query(_) >> { throw mockException }

        when:
        buildGazetteerSuggesterIndexCommand.executeWithSolrClient(mockSolrClient)

        then:
        Exception e = thrown()
        e == mockException

        where:
        exceptionType << [IOException, SolrServerException]
    }
}
