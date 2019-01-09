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
package org.codice.ddf.catalog.async.processingplugin

import ddf.catalog.CatalogFramework
import ddf.catalog.data.Attribute
import ddf.catalog.data.Metacard
import ddf.catalog.operation.CreateRequest
import ddf.catalog.operation.CreateResponse
import ddf.catalog.operation.DeleteRequest
import ddf.catalog.operation.DeleteResponse
import ddf.catalog.operation.ResourceRequest
import ddf.catalog.operation.Update
import ddf.catalog.operation.UpdateRequest
import ddf.catalog.operation.UpdateResponse
import ddf.catalog.resource.Resource
import ddf.catalog.resource.ResourceNotFoundException
import ddf.catalog.resource.ResourceNotSupportedException
import ddf.security.SecurityConstants
import ddf.security.Subject
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem
import org.codice.ddf.catalog.async.data.impl.LazyProcessResourceImpl
import org.codice.ddf.catalog.async.processingframework.api.internal.ProcessingFramework
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.function.Supplier

class ProcessingPostIngestPluginSpec extends Specification {

    private ProcessingPostIngestPlugin processingPostIngestPlugin

    private CatalogFramework catalogFramework = Mock(CatalogFramework)

    private ProcessingFramework processingFramework = Mock(ProcessingFramework)

    def setup() {
        processingPostIngestPlugin = new ProcessingPostIngestPlugin(catalogFramework, processingFramework)
    }

    def 'test construct with null arguments'(CatalogFramework catalogFramework, ProcessingFramework processingFramework) {
        when:
        new ProcessingPostIngestPlugin(catalogFramework, processingFramework)

        then:
        thrown(IllegalArgumentException)

        where:
        catalogFramework       | processingFramework
        null                   | Mock(ProcessingFramework)
        Mock(CatalogFramework) | null
    }

    /*
    test process(CreateResponse)
     */

