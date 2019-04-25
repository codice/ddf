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

import java.util.concurrent.ExecutorService

class SourcePollerSpec extends Specification {

    def 'test getCachedValueForSource'() {
        given:
        final Source mockSource = Mock(Source) {
            getId() >> 'test id'
        }

        final SourcePoller sourcePoller = Spy(SourcePoller, constructorArgs: [Mock(ExecutorService), Mock(ExecutorService)]) {
            getCachedValue(new SourceKey(mockSource)) >> cachedValue
        }

        when:
        Optional cachedValueForSource = sourcePoller.getCachedValueForSource(mockSource)

        then:
        cachedValueForSource == cachedValue

        where:
        cachedValue << [Optional.empty(), Optional.of(_)]
    }
}