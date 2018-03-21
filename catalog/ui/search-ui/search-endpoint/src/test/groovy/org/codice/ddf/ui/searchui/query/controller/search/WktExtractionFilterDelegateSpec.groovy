/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.ui.searchui.query.controller.search

import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl
import org.geotools.filter.text.ecql.ECQL
import spock.lang.Specification

class WktExtractionFilterDelegateSpec extends Specification {

    def adapter = new GeotoolsFilterAdapterImpl();

    def delegate = new WktExtractionFilterDelegate()

    def "Can extract the wkt"() {
        setup:
        def filter = ECQL.toFilter(wkt);

        when:
        def result = adapter.adapt(filter, delegate)

        then:
        result == "POINT (1 1)"

        where:
        wkt | _
        "INTERSECTS(location, POINT (1 1))" | _
        "DISJOINT(location, POINT (1 1))" | _
        "CONTAINS(location, POINT (1 1))" | _
        "WITHIN(location, POINT (1 1))" | _
        "TOUCHES(location, POINT (1 1))" | _
        "CROSSES(location, POINT (1 1))" | _
        "OVERLAPS(location, POINT (1 1))" | _
        "BEYOND(location, POINT (1 1), 2, meters)" | _
        "BEYOND(location, POINT (1 1), 0, meters)" | _ // nearest neighbor
        "DWITHIN(location, POINT (1 1), 2, meters)" | _
        "INTERSECTS(location, POINT (1 1)) AND CONTAINS(location, POINT (2 2))" | _
        "INTERSECTS(location, POINT (1 1)) OR CONTAINS(location, POINT (2 2))" | _
    }

    def "Empty string if no wkt present"() {
        setup:
        def filter = ECQL.toFilter(wkt);

        when:
        def result = adapter.adapt(filter, delegate)

        then:
        result == ""

        where:
        wkt | _
        "title LIKE 'test'" | _
        "title LIKE 'foo' AND title LIKE 'bar'" | _
        "title LIKE 'foo' OR title LIKE 'bar'" | _
    }
}
