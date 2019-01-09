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

package ddf.catalog.operation.faceting

import ddf.catalog.operation.impl.FacetAttributeResultImpl
import spock.lang.Specification

class FacetedAttributeResultSpec extends Specification {
    static testSameLenValues = ["keyboards", "cutting boards", "school boards", "skateboards"]
    static testShorterValues = ["apples", "oranges"]
    static testLongerValues = ["barbecued", "boiled", "broiled", "sauted", "kabob'd", "creole",
                               "gumbo", "pan fried", "deep fried", "stir-fried", "pineapple",
                               "lemon", "coconut", "pepper", "soup", "stew", "salad", "and potatoes",
                               "burger", "sandwich"]

    static testCounts = [54L, 123L, 99L, 6032L]

    def "test empty lists"() {
        when:
        def ffr = new FacetAttributeResultImpl("emptyTest", [], [])

        then:
        ffr.getAttributeName() == "emptyTest"
        ffr.getFacetValues().isEmpty()
    }

    def "test creation with equal length value and count lists"() {
        when:
        def ffr = new FacetAttributeResultImpl("equalTest", testSameLenValues, testCounts)

        then:
        ffr.facetValues.size() == 4
    }

    def "test creation with unequal length value and count lists"(values, long length) {
        when:
        def ffr = new FacetAttributeResultImpl("unequalTest", values, testCounts)

        then:
        ffr.facetValues.size() == length

        where:
        values << [testShorterValues, testLongerValues]
        length << [2, 4]
    }

}