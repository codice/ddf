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
package org.codice.ddf.catalog.plugin.gazetteer

import ddf.catalog.CatalogFramework
import ddf.catalog.data.types.Core
import ddf.catalog.filter.AttributeBuilder
import ddf.catalog.filter.FilterBuilder
import ddf.catalog.operation.QueryRequest
import ddf.catalog.source.SourceUnavailableException
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.Specification

import static ddf.catalog.Constants.SUGGESTION_BUILD_KEY
import static ddf.catalog.Constants.SUGGESTION_CONTEXT_KEY
import static ddf.catalog.Constants.SUGGESTION_DICT_KEY
import static ddf.catalog.Constants.SUGGESTION_QUERY_KEY
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.GAZETTEER_METACARD_TAG
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.SUGGEST_PLACE_KEY

@RunWith(JUnitPlatform.class)
class BuildSuggesterIndexSpec extends Specification {
    def catalogFramework = Mock(CatalogFramework)
    def filterBuilder = Stub(FilterBuilder) {
        attribute(Core.METACARD_TAGS) >> Stub(AttributeBuilder)
    }
    def buildSuggesterIndex = new BuildSuggesterIndex(catalogFramework, filterBuilder)

    def "sends a query request with the parameters needed to build the suggester index"() {
        when:
        buildSuggesterIndex.run()

        then:
        1 * catalogFramework.query((QueryRequest) {
            it.getPropertyValue(SUGGESTION_QUERY_KEY) == "anything"
            it.getPropertyValue(SUGGESTION_CONTEXT_KEY) == GAZETTEER_METACARD_TAG
            it.getPropertyValue(SUGGESTION_DICT_KEY) == SUGGEST_PLACE_KEY
            it.getPropertyValue(SUGGESTION_BUILD_KEY) == true
        })
    }

    def "does not propagate exceptions thrown by the catalog framework"() {
        when:
        buildSuggesterIndex.run()

        then:
        1 * catalogFramework.query(_ as QueryRequest) >> { throw new SourceUnavailableException() }
    }
}
