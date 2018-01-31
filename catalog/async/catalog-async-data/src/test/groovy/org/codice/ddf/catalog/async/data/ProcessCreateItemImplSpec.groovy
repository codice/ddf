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
package org.codice.ddf.catalog.async.data

import ddf.catalog.data.Metacard
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource
import org.codice.ddf.catalog.async.data.impl.ProcessCreateItemImpl
import spock.lang.Specification

class ProcessCreateItemImplSpec extends Specification {

    def 'test ProcessCreateItemImpl(ProcessResource, Metacard) success'() {
        when:
        def processResource = Mock(ProcessResource)
        def metacard = Mock(Metacard)

        def processCreateItem = new ProcessCreateItemImpl(processResource, metacard)

        then:
        processCreateItem.getProcessResource() == processResource
        processCreateItem.getMetacard() == metacard
    }

    def 'test ProcessCreateItemImpl metacard is modified'() {
        when:
        def processResource = Mock(ProcessResource)
        def metacard = Mock(Metacard)

        def processCreateItem = new ProcessCreateItemImpl(processResource, metacard)

        then:
        processCreateItem.isMetacardModified()
    }

    def 'test ProcessCreateItemImpl metacard is not modified, then modified'() {
        when:
        def processResource = Mock(ProcessResource)
        def metacard = Mock(Metacard)

        def processCreateItem = new ProcessCreateItemImpl(processResource, metacard, false)

        then:
        !processCreateItem.isMetacardModified()
        processCreateItem.markMetacardAsModified()
        processCreateItem.isMetacardModified()
    }

    def 'test ProcessCreateItemImpl null Metacard throws IllegalArgumentException'() {
        when:
        new ProcessCreateItemImpl(Mock(ProcessResource), null)

        then:
        thrown(IllegalArgumentException)
    }
}