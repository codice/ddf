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

import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@Timeout(value = 2, unit = TimeUnit.SECONDS)
class SourceStatusSourcePollerSpec extends Specification {

    def 'test handleTimeout'() {
        given:
        final StatusSourcePoller statusSourcePoller = Spy(StatusSourcePoller, constructorArgs: [Mock(ExecutorService), Mock(ExecutorService)])

        final SourceKey mockSourceKey = Mock()

        when:
        statusSourcePoller.handleTimeout(mockSourceKey)

        then:
        1 * statusSourcePoller.cacheNewValue(mockSourceKey, SourceStatus.TIMEOUT)

        cleanup:
        statusSourcePoller.destroy()
    }

    def 'test handleException'() {
        given:
        final StatusSourcePoller statusSourcePoller = Spy(StatusSourcePoller, constructorArgs: [Mock(ExecutorService), Mock(ExecutorService)])

        final SourceKey mockSourceKey = Mock()

        when:
        statusSourcePoller.handleException(mockSourceKey, Mock(RuntimeException))

        then:
        1 * statusSourcePoller.cacheNewValue(mockSourceKey, SourceStatus.EXCEPTION)

        cleanup:
        statusSourcePoller.destroy()
    }
}