    def 'test process CreateResponse with many metacards'() {
        given:
        def sourceId = "source"
        def metacard1 = mockMetacard("1", sourceId)
        def metacard2 = mockMetacard("2", sourceId)
        def metacard3 = mockMetacard("3", sourceId)

        def responseProperties = propertiesWithSubject()

        def input = Mock(CreateResponse) {
            getRequest() >> Mock(CreateRequest) {
                getProperties() >> properties
            }

            getCreatedMetacards() >> [metacard1, metacard2, metacard3]

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        0 * catalogFramework._
        1 * processingFramework.submitCreate(_ as ProcessRequest<ProcessCreateItem>) >> { ProcessRequest<ProcessCreateItem> processCreateRequest ->
            assert processCreateRequest.getProcessItems().size() == 3

            final processCreateItem1 = processCreateRequest.getProcessItems().get(0)
            assert processCreateItem1.getMetacard() == metacard1

            final processCreateItem2 = processCreateRequest.getProcessItems().get(1)
            assert processCreateItem2.getMetacard() == metacard2

            final processCreateItem3 = processCreateRequest.getProcessItems().get(2)
            assert processCreateItem3.getMetacard() == metacard3

            assert postProcessCompleteEntryAdded(processCreateRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), [:]]
    }

    def 'test process CreateResponse with processResource populated from metacard'() {
        given:
        String sourceId = "source"
        Metacard metacard = mockMetacard("metacardId", sourceId)
        URI expectedUri = new URI("content:ac39ae14d22d4bbba39148973a70be39#frag")
        String expectedQualifier = "frag"
        long expectedSize = 3914

        metacard.getAttribute("resource-uri") >> mockAttribute("resource-uri", "content:ac39ae14d22d4bbba39148973a70be39#frag")
        metacard.getAttribute("resource-size") >> mockAttribute("name", Long.toString(expectedSize))

        def responseProperties = propertiesWithSubject()

        def input = Mock(CreateResponse) {
            getRequest() >> Mock(CreateRequest) {
                getProperties() >> properties
            }

            getCreatedMetacards() >> [metacard]

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input);

        then:
        output == input

        0 * catalogFramework._
        1 * processingFramework.submitCreate(_ as ProcessRequest<ProcessCreateItem>) >> { ProcessRequest<ProcessCreateItem> processCreateRequest ->
            assert processCreateRequest.getProcessItems().size() == 1

            final processCreateItem = processCreateRequest.getProcessItems().get(0)
            assert processCreateItem.getMetacard() == metacard

            final LazyProcessResourceImpl processResource = processCreateItem.getProcessResource()
            assert processResource.getUri() == expectedUri
            assert processResource.getQualifier() == expectedQualifier
            assert processResource.getSize() == expectedSize

        }

    }

    def 'test process CreateResponse with processResource populated from metacard with a non long size and uri without a qualifier'() {
        String sourceId = "source"
        Metacard metacard = mockMetacard("metacardId", sourceId)
        URI expectedUri = new URI("content:ac39ae14d22d4bbba39148973a70be39")
        long expectedSize = -1

        metacard.getAttribute("resource-uri") >> mockAttribute("resource-uri", "content:ac39ae14d22d4bbba39148973a70be39")
        metacard.getAttribute("resource-size") >> mockAttribute("name", "3914 MB")

        def responseProperties = propertiesWithSubject()

        def input = Mock(CreateResponse) {
            getRequest() >> Mock(CreateRequest) {
                getProperties() >> properties
            }

            getCreatedMetacards() >> [metacard]

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input);

        then:
        output == input

        0 * catalogFramework._
        1 * processingFramework.submitCreate(_ as ProcessRequest<ProcessCreateItem>) >> { ProcessRequest<ProcessCreateItem> processCreateRequest ->
            assert processCreateRequest.getProcessItems().size() == 1

            final processCreateItem = processCreateRequest.getProcessItems().get(0)
            assert processCreateItem.getMetacard() == metacard

            final LazyProcessResourceImpl processResource = processCreateItem.getProcessResource()
            assert processResource.getUri() == expectedUri
            assert processResource.getQualifier() == null
            assert processResource.getSize() == expectedSize

        }

    }

    def 'test process CreateResponse invalid already-processed request property'() {
        setup:
        def input = Mock(CreateResponse) {
            getRequest() >> Mock(CreateRequest) {
                getProperties() >> createNotBooleanProcessingCompleteProperties()
            }

            getCreatedMetacards() >> []
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        0 * processingFramework.submitCreate(_ as ProcessRequest<ProcessCreateItem>)
        !input.getRequest().getProperties().containsKey(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE)
    }

    def 'test process CreateResponse with no created metacards'() {
        given:
        def responseProperties = [a: _]

        def input = Mock(CreateResponse) {
            getRequest() >> Mock(CreateRequest) {
                getProperties() >> properties
            }

            getCreatedMetacards() >> []

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        0 * catalogFramework._
        1 * processingFramework.submitCreate(_ as ProcessRequest<ProcessCreateItem>) >> { ProcessRequest<ProcessCreateItem> processCreateRequest ->
            assert processCreateRequest.getProcessItems().isEmpty()
            assert postProcessCompleteEntryAdded(processCreateRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), [:]]
    }

    def 'test CreateResponse with null input and null metacards'() {
        when:
        def output = processingPostIngestPlugin.process(input as CreateResponse)

        then:
        output == input
        0 * processingFramework._

        where:
        input << [null, Mock(CreateResponse) {
            getCreatedMetacards() >> null
        }, Mock(CreateResponse) {
            getCreatedMetacards() >> []
            getRequest() >> Mock(CreateRequest) {
                getProperties() >> createProcessingCompleteProperties()
            }
        }]
    }

    /*
    test process(UpdateResponse)
     */

    def 'test process UpdateResponse'() {
        given:
        def oldCard1 = Mock(Metacard)
        def newCard1 = mockMetacard("1", "s1")
        def oldCard2 = Mock(Metacard)
        def newCard2 = mockMetacard("2", "s2")
        def oldCard3 = Mock(Metacard)
        def newCard3 = mockMetacard("3", "s3")

        def responseProperties = propertiesWithSubject()

        def input = Mock(UpdateResponse) {
            getRequest() >> Mock(UpdateRequest) {
                getProperties() >> properties
            }

            getUpdatedMetacards() >> {
                [Mock(Update) {
                    getOldMetacard() >> oldCard1
                    getNewMetacard() >> newCard1
                }, Mock(Update) {
                    getOldMetacard() >> oldCard2
                    getNewMetacard() >> newCard2
                }, Mock(Update) {
                    getOldMetacard() >> oldCard3
                    getNewMetacard() >> newCard3
                }]
            }

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        0 * catalogFramework._
        1 * processingFramework.submitUpdate(_ as ProcessRequest<ProcessUpdateItem>) >> { ProcessRequest<ProcessUpdateItem> processUpdateRequest ->
            assert processUpdateRequest.getProcessItems().size() == 3
            verifyUpdate(processUpdateRequest.getProcessItems().get(0), newCard1, oldCard1)
            verifyUpdate(processUpdateRequest.getProcessItems().get(1), newCard2, oldCard2)
            verifyUpdate(processUpdateRequest.getProcessItems().get(2), newCard3, oldCard3)
            assert postProcessCompleteEntryAdded(processUpdateRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), [:]]
    }

    def 'test process UpdateResponse invalid already-processed request property'() {
        setup:
        def input = Mock(UpdateResponse) {
            getRequest() >> Mock(UpdateRequest) {
                getProperties() >> createNotBooleanProcessingCompleteProperties()
            }

            getUpdatedMetacards() >> []
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        0 * processingFramework.submitUpdate(_ as ProcessRequest<ProcessUpdateItem>)
        !input.getRequest().getProperties().containsKey(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE)
    }

    def 'test process UpdateResponse without no updated metacards'() {
        given:
        def responseProperties = [a: _]

        def input = Mock(UpdateResponse) {
            getRequest() >> Mock(UpdateRequest) {
                getProperties() >> properties
            }

            getUpdatedMetacards() >> []

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        0 * catalogFramework._
        1 * processingFramework.submitUpdate(_ as ProcessRequest<ProcessUpdateItem>) >> { ProcessRequest<ProcessUpdateItem> processUpdateRequest ->
            assert processUpdateRequest.getProcessItems().isEmpty()
            assert postProcessCompleteEntryAdded(processUpdateRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), [:]]
    }

    def 'test UpdateResponse with null input and null metacards'() {
        when:
        def output = processingPostIngestPlugin.process(input as UpdateResponse)

        then:
        output == input

        0 * processingFramework._

        where:
        input << [null, Mock(UpdateResponse) {
            getUpdatedMetacards() >> null
        }, Mock(UpdateResponse) {
            getUpdatedMetacards() >> []
            getRequest() >> Mock(UpdateRequest) {
                getProperties() >> createProcessingCompleteProperties()
            }
        }]
    }

    /*
    test process(DeleteResponse)
     */

    def 'test process DeleteResponse'() {
        given:
        def metacard1 = Mock(Metacard)
        def metacard2 = Mock(Metacard)
        def metacard3 = Mock(Metacard)
        def responseProperties = [a: _]

        def input = Mock(DeleteResponse) {
            getRequest() >> Mock(DeleteRequest) {
                getProperties() >> properties
            }

            getDeletedMetacards() >> [metacard1, metacard2, metacard3]

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        1 * processingFramework.submitDelete(_ as ProcessRequest<ProcessDeleteItem>) >> { ProcessRequest<ProcessDeleteItem> processDeleteRequest ->
            assert processDeleteRequest.getProcessItems().size() == 3
            assert processDeleteRequest.getProcessItems().get(0).getMetacard() == metacard1
            assert processDeleteRequest.getProcessItems().get(1).getMetacard() == metacard2
            assert processDeleteRequest.getProcessItems().get(2).getMetacard() == metacard3
            assert postProcessCompleteEntryAdded(processDeleteRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), [:]]
    }

    def 'test process DeleteResponse invalid already-processed request property'() {
        setup:
        def input = Mock(DeleteResponse) {
            getRequest() >> Mock(DeleteRequest) {
                getProperties() >> createNotBooleanProcessingCompleteProperties()
            }

            getDeletedMetacards() >> []
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        0 * processingFramework.submitDelete(_ as ProcessRequest<ProcessDeleteItem>)
        !input.getRequest().getProperties().containsKey(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE)
    }

    def 'test process DeleteResponse with no deleted metacards'() {
        given:
        def responseProperties = [a: _]

        def input = Mock(DeleteResponse) {
            getRequest() >> Mock(DeleteRequest) {
                getProperties() >> properties
            }

            getDeletedMetacards() >> []

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        0 * catalogFramework._
        1 * processingFramework.submitDelete(_ as ProcessRequest<ProcessDeleteItem>) >> { ProcessRequest<ProcessDeleteItem> processDeleteRequest ->
            assert processDeleteRequest.getProcessItems().isEmpty()
            assert postProcessCompleteEntryAdded(processDeleteRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), [:]]
    }

    def 'test DeleteResponse with null input and null metacards'() {
        when:
        def output = processingPostIngestPlugin.process(input as DeleteResponse)

        then:
        output == input

        0 * processingFramework._

        where:
        input << [null, Mock(DeleteResponse) {
            getDeletedMetacards() >> null
        }, Mock(DeleteResponse) {
            getDeletedMetacards() >> []
            getRequest() >> Mock(DeleteRequest) {
                getProperties() >> createProcessingCompleteProperties()
            }
        }]
    }

    /*
    other tests
    */

    def 'test catalogFramework in resource supplier.get'() {
        given:
        String sourceId = "sourceId"
        Metacard metacard = mockMetacard("metacardId", sourceId)

        CatalogFramework cf = Mock(CatalogFramework)

        processingPostIngestPlugin = new ProcessingPostIngestPlugin(cf, processingFramework)

        Supplier<Resource> supplier = processingPostIngestPlugin.getResourceSupplier(metacard, mockSubject())

        when:
        supplier.get()

        then:
        1 * cf.getResource(_ as ResourceRequest, sourceId)
    }

    def 'test catalogFramework.getResource Exceptions'() {
        given:
        String sourceId = "sourceId"
        Metacard metacard = mockMetacard("metacardId", sourceId)
        CatalogFramework cf = Mock(CatalogFramework) {
            getResource(_ as ResourceRequest, sourceId) >> { throw exception }
        }

        processingPostIngestPlugin = new ProcessingPostIngestPlugin(cf, processingFramework)

        Supplier<Resource> supplier = processingPostIngestPlugin.getResourceSupplier(metacard, mockSubject())

        when:
        def output = supplier.get()

        then:
        assert output == null

        0 * catalogFramework.getResource(_ as ResourceRequest, sourceId)

        where:
        exception << [new IOException(), new ResourceNotFoundException(), new ResourceNotSupportedException(), new RuntimeException()]
    }

    /*
    helper methods
     */

    Map<String, Serializable> createProcessingCompleteProperties() {
        def properties = [:]
        properties.put(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE, true)
        return properties
    }

    Map<String, Serializable> createFalseProcessingCompleteProperties() {
        def properties = [:]
        properties.put(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE, false)
        return properties
    }

    Map<String, Serializable> createNotBooleanProcessingCompleteProperties() {
        def properties = [:]
        properties.put(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE, 3)
        return properties
    }

    Map<String, Serializable> propertiesWithSubject() {
        def properties = [:]
        properties.put(SecurityConstants.SECURITY_SUBJECT, mockSubject())
        return properties
    }

    def postProcessCompleteEntryAdded(newProperties, originalProperties) {
        Map<String, Serializable> expectedProperties = new HashMap<>(originalProperties)
        expectedProperties.put(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE, true)

        return expectedProperties == newProperties
    }

    def mockMetacard(String id, String source) {
        return Mock(Metacard) {
            getId() >> id
            getSourceId() >> source
        }
    }

    def mockAttribute(String attributeName, String attributeValue) {
        return Mock(Attribute) {
            getName() >> attributeName
            getValue() >> attributeValue
        }
    }

    def verifyUpdate(ProcessUpdateItem processUpdateItem, Metacard newMetacard, Metacard oldMetacard) {
        assert processUpdateItem.getMetacard() == newMetacard
        assert processUpdateItem.getOldMetacard() == oldMetacard
    }

    def mockSubject() {
        return Mock(Subject) {
            execute(_ as Callable) >> { Callable callable -> callable.call() }
        }
    }
}