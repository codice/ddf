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
package org.codice.ddf.catalog.async.processingframework.impl

import ddf.catalog.CatalogFramework
import ddf.catalog.content.operation.UpdateStorageRequest
import ddf.catalog.data.Metacard
import ddf.catalog.operation.UpdateRequest
import ddf.catalog.plugin.PluginExecutionException
import ddf.catalog.source.IngestException
import ddf.catalog.source.SourceUnavailableException
import ddf.security.Subject
import org.codice.ddf.catalog.async.data.api.internal.ProcessDeleteItem
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource
import org.codice.ddf.catalog.async.data.api.internal.ProcessResourceItem
import org.codice.ddf.catalog.async.plugin.api.internal.PostProcessPlugin
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class InMemoryProcessingFrameworkSpec extends Specification {

    InMemoryProcessingFramework inMemoryProcessingFramework

    CatalogFramework catalogFramework = Mock(CatalogFramework)

    ExecutorService threadPool = Mock(ExecutorService)

    def setup() {
        inMemoryProcessingFramework = new InMemoryProcessingFramework(catalogFramework, threadPool)
    }

    def 'test construct will null arguments'(CatalogFramework catalogFramework, ExecutorService threadPool) {
        when:
        new InMemoryProcessingFramework(catalogFramework, threadPool)

        then:
        thrown(NullPointerException)

        where:
        catalogFramework       | threadPool
        null                   | Mock(ExecutorService)
        Mock(CatalogFramework) | null
        null                   | null
    }

    /*
    test submitCreate
     */

    def 'test submitCreate with no postProcessPlugins'(List<PostProcessPlugin> postProcessPlugins) {
        given:
        inMemoryProcessingFramework.setPostProcessPlugins(postProcessPlugins)

        when:
        inMemoryProcessingFramework.submitCreate(_ as ProcessRequest)

        then:
        0 * threadPool._
        0 * catalogFramework._

        where:
        postProcessPlugins << [null, []]
    }

    def 'test submitCreate when the plugin does not set modified flags'() {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processCreateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitCreate(processCreateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processCreate(processCreateRequest) >> processCreateRequest
        0 * catalogFramework._
    }

    def 'test submitCreate with one ProcessItem with Metacard, and one ProcessItem with Metacard+ProcessResource'() {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processCreateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitCreate(processCreateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processCreate(processCreateRequest) >> markAsModifiedAndGet(processCreateRequest)
        1 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest)
    }

    def 'test submitCreate when a plugin throws error'() {
        given:
        def postProcessPlugin1 = Mock(PostProcessPlugin)
        def postProcessPlugin2 = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin1, postProcessPlugin2])

        def processCreateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitCreate(processCreateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin1.processCreate(processCreateRequest) >> markAsModifiedAndGet(processCreateRequest)
        1 * postProcessPlugin2.processCreate(processCreateRequest) >> {
            throw new PluginExecutionException()
        }
        1 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest)
    }

    def 'test submitCreate when the ProcessResource throws error'() {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processCreateRequest = createMockProcessResourceRequest({ throw new IOException() })

        when:
        inMemoryProcessingFramework.submitCreate(processCreateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processCreate(processCreateRequest) >> markAsModifiedAndGet(processCreateRequest)
        0 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest)
    }

    def 'test submitCreate catalogFramework.update(updateStorageRequest) exceptions'(Exception exception) {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processCreateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitCreate(processCreateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processCreate(processCreateRequest) >> markAsModifiedAndGet(processCreateRequest)
        1 * catalogFramework.update(_ as UpdateStorageRequest) >> { throw exception }
        1 * catalogFramework.update(_ as UpdateRequest)

        where:
        exception << [new IngestException(), new SourceUnavailableException(), new RuntimeException()]
    }

    def 'test submitCreate catalogFramework.update(updateMetacardsRequest) exceptions'(Exception exception) {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processCreateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitCreate(processCreateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processCreate(processCreateRequest) >> markAsModifiedAndGet(processCreateRequest)
        1 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest) >> { throw exception }

        where:
        exception << [new IngestException(), new SourceUnavailableException(), new RuntimeException()]
    }

    /*
    test submitUpdate
    */

    def 'test submitUpdate with no postProcessPlugins'(List<PostProcessPlugin> postProcessPlugins) {
        given:
        inMemoryProcessingFramework.setPostProcessPlugins(postProcessPlugins)

        when:
        inMemoryProcessingFramework.submitUpdate(_ as ProcessRequest)

        then:
        0 * threadPool._
        0 * catalogFramework._

        where:
        postProcessPlugins << [null, []]
    }

    def 'test submitUpdate when the plugin does not set modified flags'() {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processUpdateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitUpdate(processUpdateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processUpdate(processUpdateRequest) >> processUpdateRequest
        0 * catalogFramework._
    }

    def 'test submitUpdate'() {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processUpdateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitUpdate(processUpdateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processUpdate(processUpdateRequest) >> markAsModifiedAndGet(processUpdateRequest)
        1 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest)
    }

    def 'test submitUpdate when a plugin throws error'() {
        given:
        def postProcessPlugin1 = Mock(PostProcessPlugin)
        def postProcessPlugin2 = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin1, postProcessPlugin2])

        def processUpdateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitUpdate(processUpdateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin1.processUpdate(processUpdateRequest) >> markAsModifiedAndGet(processUpdateRequest)
        1 * postProcessPlugin2.processUpdate(processUpdateRequest) >> {
            throw new PluginExecutionException()
        }
        1 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest)
    }

    def 'test submitUpdate when the ProcessResource throws error'() {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processUpdateRequest = createMockProcessResourceRequest({ throw new IOException() })

        when:
        inMemoryProcessingFramework.submitUpdate(processUpdateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processUpdate(processUpdateRequest) >> markAsModifiedAndGet(processUpdateRequest)
        0 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest)
    }

    def 'test submitUpdate catalogFramework.update(updateStorageRequest) exceptions'(Exception exception) {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processUpdateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitUpdate(processUpdateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processUpdate(processUpdateRequest) >> markAsModifiedAndGet(processUpdateRequest)
        1 * catalogFramework.update(_ as UpdateStorageRequest) >> { throw exception }
        1 * catalogFramework.update(_ as UpdateRequest)

        where:
        exception << [new IngestException(), new SourceUnavailableException()]
    }

    def 'test submitUpdate catalogFramework.update(updateMetacardsRequest) exceptions'(Exception exception) {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processUpdateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitUpdate(processUpdateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processUpdate(processUpdateRequest) >> markAsModifiedAndGet(processUpdateRequest)
        1 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest) >> { throw exception }

        where:
        exception << [new IngestException(), new SourceUnavailableException()]
    }

    /*
    test submitDelete
     */

    def 'test submitDelete with no postProcessPlugins'(List<PostProcessPlugin> postProcessPlugins) {
        given:
        inMemoryProcessingFramework.setPostProcessPlugins(postProcessPlugins)

        when:
        inMemoryProcessingFramework.submitDelete(_ as ProcessRequest)

        then:
        0 * threadPool._
        0 * catalogFramework._

        where:
        postProcessPlugins << [null, []]
    }

    def 'test submitDelete'() {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processDeleteRequest = createMockDeleteRequest()

        when:
        inMemoryProcessingFramework.submitDelete(processDeleteRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processDelete(processDeleteRequest) >> processDeleteRequest
    }

    def 'test submitDelete when plugin throws error'() {
        given:
        def postProcessPlugin1 = Mock(PostProcessPlugin)
        def postProcessPlugin2 = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin1, postProcessPlugin2])

        def processDeleteRequest = createMockDeleteRequest()

        when:
        inMemoryProcessingFramework.submitDelete(processDeleteRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin1.processDelete(processDeleteRequest) >> processDeleteRequest
        1 * postProcessPlugin2.processDelete(processDeleteRequest) >> {
            throw new PluginExecutionException()
        }
    }

    def 'test cleanup already shutdown'() {
        when:
        inMemoryProcessingFramework.cleanUp()

        then:
        1 * threadPool.isShutdown() >> {
            return true
        }
        0 * threadPool.shutdown()
        0 * threadPool.awaitTermination(_ as int, _ as TimeUnit)
        0 * threadPool.shutdownNow()
    }

    def 'test cleanup success shutdown'() {
        when:
        inMemoryProcessingFramework.cleanUp()

        then:
        1 * threadPool.isShutdown() >> {
            return false
        }

        1 * threadPool.shutdown()
        1 * threadPool.awaitTermination(600, TimeUnit.SECONDS) >> {
            return true
        }
        0 * threadPool.shutdownNow()
    }

    def 'test cleanup success shutdown with unfinished processes'() {
        when:
        inMemoryProcessingFramework.cleanUp()

        then:
        1 * threadPool.isShutdown() >> {
            return false
        }

        1 * threadPool.shutdown()
        1 * threadPool.awaitTermination(600, TimeUnit.SECONDS) >> {
            return false
        }
        1 * threadPool.shutdownNow()
        1 * threadPool.awaitTermination(60, TimeUnit.SECONDS)
    }

    def 'test submitCreate with modified metacard but not a modified resource'() {
        given:
        def postProcessPlugin = Mock(PostProcessPlugin)
        inMemoryProcessingFramework.setPostProcessPlugins([postProcessPlugin])

        def processCreateRequest = createMockProcessResourceRequest()

        when:
        inMemoryProcessingFramework.submitCreate(processCreateRequest)

        then:
        1 * threadPool.submit(_ as Runnable) >> { Runnable runnable -> runnable.run() }
        1 * postProcessPlugin.processCreate(processCreateRequest) >> markMetacardAsModifiedAndGet(processCreateRequest)
        0 * catalogFramework.update(_ as UpdateStorageRequest)
        1 * catalogFramework.update(_ as UpdateRequest)

    }

    private <T extends ProcessResourceItem> ProcessRequest<T> createMockProcessResourceRequest() {
        return createMockProcessResourceRequest(new ByteArrayInputStream((_ as String).getBytes()))
    }

    private <T extends ProcessResourceItem> ProcessRequest<T> createMockProcessResourceRequest(inputStream) {
        return Mock(ProcessRequest) {
            def subject = Mock(Subject) {
                execute(_ as Callable) >> { Callable callable -> callable.call() }
            }

            getProperties() >> ['ddf.security.subject': subject]

            getProcessItems() >> [Mock(T) {
                isMetacardModified() >> false

                getMetacard() >> Mock(Metacard) {
                    getId() >> "test resource id 1"
                }

                getProcessResource() >> Mock(ProcessResource) {
                    isModified() >> false
                    getInputStream() >> inputStream
                }
            }, Mock(T) {
                isMetacardModified() >> false

                getMetacard() >> Mock(Metacard) {
                    getId() >> "test resource id 2"
                }
            }]
        }
    }

    private <T extends ProcessResourceItem> ProcessRequest<T> markAsModifiedAndGet(ProcessRequest<T> processResourceRequest) {
        for (T processResourceItem : processResourceRequest.getProcessItems()) {
            processResourceItem.isMetacardModified() >> true
            processResourceItem.getProcessResource().isModified() >> true
        }

        return processResourceRequest
    }

    private <T extends ProcessResourceItem> ProcessRequest<T> markMetacardAsModifiedAndGet(ProcessRequest<T> processResourceRequest) {
        for (T processResourceItem : processResourceRequest.getProcessItems()) {
            processResourceItem.isMetacardModified() >> true
        }

        return processResourceRequest
    }

    private ProcessRequest<ProcessDeleteItem> createMockDeleteRequest() {
        return Mock(ProcessRequest) {
            getProperties() >> [test: _]
        }
    }
}