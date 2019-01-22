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
package ddf.catalog.impl.operations

import ddf.catalog.impl.FrameworkProperties
import ddf.catalog.operation.Request
import ddf.catalog.source.CatalogProvider
import ddf.catalog.source.CatalogStore
import spock.lang.Specification

class OperationsCatalogStoreSupportSpec extends Specification {
    private static final String SOURCE_ID = "test_source"

    private List<CatalogProvider> catalogProviders
    private FrameworkProperties frameworkProperties
    private SourceOperations sourceOperations

    def setup() {
        catalogProviders = (1..3).collect { mockCatalogProvider(it) }
        frameworkProperties = new FrameworkProperties()
        frameworkProperties.with {
            catalogProviders = this.catalogProviders
            catalogStores = [cat1: Mock(CatalogStore), cat2: Mock(CatalogStore)]
        }

        sourceOperations = Mock(SourceOperations)
        sourceOperations.getId() >> SOURCE_ID
        sourceOperations.getCatalog() >> { catalogProviders.first() }
    }

    def 'test if catalog store for null request'() {
        setup:
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)

        expect:
        !opsCatalog.isCatalogStoreRequest(null)
    }

    def 'test if catalog store for request with no store ids'() {
        setup:
        def request = Mock(Request)
        request.getStoreIds() >> { null }
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)

        expect:
        !opsCatalog.isCatalogStoreRequest(request)
    }

    def 'test if catalog store for request with local store id only'() {
        setup:
        def request = Mock(Request)
        request.getStoreIds() >> { ['1'] }
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)

        expect:
        !opsCatalog.isCatalogStoreRequest(request)
    }

    def 'test if catalog store for request with non-local store id only'() {
        setup:
        def request = Mock(Request)
        request.getStoreIds() >> { ['foobar'] }
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)

        expect:
        opsCatalog.isCatalogStoreRequest(request)
    }

    def 'test if catalog store for request with multiple stores including local'() {
        setup:
        def request = Mock(Request)
        request.getStoreIds() >> { ['1', SOURCE_ID] }
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)

        expect:
        opsCatalog.isCatalogStoreRequest(request)
    }

    def 'test if catalog store for request with multiple stores not including local'() {
        setup:
        def request = Mock(Request)
        request.getStoreIds() >> { ['foobar', 'baz'] }
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)

        expect:
        opsCatalog.isCatalogStoreRequest(request)
    }

    def 'get stores for null request'() {
        setup:
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)
        def exceptions = [] as Set

        when:
        def response = opsCatalog.getCatalogStoresForRequest(null, exceptions)

        then:
        response.isEmpty()
        exceptions.isEmpty()
    }

    def 'get stores for non-catalog store request'() {
        setup:
        def request = Mock(Request)
        request.getStoreIds() >> { ['1'] }
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)
        def exceptions = [] as Set

        when:
        def response = opsCatalog.getCatalogStoresForRequest(request, exceptions)

        then:
        response.isEmpty()
        exceptions.isEmpty()
    }

    def 'get stores for unknown catalog stores'() {
        setup:
        def request = Mock(Request)
        request.getStoreIds() >> { ['unknown1', 'unknown2'] }
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)
        def exceptions = [] as Set

        when:
        def response = opsCatalog.getCatalogStoresForRequest(request, exceptions)

        then:
        response.isEmpty()
        exceptions.size() == request.getStoreIds().size()
    }

    def 'get stores for catalog stores including one unknown'() {
        setup:
        def request = Mock(Request)
        request.getStoreIds() >> { ['cat1', 'unknown2'] }
        def opsCatalog = new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations)
        def exceptions = [] as Set

        when:
        def response = opsCatalog.getCatalogStoresForRequest(request, exceptions)

        then:
        response == [frameworkProperties.catalogStores.get('cat1')]
        exceptions.size() == 1
    }

    private def mockCatalogProvider(def id) {
        def catProv = Mock(CatalogProvider)
        catProv.getId() >> { return id }
        return catProv
    }
}
