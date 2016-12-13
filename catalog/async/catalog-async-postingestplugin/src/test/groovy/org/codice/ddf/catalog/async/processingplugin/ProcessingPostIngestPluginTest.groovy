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
package org.codice.ddf.catalog.plugin.async

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
        thrown(NullPointerException)

        where:
        catalogFramework       | processingFramework
        null                   | Mock(ProcessingFramework)
        Mock(CatalogFramework) | null
        null                   | null
    }

    /*
    test process(CreateResponse)
     */

    def 'test process CreateResponse'() {
        given:
        final String id = _
        final String sourceId = _
        def metacard = Mock(Metacard) {
            getId() >> id
            getSourceId() >> sourceId
        }

        def responseProperties = [a: _]

        def input = Mock(CreateResponse) {
            getRequest() >> Mock(CreateRequest) {
                getProperties() >> properties
            }

            getCreatedMetacards() >> [metacard]

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        1 * catalogFramework.getResource(_ as ResourceRequest, sourceId) >> Mock(ResourceResponse) {
            getResource() >> Mock(Resource)
        }
        1 * processingFramework.submitCreate(_ as ProcessRequest<ProcessCreateItem>) >> { ProcessRequest<ProcessCreateItem> processCreateRequest ->
            assert processCreateRequest.getProcessItems().size() == 1
            final processCreateItem = processCreateRequest.getProcessItems().get(0)
            assert processCreateItem.getMetacard() == metacard
            assert postProcessCompleteEntryAdded(processCreateRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), createNotBooleanProcessingCompleteProperties(), [:]]
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
        properties << [createFalseProcessingCompleteProperties(), createNotBooleanProcessingCompleteProperties(), [:]]
    }

    def 'test no process CreateResponse'() {
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
        def oldCard = Mock(Metacard)
        final String newId = _
        final String newSourceId = _
        def newCard = Mock(Metacard) {
            getId() >> newId
            getSourceId() >> newSourceId
        }

        def responseProperties = [a: _]

        def input = Mock(UpdateResponse) {
            getRequest() >> Mock(UpdateRequest) {
                getProperties() >> properties
            }

            getUpdatedMetacards() >> {
                [Mock(Update) {
                    getOldMetacard() >> oldCard
                    getNewMetacard() >> newCard
                }]
            }

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        1 * catalogFramework.getResource(_ as ResourceRequest, newSourceId) >> Mock(ResourceResponse) {
            getResource() >> Mock(Resource)
        }
        1 * processingFramework.submitUpdate(_ as ProcessRequest<ProcessUpdateItem>) >> { ProcessRequest<ProcessUpdateItem> processUpdateRequest ->
            assert processUpdateRequest.getProcessItems().size() == 1
            final processUpdateItem = processUpdateRequest.getProcessItems().get(0)
            assert processUpdateItem.getOldMetacard() == oldCard
            assert processUpdateItem.getMetacard() == newCard
            assert postProcessCompleteEntryAdded(processUpdateRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), createNotBooleanProcessingCompleteProperties(), [:]]
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
        properties << [createFalseProcessingCompleteProperties(), createNotBooleanProcessingCompleteProperties(), [:]]
    }

    def 'test no process UpdateResponse'() {
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
        def metacard = Mock(Metacard)
        def responseProperties = [a: _]

        def input = Mock(DeleteResponse) {
            getRequest() >> Mock(DeleteRequest) {
                getProperties() >> properties
            }

            getDeletedMetacards() >> [metacard]

            getProperties() >> responseProperties
        }

        when:
        def output = processingPostIngestPlugin.process(input)

        then:
        output == input

        1 * processingFramework.submitDelete(_ as ProcessRequest<ProcessDeleteItem>) >> { ProcessRequest<ProcessDeleteItem> processDeleteRequest ->
            assert processDeleteRequest.getProcessItems().size() == 1
            assert processDeleteRequest.getProcessItems().get(0).getMetacard() == metacard
            assert postProcessCompleteEntryAdded(processDeleteRequest.getProperties(), responseProperties)
        }

        where:
        properties << [createFalseProcessingCompleteProperties(), createNotBooleanProcessingCompleteProperties(), [:]]
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
        properties << [createFalseProcessingCompleteProperties(), createNotBooleanProcessingCompleteProperties(), [:]]
    }

    def 'test no process DeleteResponse'() {
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
        exception << [new IOException(), new ResourceNotFoundException(), new ResourceNotSupportedException()]
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
}