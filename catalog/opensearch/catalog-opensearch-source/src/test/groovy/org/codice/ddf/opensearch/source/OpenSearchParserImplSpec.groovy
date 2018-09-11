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
package org.codice.ddf.opensearch.source

import org.apache.cxf.jaxrs.client.WebClient
import spock.lang.Specification

class OpenSearchParserImplSpec extends Specification {

    private OpenSearchParser openSearchParser = new OpenSearchParserImpl()

    private WebClient webClient = Mock(WebClient)

    // {@link OpenSearchParser#populateContextual(WebClient, Map, List)} tests

    def 'test populate contextual'() {
        when:
        openSearchParser.populateContextual(
                webClient,
                [q: searchPhrase],
                ['q', 'src', 'mr', 'start', 'count', 'mt', 'dn', 'lat', 'lon', 'radius', 'bbox', 'polygon', 'dtstart', 'dtend', 'dateName', 'filter', 'sort']
        )

        then:
        1 * webClient.replaceQueryParam('q', searchPhrase)

        where:
        searchPhrase << ['TestQuery123', 'Test Query 123']
    }

    def 'test contextual not populated'() {
        when:
        openSearchParser.populateContextual(
                webClient,
                searchPhraseMap,
                ['q', 'src', 'mr', 'start', 'count', 'mt', 'dn', 'lat', 'lon', 'radius', 'bbox', 'polygon', 'dtstart', 'dtend', 'dateName', 'filter', 'sort']
        )

        then:
        0 * webClient._

        where:
        searchPhraseMap << [[unrecognizedParameter: 'TestQuery123'], [:], null]
    }
}
