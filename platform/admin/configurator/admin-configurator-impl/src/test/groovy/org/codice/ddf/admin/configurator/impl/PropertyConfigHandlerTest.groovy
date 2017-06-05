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

import org.codice.ddf.admin.configurator.ConfiguratorException
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

class PropertyConfigHandlerTest extends Specification {
    @Rule
    TemporaryFolder tempFolder

    @Shared
    File workFolder

    @Shared
    File file

    def setup() {
        workFolder = tempFolder.newFolder()
        file = new File(workFolder, 'test.properties')

        def initProps = new Properties()
        initProps.put('key1', 'val1')
        initProps.put('key2', 'val2')
        initProps.put('key3', 'val3')
        file.newWriter().withWriter {
            initProps.store(it, null)
        }
    }

    def 'test write properties to a new file'() {
        setup:
        def configs = [key1: 'newVal1', key4: 'val4', key5: 'val5']
        def newFile = new File(workFolder, "doesnotexistyet.properties")
        def props = new Properties()
        def handler = new PropertyOperation.CreateHandler(newFile.toPath(), configs)

        when:
        handler.commit()
        newFile.newReader().with {
            props.load(it)
        }

        then:
        props.getProperty('key1') == 'newVal1'
        props.getProperty('key4') == 'val4'
        props.getProperty('key5') == 'val5'
    }

    def 'test rollback new properties file causes delete'() {
        setup:
        def configs = [key1: 'newVal1', key4: 'val4', key5: 'val5']
        def newFile = new File(workFolder, "doesnotexistyet.properties")
        def handler = new PropertyOperation.CreateHandler(newFile.toPath(), configs)

        when:
        handler.commit()
        handler.rollback()

        then:
        !newFile.exists()
    }

    def 'test delete and rollback'() {
        setup:
        def handler = new PropertyOperation.DeleteHandler(file.toPath())
        def props = new Properties()

        when:
        handler.commit()

        then:
        !file.exists()

        when:
        handler.rollback()
        file.newReader().with {
            props.load(it)
        }

        then:
        file.exists()
        props.getProperty('key1') == 'val1'
        props.getProperty('key2') == 'val2'
        props.getProperty('key3') == 'val3'
    }

    def 'test update properties to an unknown file fails'() {
        setup:
        def configs = [key1: 'newVal1', key4: 'val4', key5: 'val5']
        def badFile = new File(workFolder, "doesnotexist")

        when:
        def handler = new PropertyOperation.UpdateHandler(badFile.toPath(), configs, true)

        then:
        thrown(ConfiguratorException)
    }

    def 'test write new properties and keep old properties'() {
        setup:
        def configs = [key1: 'newVal1', key4: 'val4', key5: 'val5']
        def handler = new PropertyOperation.UpdateHandler(file.toPath(), configs, true)
        def props = new Properties()

        when:
        handler.commit()
        file.newReader().with {
            props.load(it)
        }

        then:
        props.getProperty('key1') == 'newVal1'
        props.getProperty('key2') == 'val2'
        props.getProperty('key3') == 'val3'
        props.getProperty('key4') == 'val4'
        props.getProperty('key5') == 'val5'
    }

    def 'test write new properties and remove old properties'() {
        setup:
        def configs = [key1: 'newVal1', key4: 'val4', key5: 'val5']
        def handler = new PropertyOperation.UpdateHandler(file.toPath(), configs, false)
        def props = new Properties()

        when:
        handler.commit()
        file.newReader().with {
            props.load(it)
        }

        then:
        props.getProperty('key1') == 'newVal1'
        props.getProperty('key4') == 'val4'
        props.getProperty('key5') == 'val5'
    }

    def 'test rollback'() {
        setup:
        def configs = [key1: 'newVal1', key4: 'val4', key5: 'val5']
        def handler = new PropertyOperation.UpdateHandler(file.toPath(), configs, false)
        def props = new Properties()

        when:
        handler.commit()
        handler.rollback()
        file.newReader().with {
            props.load(it)
        }

        then:
        props.getProperty('key1') == 'val1'
        props.getProperty('key2') == 'val2'
        props.getProperty('key3') == 'val3'
        props.getProperty('key4') == null
        props.getProperty('key5') == null
    }
}
