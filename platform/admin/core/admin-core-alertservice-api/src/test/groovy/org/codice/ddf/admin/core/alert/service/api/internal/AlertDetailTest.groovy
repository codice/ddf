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
package org.codice.ddf.admin.core.alert.service.api.internal

import spock.lang.Specification

class AlertDetailTest extends Specification {

    def 'test construct Alert'() {
        given:
            final message = _ as String

        when:
            def alertDetail = new AlertDetail(message)

        then:
            alertDetail.getMessage() == message
    }

    def 'test construct AlertDetail with an invalid message'() {
        when:
            new AlertDetail(message)

        then:
            thrown(IllegalArgumentException)

        where:
            message << [null, ""]
    }
}