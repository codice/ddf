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
import ddf.catalog.data.Metacard
import ddf.catalog.operation.*
import ddf.catalog.resource.Resource
import ddf.catalog.resource.ResourceNotFoundException
import ddf.catalog.resource.ResourceNotSupportedException
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem
import org.codice.ddf.catalog.async.processingframework.api.internal.ProcessingFramework
import org.codice.ddf.catalog.async.processingplugin.ProcessingPostIngestPlugin
import spock.lang.Specification

class ProcessingPostIngestPluginTest extends Specification {

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
        null                   | null
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

        def responseProperties = [a: _]

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

        3 * catalogFramework.getResource(_ as ResourceRequest, sourceId) >> Mock(ResourceResponse) {
            getResource() >> Mock(Resource)
        }
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

        def responseProperties = [a: _]

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

        3 * catalogFramework.getResource(_ as ResourceRequest, _ as String) >> Mock(ResourceResponse) {
            getResource() >> Mock(Resource) {
                getSize() >> 1
                getInputStream() >> Mock(InputStream)
            }
        }
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

    def 'test catalogFramework.getResource Exceptions'() {
        given:
        final String sourceId
        def metacard = Mock(Metacard) {
            getSourceId() >> sourceId
        }

        def responseProperties = [a: _]

        def input = Mock(CreateResponse) {
            getRequest() >> Mock(CreateRequest) {
                getProperties() >> [:]
            }

            getCreatedMetacards() >> [metacard]

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        1 * catalogFramework.getResource(_ as ResourceRequest, sourceId) >> {
            throw exception
        }

        1 * processingFramework.submitCreate(_ as ProcessRequest<ProcessCreateItem>) >> { ProcessRequest<ProcessCreateItem> processCreateRequest ->
            assert processCreateRequest.getProcessItems().size() == 1
            final processCreateItem = processCreateRequest.getProcessItems().get(0)
            assert processCreateItem.getProcessResource() == null
            assert processCreateItem.getMetacard() == metacard
            assert postProcessCompleteEntryAdded(processCreateRequest.getProperties(), responseProperties)
        }

        where:
        exception << [new IOException(), new ResourceNotFoundException(), new ResourceNotSupportedException(), new RuntimeException()]
    }

    /*
    helper methods
     */

    static Map<String, Serializable> createProcessingCompleteProperties() {
        def properties = [:]
        properties.put(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE, true)
        return properties
    }

    static Map<String, Serializable> createFalseProcessingCompleteProperties() {
        def properties = [:]
        properties.put(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE, false)
        return properties
    }

    static Map<String, Serializable> createNotBooleanProcessingCompleteProperties() {
        def properties = [:]
        properties.put(ProcessingPostIngestPlugin.POST_PROCESS_COMPLETE, 3)
        return properties
    }

    static postProcessCompleteEntryAdded(newProperties, originalProperties) {
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

    def verifyUpdate(ProcessUpdateItem processUpdateItem, Metacard newMetacard, Metacard oldMetacard) {
        assert processUpdateItem.getMetacard() == newMetacard
        assert processUpdateItem.getOldMetacard() == oldMetacard
    }
}