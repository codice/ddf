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

import com.google.common.collect.ImmutableMap
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class PollerRunnerSpec extends Specification {

    def 'test invalid constructor parameters'() {
        when:
        new PollerRunner(cache, initialPollIntervalMinutes as long, scheduledExecutorService) {

            @Override
            protected ImmutableMap getValueLoaders() {
                throw new SpockAssertionError('getValueLoaders should not be called')
            }
        }

        then:
        thrown expectedException

        where:
        cache        | initialPollIntervalMinutes | scheduledExecutorService       || expectedException
        null         | 1                          | Mock(ScheduledExecutorService) || NullPointerException
        Mock(Poller) | 0                          | Mock(ScheduledExecutorService) || IllegalArgumentException
        Mock(Poller) | -1                         | Mock(ScheduledExecutorService) || IllegalArgumentException
        Mock(Poller) | 1                          | Mock(ScheduledExecutorService) {
            isShutdown() >> true
        }                                                                          || IllegalArgumentException
    }

    def 'test scheduledExecutorService is shutdown on destroy'() {
        given:
        final ScheduledExecutorService mockScheduledExecutorService = Mock(ScheduledExecutorService)

        final PollerRunner pollerRunner = Spy(PollerRunner, constructorArgs: [Mock(Poller), 1, mockScheduledExecutorService])
        pollerRunner.init()

        when:
        pollerRunner.destroy()

        then:
        (1.._) * mockScheduledExecutorService.shutdown()
    }

    def 'test scheduledExecutorService is shutdown when setPollIntervalMinutes'() {
        given:
        final PollerRunner pollerRunner = Spy(PollerRunner, constructorArgs: [Mock(Poller), 1, Mock(ScheduledExecutorService) {
            isShutdown() >>> [false, true]
        }])
        pollerRunner.init()

        when:
        pollerRunner.setPollIntervalMinutes(1)

        then:
        thrown IllegalStateException

        cleanup:
        pollerRunner.destroy()
    }

    def 'test invalid pollIntervalMinutes'() {
        given:
        final PollerRunner pollerRunner = Spy(PollerRunner, constructorArgs: [Mock(Poller), 1, Mock(ScheduledExecutorService)])
        pollerRunner.init()

        when:
        pollerRunner.setPollIntervalMinutes(pollIntervalMinutes)

        then:
        thrown IllegalArgumentException

        cleanup:
        pollerRunner.destroy()

        where:
        pollIntervalMinutes << [0, -1]
    }

    def 'test RejectedExecutionException when scheduling'() {
        given:
        final long pollIntervalMinutes = 1

        final PollerRunner pollerRunner = new PollerRunner(Mock(Poller), pollIntervalMinutes, Mock(ScheduledExecutorService) {
            scheduleAtFixedRate(_ as Runnable, 0, pollIntervalMinutes, TimeUnit.MINUTES) >> {
                throw new RejectedExecutionException()
            }
        }) {

            @Override
            protected ImmutableMap getValueLoaders() {
                throw new SpockAssertionError('getValueLoaders should not be called')
            }
        }

        when:
        pollerRunner.init()

        then:
        thrown IllegalStateException
    }
}
