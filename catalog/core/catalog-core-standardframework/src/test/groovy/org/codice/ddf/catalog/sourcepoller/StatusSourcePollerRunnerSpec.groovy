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
package org.codice.ddf.catalog.sourcepoller

import ddf.catalog.source.Source
import spock.lang.Specification

import java.util.concurrent.ScheduledExecutorService

class StatusSourcePollerRunnerSpec extends Specification {

    private final StatusSourcePollerRunner statusSourcePollerRunner = new StatusSourcePollerRunner(Mock(Poller), 1, Mock(ScheduledExecutorService), Mock(SourceRegistry))

    def setup() {
        statusSourcePollerRunner.init()
    }

    def cleanup() {
        statusSourcePollerRunner.destroy()
    }

    def 'test getCurrentValueForSource'() {
        when:
        final SourceStatus currentValueForSource = statusSourcePollerRunner.getCurrentValueForSource(Mock(Source) {
            isAvailable() >> availability
        })

        then:
        currentValueForSource == expectedStatus

        where:
        availability || expectedStatus
        true         || SourceStatus.AVAILABLE
        false        || SourceStatus.UNAVAILABLE
    }

    def 'test null source'() {
        when:
        statusSourcePollerRunner.getCurrentValueForSource(null)

        then:
        thrown NullPointerException
    }

    def 'test RuntimeException when getStatus'() {
        given:
        final RuntimeException runtimeException = new RuntimeException()

        when:
        statusSourcePollerRunner.getCurrentValueForSource(Mock(Source) {
            isAvailable() >> { throw runtimeException }
        })

        then:
        RuntimeException thrown = thrown()
        thrown == runtimeException
    }
}