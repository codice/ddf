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

import ddf.catalog.data.Metacard
import ddf.catalog.operation.CreateResponse
import ddf.catalog.operation.DeleteResponse
import ddf.catalog.operation.Update
import ddf.catalog.operation.UpdateResponse
import ddf.catalog.plugin.PluginExecutionException
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.Specification

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.GAZETTEER_METACARD_TAG

@RunWith(JUnitPlatform.class)
class BuildSuggesterIndexPluginSpec extends Specification {
    def future = Mock(ScheduledFuture)
    def buildSuggesterIndex = Stub(BuildSuggesterIndex)
    def executor = Mock(ScheduledThreadPoolExecutor)
    def buildSuggesterIndexPlugin = new BuildSuggesterIndexPlugin(executor, buildSuggesterIndex)

    def gazetteerMetacard = Stub(Metacard) {
        getTags() >> (Set) [GAZETTEER_METACARD_TAG]
    }

    def "schedules a suggester index build when gazetteer metacards are created"() {
        setup:
        def createResponse = Stub(CreateResponse) {
            getCreatedMetacards() >> [gazetteerMetacard]
        }

        when:
        buildSuggesterIndexPlugin.process(createResponse)

        then:
        1 * executor.schedule(buildSuggesterIndex, 1, TimeUnit.MINUTES) >> future
    }

    def "schedules a suggester index build when gazetteer metacards are updated"() {
        setup:
        def update = Stub(Update) {
            getOldMetacard() >> gazetteerMetacard
        }
        def updateResponse = Stub(UpdateResponse) {
            getUpdatedMetacards() >> [update]
        }

        when:
        buildSuggesterIndexPlugin.process(updateResponse)

        then:
        1 * executor.schedule(buildSuggesterIndex, 1, TimeUnit.MINUTES) >> future
    }

    def "schedules a suggester index build when metacards are updated to gazetteer metacards"() {
        setup:
        def update = Stub(Update) {
            getOldMetacard() >> Stub(Metacard)
            getNewMetacard() >> gazetteerMetacard
        }
        def updateResponse = Stub(UpdateResponse) {
            getUpdatedMetacards() >> [update]
        }

        when:
        buildSuggesterIndexPlugin.process(updateResponse)

        then:
        1 * executor.schedule(buildSuggesterIndex, 1, TimeUnit.MINUTES) >> future
    }

    def "schedules a suggester index build when gazetteer metacards are deleted"() {
        setup:
        def deleteResponse = Stub(DeleteResponse) {
            getDeletedMetacards() >> [gazetteerMetacard]
        }

        when:
        buildSuggesterIndexPlugin.process(deleteResponse)

        then:
        1 * executor.schedule(buildSuggesterIndex, 1, TimeUnit.MINUTES) >> future
    }

    def "does nothing when no gazetteer metacards are created"() {
        setup:
        def createResponse = Stub(CreateResponse)

        when:
        buildSuggesterIndexPlugin.process(createResponse)

        then:
        0 * _
    }

    def "does nothing when no gazetteer metacards are updated"() {
        setup:
        def updateResponse = Stub(UpdateResponse)

        when:
        buildSuggesterIndexPlugin.process(updateResponse)

        then:
        0 * _
    }

    def "does nothing when no gazetteer metacards are deleted"() {
        setup:
        def deleteResponse = Stub(DeleteResponse)

        when:
        buildSuggesterIndexPlugin.process(deleteResponse)

        then:
        0 * _
    }

    def "cancels last scheduled task before scheduling another"() {
        setup:
        def createResponse = Stub(CreateResponse) {
            getCreatedMetacards() >> [gazetteerMetacard]
        }

        when:
        buildSuggesterIndexPlugin.process(createResponse)

        then:
        1 * executor.schedule(buildSuggesterIndex, 1, TimeUnit.MINUTES) >> future

        when:
        buildSuggesterIndexPlugin.process(createResponse)

        then:
        1 * future.cancel(false)
        1 * executor.schedule(buildSuggesterIndex, 1, TimeUnit.MINUTES) >> future
    }

    def "throws a PluginExecutionException when the task cannot be scheduled"() {
        setup:
        def createResponse = Stub(CreateResponse) {
            getCreatedMetacards() >> [gazetteerMetacard]
        }

        when:
        buildSuggesterIndexPlugin.process(createResponse)

        then:
        1 * executor.schedule(buildSuggesterIndex, 1, TimeUnit.MINUTES) >> {
            throw new RejectedExecutionException()
        }
        PluginExecutionException e = thrown()
        e.cause instanceof RejectedExecutionException
    }
}
