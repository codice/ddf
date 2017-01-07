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
package org.codice.ddf.admin.api

import org.codice.ddf.admin.api.persist.handlers.AdminConfigHandler
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean
import org.codice.ddf.admin.api.persist.ConfiguratorException
import spock.lang.Specification

class AdminConfigHandlerTest extends Specification {
    def initProps

    def setup() {
        initProps = [key1: 'val1', key2: 'val2', key3: 'val3']
    }

    def 'test write configs to an unknown bundle fails'() {
        setup:
        def cfgMbean = Mock(ConfigurationAdminMBean)
        cfgMbean.getProperties('xxx') >> { throw new IOException('unknown pid') }
        def newProps = [key1: 'newVal1', key4: 'val4', key5: 'val5']

        when:
        def handler = AdminConfigHandler.instance('xxx', newProps, false, cfgMbean)

        then:
        thrown(ConfiguratorException)
    }

    def 'test write new configs and keep old configs'() {
        setup:
        def cfgMbean = Mock(ConfigurationAdminMBean)
        def newProps = [key1: 'newVal1', key4: 'val4', key5: 'val5']
        def combinedProps = initProps << newProps

        when:
        def handler = AdminConfigHandler.instance('xxx', newProps, true, cfgMbean)

        then:
        1 * cfgMbean.getProperties('xxx') >> initProps

        when:
        handler.commit()

        then:
        1 * cfgMbean.update('xxx', combinedProps)
    }

    def 'test write new configs and remove old configs'() {
        setup:
        def cfgMbean = Mock(ConfigurationAdminMBean)
        def newProps = [key1: 'newVal1', key4: 'val4', key5: 'val5']

        when:
        def handler = AdminConfigHandler.instance('xxx', newProps, false, cfgMbean)

        then:
        1 * cfgMbean.getProperties('xxx') >> initProps

        when:
        handler.commit()

        then:
        1 * cfgMbean.update('xxx', newProps)
    }

    def 'test rollback'() {
        setup:
        def cfgMbean = Mock(ConfigurationAdminMBean)
        def newProps = [key1: 'newVal1', key4: 'val4', key5: 'val5']

        when:
        def handler = AdminConfigHandler.instance('xxx', newProps, false, cfgMbean)

        then:
        1 * cfgMbean.getProperties('xxx') >> initProps

        when:
        handler.commit()

        then:
        1 * cfgMbean.update('xxx', newProps)

        when:
        handler.rollback()

        then:
        1 * cfgMbean.update('xxx', initProps)
    }
}
