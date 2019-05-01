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
import spock.lang.Ignore
import spock.lang.Specification

import static org.hamcrest.Matchers.containsString

class SourceKeySpec extends Specification {

    def 'test invalid constructor parameters'() {
        when:
        new SourceKey(Mock(Source) {
            getId() >> id
        })

        then:
        thrown expectedException

        where:
        id   || expectedException
        null || NullPointerException
        ''   || IllegalArgumentException
    }

    def 'test toString'() {
        given:
        final String id = 'test id'
        final SourceKey sourceKey = new SourceKey(Mock(Source) {
            getVersion() >> 'test version'
            getId() >> id
            getTitle() >> 'test title'
            getDescription() >> 'test description'
            getOrganization() >> 'test organization'
        })

        expect:
        ((String) sourceKey.toString()) containsString(id)
    }

    def 'test equals same object'() {
        given:
        final SourceKey sourceKey = new SourceKey(Mock(Source) {
            getId() >> 'test id'
        })

        expect:
        verifyAll {
            sourceKey.equals(sourceKey)
            sourceKey.hashCode() == sourceKey.hashCode()
        }
    }

    def 'test equals not SourceKey'() {
        given:
        final SourceKey sourceKey = new SourceKey(Mock(Source) {
            getId() >> 'test id'
        })

        expect:
        !sourceKey.equals(!SourceKey)
    }

    @Ignore("TODO DDF-4288")
    def 'test two Sources with the same Describable values'() {
        given:
        final String id = _
        final Source firstMockSource = Mock() {
            getId() >> id
        }
        final Source secondMockSource = Mock() {
            getId() >> id
        }

        expect:
        new SourceKey(firstMockSource) != new SourceKey(secondMockSource)
    }

    @Ignore("TODO DDF-4288")
    def 'test Describable value of a Source changes'() {
        given:
        final Source source = Mock() {
            2 * getId() >>> ['id1', 'id2']
        }

        expect:
        new SourceKey(source) == new SourceKey(source)
    }

    @Ignore("TODO DDF-4288")
    def 'test Source non-Describable value is modified'() {
        given:
        final Source source /*= TODO DDF-4288*/

        expect:
        new SourceKey(source) == new SourceKey(source)
    }
}
