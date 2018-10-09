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
 **/
package org.codice.ddf.admin.configurator.impl

import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

import static org.codice.ddf.admin.configurator.impl.ConfigValidator.validatePropertiesPath

class ConfigValidatorSpec extends Specification {
    @Shared
    String originalDdfHome

    void setup() {
        originalDdfHome = System.getProperty('ddf.home')
        def tempDir = Files.createTempDirectory("testroot")

        def tempPath = tempDir.toString()
        System.setProperty('ddf.home', tempPath)
    }

    void cleanup() {
        if (originalDdfHome != null) {
            System.setProperty('ddf.home', originalDdfHome)
        } else {
            System.clearProperty('ddf.home')
        }
    }

    def 'test validate string none empty'() {
        setup:
        def msg = 'Message'

        when:
        ConfigValidator.validateString(input, msg)

        then:
        noExceptionThrown()

        where:
        input << ['abc', 'def', '   adf', 'd d asd']
    }

    def 'test validate string empty'() {
        setup:
        def msg = 'Message'

        when:
        ConfigValidator.validateString(input, msg)

        then:
        thrown(IllegalArgumentException)

        where:
        input << ['', '   ', null]
    }

    def 'test validate map none empty'() {
        setup:
        def msg = 'Message'

        when:
        ConfigValidator.validateMap(input, msg)

        then:
        noExceptionThrown()

        where:
        input << [[a: 'a', b: 'b'], [a: 'a'], [a: null], [a: '', b: null]]
    }

    def 'test validate map empty'() {
        setup:
        def msg = 'Message'

        when:
        ConfigValidator.validateMap(input, msg)

        then:
        thrown(IllegalArgumentException)

        where:
        input << [[:], null]
    }

    def 'test validate properties path'() {
        when:
        validatePropertiesPath(Paths.get(System.getProperty('ddf.home'), 'system', 'foo.properties'))

        then:
        noExceptionThrown()

        when:
        validatePropertiesPath(Paths.get(System.getProperty('ddf.home'), 'system', 'foo', 'bar.props'))

        then:
        noExceptionThrown()

        when:
        validatePropertiesPath(Paths.get(System.getProperty('ddf.home'), 'etc', 'ws-security', 'bar.props'))

        then:
        noExceptionThrown()

        // Failure cases
        when:
        validatePropertiesPath(Paths.get('foo.props'))

        then:
        thrown(IllegalArgumentException)

        when:
        validatePropertiesPath(Paths.get(System.getProperty('ddf.home'), 'foo.props'))

        then:
        thrown(IllegalArgumentException)

        when:
        validatePropertiesPath(Paths.get(System.getProperty('ddf.home'), 'system', 'foo.propss'))

        then:
        thrown(IllegalArgumentException)

        when:
        validatePropertiesPath(Paths.get(System.getProperty('ddf.home'), 'etc', 'users.properties'))

        then:
        thrown(IllegalArgumentException)

        when:
        validatePropertiesPath(Paths.get(System.getProperty('ddf.home'), 'etc', 'custom.system.properties'))

        then:
        thrown(IllegalArgumentException)
    }
}
