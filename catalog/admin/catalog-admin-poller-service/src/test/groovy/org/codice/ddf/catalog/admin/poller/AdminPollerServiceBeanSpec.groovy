/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.admin.poller

import spock.lang.Specification

class AdminPollerServiceBeanSpec extends Specification {

    def "test LDAP filter generation"() {
        setup:
        def apsb = new AdminPollerServiceBean(null, null)
        apsb.setIncludeAsSource(includes)
        apsb.setExcludeAsSource(excludes)

        expect:
        apsb.getServiceFactoryFilterProperties() == filter

        where:
        includes              | excludes       | filter
        null                  | null           | "(|(service.factoryPid=*))"
        []                    | []             | "(|(service.factoryPid=*))"
        ["foo", "bar"]        | []             | "(|(service.factoryPid=foo)(service.factoryPid=bar))"
        []                    | ["foo"]        | "(&(|(service.factoryPid=*))(&(!(service.factoryPid=foo))))"
        ["fus", "roh", "dah"] | ["foo", "bar"] | "(&(|(service.factoryPid=fus)(service.factoryPid=roh)(service.factoryPid=dah))(&(!(service.factoryPid=foo))(!(service.factoryPid=bar))))"
    }
}
