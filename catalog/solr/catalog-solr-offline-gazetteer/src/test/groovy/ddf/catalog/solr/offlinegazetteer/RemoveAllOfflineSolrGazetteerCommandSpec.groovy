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
import org.codice.solr.client.solrj.SolrClient
import org.codice.solr.client.solrj.UnavailableSolrException
import spock.lang.Specification
import spock.lang.Unroll

class RemoveAllOfflineSolrGazetteerCommandSpec extends Specification {

    private final RemoveAllOfflineSolrGazetteerCommand removeAllOfflineSolrGazetteerCommand = new RemoveAllOfflineSolrGazetteerCommand()

    private final SolrClient mockSolrClient = Mock()

    def 'test executeWithSolrClient'() {
        when:
        removeAllOfflineSolrGazetteerCommand.executeWithSolrClient(mockSolrClient)

        then:
        1 * mockSolrClient.deleteByQuery("*:*")
    }

    @Unroll("test SolrClient query fails with #exceptionType")
    def 'test SolrClient query fails'() {
        given:
        final mockException = Mock(exceptionType)
        mockSolrClient.deleteByQuery(_) >> { throw mockException }

        when:
        removeAllOfflineSolrGazetteerCommand.executeWithSolrClient(mockSolrClient)

        then:
        Exception e = thrown()
        e == mockException

        where:
        exceptionType << [IOException, SolrServerException, UnavailableSolrException]
    }

}
