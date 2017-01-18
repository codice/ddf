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
package groovy.org.codice.ddf.catalog.async.data.impl

import org.codice.ddf.catalog.async.data.impl.ProcessItemImpl
import spock.lang.Specification

class ProcessItemImplTest extends Specification {

    def 'ProcessItemImpl null metacard throws IllegalArgumentException'() {
        when:
        new ProcessItemImpl(null)

        then:
        thrown(IllegalArgumentException)
    }
}