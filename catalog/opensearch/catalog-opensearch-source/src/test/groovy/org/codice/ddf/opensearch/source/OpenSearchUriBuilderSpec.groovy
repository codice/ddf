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

import org.apache.http.client.utils.URIBuilder
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.Specification

@RunWith(JUnitPlatform.class)
class OpenSearchUriBuilderSpec extends Specification {

    private URIBuilder uriBuilder = Mock(URIBuilder)

    // {@link OpenSearchParser#populateContextual(WebClient, Map, List)} tests

    def 'test populate contextual'() {
        when:
        OpenSearchUriBuilder.populateContextual(
                uriBuilder,
                [q: searchPhrase],
                ['q', 'src', 'mr', 'start', 'count', 'mt', 'dn', 'lat', 'lon', 'radius', 'bbox', 'polygon', 'dtstart', 'dtend', 'dateName', 'filter', 'sort']
        )

        then:
        1 * uriBuilder.setParameter('q', searchPhrase)

        where:
        searchPhrase << ['TestQuery123', 'Test Query 123']
    }

    def 'test contextual not populated'() {
        when:
        OpenSearchUriBuilder.populateContextual(
                uriBuilder,
                searchPhraseMap,
                ['q', 'src', 'mr', 'start', 'count', 'mt', 'dn', 'lat', 'lon', 'radius', 'bbox', 'polygon', 'dtstart', 'dtend', 'dateName', 'filter', 'sort']
        )

        then:
        0 * uriBuilder._

        where:
        searchPhraseMap << [[unrecognizedParameter: 'TestQuery123'], [:], null]
    }
}
