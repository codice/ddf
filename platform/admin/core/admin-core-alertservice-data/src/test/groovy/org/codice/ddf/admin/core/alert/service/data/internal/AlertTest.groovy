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
package org.codice.ddf.admin.core.alert.service.data.internal

import spock.lang.Specification
import spock.lang.Unroll

class AlertTest extends Specification {

    @Unroll
    def 'test construct Alert with key and #level level'(level) {
        given:
            final key = _ as String
            final title = _ as String
            final details = []

        when:
            final alert = new Alert(key, level, title, details)

        then:
            notThrown IllegalArgumentException
            alert.getKey() == key
            alert.getLevel() == level
            alert.getTitle() == title
            alert.getDetails() == details

        where:
            level << Alert.Level.values()
    }

    @Unroll
    def 'test construct Alert with key and #details details'(details) {
        given:
            final key = _ as String
            final level = Alert.Level.ERROR
            final title = _ as String

        when:
            final alert = new Alert(key, level, title, details)

        then:
            notThrown IllegalArgumentException
            alert.getKey() == key
            alert.getLevel() == level
            alert.getTitle() == title
            alert.getDetails() == details

        where:
            details << [[], [_ as String], [_ as String, _ as String]]
    }

    def 'test construct Alert with an invalid parameter'() {
        when:
            new Alert(key, level, title, details)

        then:
            thrown IllegalArgumentException

        where:
            level             | title       | details | key
            null              | _ as String | _       | _ as String
            Alert.Level.ERROR | null        | _       | _ as String
            Alert.Level.ERROR | ""          | _       | _ as String
            Alert.Level.ERROR | _ as String | null    | _ as String
            Alert.Level.ERROR | _ as String | _       | ""
            Alert.Level.ERROR | _ as String | _       | null
    }

    def 'test set key'() {
        given:
            final level = Alert.Level.ERROR
            final title = _ as String
            final details = []
            final alert = new Alert(level, title, details)
            final key = _ as String

        when:
            alert.setKey(key)

        then:
            notThrown IllegalArgumentException
            alert.getLevel() == level
            alert.getTitle() == title
            alert.getDetails() == details
            alert.getKey() == key
    }

    def 'test construct Alert without a key'() {
        given:
            final level = Alert.Level.ERROR
            final title = _ as String
            final details = []

        when:
            final alert = new Alert(level, title, details)

        then:
            notThrown IllegalArgumentException
            alert.getKey() == null
            alert.getLevel() == level
            alert.getTitle() == title
            alert.getDetails() == details
    }
}