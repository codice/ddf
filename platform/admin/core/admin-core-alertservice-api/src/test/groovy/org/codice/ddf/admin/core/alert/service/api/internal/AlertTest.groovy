/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.core.alert.service.api.internal

import spock.lang.Specification
import spock.lang.Unroll

class AlertTest extends Specification {

    @Unroll
    def 'test construct Alert with with #level level'(level) {
        given:
            final String title = _ as String
            final List<AlertDetail> details = []
            final String key = _ as String

        when:
            def alert = new Alert(level, title, details, key)

        then:
            alert.getLevel() == level
            alert.getTitle() == title
            alert.getDetails() == details
            alert.getKey().isPresent()
            alert.getKey().get() == key

        where:
            level << Alert.Level.values()
    }

    @Unroll
    def 'test construct Alert with #details'(details) {
        given:
            final title = _ as String
            final level = Alert.Level.ERROR
            final key = _ as String

        when:
            def alert = new Alert(level, title, details, key)

        then:
            alert.getLevel() == level
            alert.getTitle() == title
            alert.getDetails() == details
            alert.getKey().isPresent()
            alert.getKey().get() == key

        where:
            details << [[], [Mock(AlertDetail)], [Mock(AlertDetail), Mock(AlertDetail)]]
    }

    def 'test construct Alert with null key'() {
        given:
            final title = _ as String
            final level = Alert.Level.ERROR
            final List<AlertDetail> details = []

        when:
            def alert = new Alert(level, title, details, null)

        then:
            alert.getLevel() == level
            alert.getTitle() == title
            alert.getDetails() == details
            !alert.getKey().isPresent()
    }

    def 'test construct Alert with an invalid parameter'() {
        when:
            new Alert(level, title, details, key)

        then:
            thrown(IllegalArgumentException)

        where:
            level             | title       | details | key
            null              | _ as String | _       | _ as String
            Alert.Level.ERROR | null        | _       | _ as String
            Alert.Level.ERROR | ""          | _       | _ as String
            Alert.Level.ERROR | _ as String | null    | _ as String
            Alert.Level.ERROR | _ as String | _       | ""
    }
}