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
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification

import java.util.concurrent.ScheduledExecutorService

class SourcePollerRunnerSpec extends Specification {

    def 'test null SourceRegistry'() {
        when:
        new SourcePollerRunner(Mock(Poller), 1, Mock(ScheduledExecutorService), null) {

            @Override
            protected Object getCurrentValueForSource(Source source) {
                throw new SpockAssertionError('getCurrentValueForSource should not be called')
            }
        }

        then:
        thrown NullPointerException
    }

    def 'test two sources with the same Key'() {
        given:
        final SourcePollerRunner sourcePollerRunner = Spy(SourcePollerRunner, constructorArgs: [Mock(Poller), 1, Mock(ScheduledExecutorService), Mock(SourceRegistry) {
            final String id = _

            getCurrentSources() >> [Mock(Source) {
                getId() >> id
            }, Mock(Source) {
                getId() >> id
            }]
        }])

        expect:
        // TODO DDF-4288
        sourcePollerRunner.getValueLoaders().size() == 1
    }

    def 'test null Source in SourceRegistry'() {
        given:
        final SourcePollerRunner sourcePollerRunner = Spy(SourcePollerRunner, constructorArgs: [Mock(Poller), 1, Mock(ScheduledExecutorService), Mock(SourceRegistry) {
            getCurrentSources() >> [Mock(Source) {
                getId() >> "id1"
            }, null, Mock(Source) {
                getId() >> "id2"
            }]
        }])

        when:
        sourcePollerRunner.getValueLoaders()

        then:
        thrown NullPointerException
    }
}
