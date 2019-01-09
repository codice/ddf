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

import org.codice.ddf.catalog.async.data.api.internal.ProcessItem
import org.codice.ddf.catalog.async.data.impl.ProcessRequestImpl
import spock.lang.Specification

class ProcessRequestImplSpec extends Specification {

    def 'test ProcessRequestImpl(ProcessItems, Map<String, Serializable>) success'() {
        setup:
        def processItems = [Mock(ProcessItem)]
        def properties = ["key": "value"]

        when:
        def request = new ProcessRequestImpl<>(processItems, properties)

        then:
        request.getProcessItems() == processItems
        request.getProperties() == properties
        properties.get("key") == "value"
    }

    def 'ProcessRequestImpl null ProcessItems throws IllegalArgumentException'() {
        when:
        new ProcessRequestImpl<>(null, [:])

        then:
        thrown(IllegalArgumentException)
    }
}