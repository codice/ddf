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
package org.codice.ddf.catalog.sourcepoller

import ddf.catalog.source.CatalogProvider
import ddf.catalog.source.CatalogStore
import ddf.catalog.source.ConnectedSource
import ddf.catalog.source.FederatedSource
import ddf.catalog.source.Source
import spock.lang.Specification

import static org.hamcrest.Matchers.containsInAnyOrder

class SourceRegistrySpec extends Specification {

    def 'test set sources with null'(Closure setSources) {
        given:
        final SourceRegistry sourceRegistry = new SourceRegistry()

        when:
        setSources(sourceRegistry, null)

        then:
        thrown NullPointerException

        where:
        setSources << [
                { sr, sources -> sr.setConnectedSources(sources) },
                { sr, sources -> sr.setFederatedSources(sources) },
                { sr, sources -> sr.setCatalogProviders(sources) },
                { sr, sources -> sr.setCatalogStores(sources) }
        ]
    }

    def 'test set sources with empty list'(Closure setSources) {
        given:
        final SourceRegistry sourceRegistry = new SourceRegistry()

        when:
        setSources(sourceRegistry, [])

        then:
        noExceptionThrown()

        where:
        setSources << [
                { sr, sources -> sr.setConnectedSources(sources) },
                { sr, sources -> sr.setFederatedSources(sources) },
                { sr, sources -> sr.setCatalogProviders(sources) },
                { sr, sources -> sr.setCatalogStores(sources) }
        ]
    }

    def 'test getCurrentSources'() {
        given:
        final SourceRegistry sourceRegistry = new SourceRegistry()

        sourceRegistry.setConnectedSources(connectedSources)
        sourceRegistry.setFederatedSources(federatedSources)
        sourceRegistry.setCatalogProviders(catalogProviders)
        sourceRegistry.setCatalogStores(catalogStores)

        expect:
        ((Collection<Source>) sourceRegistry.getCurrentSources()) containsInAnyOrder(connectedSources + federatedSources + catalogProviders + catalogStores as Source[])

        where:
        connectedSources                             | federatedSources                                                   | catalogProviders       | catalogStores
        []                                           | []                                                                 | []                     | []
        []                                           | []                                                                 | [_ as CatalogProvider] | []
        []                                           | [_ as FederatedSource, _ as FederatedSource, _ as FederatedSource] | [_ as CatalogProvider] | []
        [_ as ConnectedSource, _ as ConnectedSource] | [_ as FederatedSource, _ as FederatedSource, _ as FederatedSource] | [_ as CatalogProvider] | [_ as CatalogStore]
    }
}
