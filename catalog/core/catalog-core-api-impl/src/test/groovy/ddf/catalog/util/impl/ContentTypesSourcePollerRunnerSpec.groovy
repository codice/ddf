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
package ddf.catalog.util.impl

import ddf.catalog.data.ContentType
import ddf.catalog.source.Source
import org.hamcrest.Matchers
import spock.lang.Specification

import java.util.concurrent.ScheduledExecutorService

import static org.hamcrest.Matchers.empty

class ContentTypesSourcePollerRunnerSpec extends Specification {

    private final ContentTypesSourcePollerRunner contentTypesSourcePollerRunner = new ContentTypesSourcePollerRunner(Mock(Poller), 1, Mock(ScheduledExecutorService), Mock(SourceRegistry))

    def setup() {
        contentTypesSourcePollerRunner.init()
    }

    def cleanup() {
        contentTypesSourcePollerRunner.destroy()
    }

    def 'test getCurrentValueForSource'(final Set<ContentType> contentTypes) {
        when:
        final Set<ContentType> currentValueForSource = contentTypesSourcePollerRunner.getCurrentValueForSource(Mock(Source) {
            getContentTypes() >> contentTypes
        })

        then:
        currentValueForSource == contentTypes

        where:
        contentTypes << [[], _]
    }

    def 'test null source'() {
        when:
        contentTypesSourcePollerRunner.getCurrentValueForSource(null)

        then:
        thrown NullPointerException
    }

    def 'test RuntimeException when getContentTypes'() {
        given:
        final RuntimeException runtimeException = new RuntimeException()

        when:
        contentTypesSourcePollerRunner.getCurrentValueForSource(Mock(Source) {
            getContentTypes() >> { throw runtimeException }
        })

        then:
        RuntimeException thrown = thrown()
        thrown == runtimeException
    }

    def 'test null getContentTypes'() {
        when:
        final Set<ContentType> currentValueForSource = contentTypesSourcePollerRunner.getCurrentValueForSource(Mock(Source) {
            getContentTypes() >> null
        })

        then:
        currentValueForSource Matchers.is(empty())
    }
}