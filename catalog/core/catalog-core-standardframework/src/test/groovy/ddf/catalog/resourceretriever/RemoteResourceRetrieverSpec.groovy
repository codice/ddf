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
package ddf.catalog.resourceretriever

import ddf.catalog.operation.ResourceResponse
import ddf.catalog.resource.ResourceNotFoundException
import ddf.catalog.source.RemoteSource
import spock.lang.Specification

class RemoteResourceRetrieverSpec extends Specification {
    private URI uri
    private Map<String, Serializable> props
    private RemoteSource remoteSource
    private ResourceResponse mockResponse

    def setup() {
        uri = getClass().getResource('/ddf/catalog/resourceretriever/RemoteResourceRetrieverSpec.class').toURI()
        props = [:]
        remoteSource = Mock(RemoteSource)
        mockResponse = Mock(ResourceResponse)
    }

    def 'test no resource uri'() {
        setup:
        def retriever = new RemoteResourceRetriever(Mock(RemoteSource), null, null)

        when:
        retriever.retrieveResource()

        then:
        thrown(ResourceNotFoundException)
    }

    def 'test real resource with no bytes to skip'() {
        setup:
        def retriever = new RemoteResourceRetriever(remoteSource, this.uri, props)

        when:
        def response = retriever.retrieveResource(0)

        then:
        1 * remoteSource.retrieveResource(_ as URI, _ as Map<String, Serializable>) >> mockResponse

        response == mockResponse
    }

    def 'test real resource with bytes to skip'() {
        setup:
        def retriever = new RemoteResourceRetriever(remoteSource, this.uri, props)

        when:
        def response = retriever.retrieveResource(bytesToSkip)

        then:
        1 * remoteSource.retrieveResource(_ as URI,
                { it.get(ResourceRetriever.BYTES_TO_SKIP) == bytesToSkip }) >> mockResponse

        response == mockResponse

        where:
        bytesToSkip << [1, 100, 4096, 4097]
    }
}
