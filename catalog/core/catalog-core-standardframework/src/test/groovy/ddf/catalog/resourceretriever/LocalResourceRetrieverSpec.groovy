package ddf.catalog.resourceretriever

import ddf.catalog.data.Attribute
import ddf.catalog.data.Metacard
import ddf.catalog.data.types.Core
import ddf.catalog.operation.ResourceResponse
import ddf.catalog.resource.Resource
import ddf.catalog.resource.ResourceReader
import spock.lang.Specification

class LocalResourceRetrieverSpec extends Specification {

    String TEST_SCHEME = "testScheme"

    String TEST_QUALIFIER = "testQualifier"

    ResourceRetriever localResourceReader

    ResourceReader resourceReader

    Metacard resourceMetacard

    def setup() {
        resourceReader = Mock(ResourceReader)
        resourceMetacard = Mock(Metacard)
    }

    def 'test qualifier in resource props retrieves qualified content'() {
        setup:
        resourceReader.getSupportedSchemes() >> [TEST_SCHEME]
        def mockResourceResponse = mockResourceResponse()
        def props = [qualifier: TEST_QUALIFIER]

        def derivedUri = new URI(TEST_SCHEME, "someid", TEST_QUALIFIER)

        Attribute derivedResourceUriAttr = Mock(Attribute)
        derivedResourceUriAttr.getValues() >> [derivedUri.toASCIIString()]
        resourceMetacard.getAttribute(Core.DERIVED_RESOURCE_URI) >> derivedResourceUriAttr

        def mainUri = new URI("doesnt:matter")
        localResourceReader = new LocalResourceRetriever([resourceReader], mainUri, resourceMetacard, props)

        when:
        localResourceReader.retrieveResource()

        then:
        1 * resourceReader.retrieveResource(derivedUri, props) >> mockResourceResponse
    }

    def 'test no qualifier in resource props retrieves original resourceUri'() {
        setup:
        resourceReader.getSupportedSchemes() >> [TEST_SCHEME]
        def mockResourceResponse = mockResourceResponse()

        def mainUri = new URI(TEST_SCHEME + ":someid")
        localResourceReader = new LocalResourceRetriever([resourceReader], mainUri, resourceMetacard, [:])

        when:
        localResourceReader.retrieveResource()

        then:
        1 * resourceReader.retrieveResource(mainUri, [:]) >> mockResourceResponse
    }

    def mockResourceResponse() {
        def mockResource = Mock(Resource)
        mockResource.getName() >> "resourceName"

        def mockResourceResponse = Mock(ResourceResponse)
        mockResourceResponse.getResource() >> mockResource
        return mockResourceResponse
    }
}
