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

import ddf.catalog.data.Metacard
import ddf.catalog.data.Result
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder
import ddf.catalog.impl.FrameworkProperties
import ddf.catalog.operation.QueryResponse
import ddf.catalog.operation.ResourceRequest
import ddf.catalog.operation.ResourceResponse
import ddf.catalog.plugin.PostResourcePlugin
import ddf.catalog.plugin.PreResourcePlugin
import ddf.catalog.resource.Resource
import ddf.catalog.resource.ResourceNotFoundException
import ddf.catalog.resource.ResourceNotSupportedException
import ddf.catalog.resource.download.DefaultDownloadManager
import ddf.catalog.resourceretriever.ResourceRetriever
import ddf.catalog.source.FederatedSource
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.Specification
import spock.lang.Unroll

@RunWith(JUnitPlatform.class)
class ResourceOperationsSpec extends Specification {
    private FrameworkProperties frameworkProperties
    private QueryOperations queryOperations
    private ResourceOperations resourceOperations
    private URI uri
    private Metacard metacard

    def setup() {
        uri = getClass().getResource('/ddf/catalog/impl/operations/ResourceOperationsTest.class').toURI()

        metacard = Mock(Metacard)
        metacard.getSourceId() >> 'sourceid1'
        metacard.getId() >> 'metaid1'
        metacard.getResourceURI() >> uri

        def result = Mock(Result)
        result.getMetacard() >> metacard

        def queryResponse = Mock(QueryResponse)
        queryResponse.getResults() >> [result]
        queryOperations = Mock(QueryOperations)
        queryOperations.query(_, _, _, _) >> queryResponse

        frameworkProperties = new FrameworkProperties()
        frameworkProperties.setFilterBuilder(new GeotoolsFilterBuilder())
        resourceOperations = new ResourceOperations(frameworkProperties, queryOperations)
    }

    @Unroll
    def 'get resource no resource name given'() {
        when:
        resourceOperations.getResource(null, enterprise, null, fanout)

        then:
        thrown(expected_exception)

        where:
        enterprise | fanout || expected_exception
        false      | false  || ResourceNotFoundException
        false      | true   || ResourceNotSupportedException
        true       | false  || ResourceNotSupportedException
    }

    def 'get resource when no attribute given'() {
        setup:
        def request = Mock(ResourceRequest)

        when:
        resourceOperations.getResource(request, false, 'resourcename', false)

        then:
        1 * request.getAttributeValue() >> null
        thrown(ResourceNotSupportedException)

        when:
        resourceOperations.getResource(request, false, 'resourcename', false)

        then:
        1 * request.getAttributeValue() >> { Mock(Serializable) }
        1 * request.getAttributeName() >> { null }
        thrown(ResourceNotSupportedException)
    }

    def 'confirm plugins are invoked in correct order'() {
        setup:
        def request = Mock(ResourceRequest)
        request.getProperties() >> { [:] }
        request.getAttributeName() >> { ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI }
        request.getAttributeValue() >> uri
        def resourceResponse = Mock(ResourceResponse)
        resourceResponse.getResource() >> Mock(Resource)
        resourceResponse.getProperties() >> [:]

        and:
        def downManager = Spy(DefaultDownloadManager)
        downManager.download(request, metacard, !null as ResourceRetriever) >> resourceResponse
        frameworkProperties.with {
            preResource = 3.collect { Mock(PreResourcePlugin) }
            postResource = 4.collect { Mock(PostResourcePlugin) }
            defaultDownloadManager = downManager
            federatedSources = [Mock(FederatedSource) {
                getId() >> 'resourcename'
            }]
        }

        when:
        resourceOperations.getResource(request, false, 'resourcename', false)

        then:
        frameworkProperties.preResource.each {
            1 * it.process(request) >> request
        }

        then:
        frameworkProperties.postResource.each {
            1 * it.process(_ as ResourceResponse) >> { ResourceResponse res -> return res }
        }
    }

}