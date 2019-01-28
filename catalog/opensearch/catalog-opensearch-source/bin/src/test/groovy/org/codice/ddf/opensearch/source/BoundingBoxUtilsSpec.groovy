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
package org.codice.ddf.opensearch.source

import spock.lang.Specification
import spock.lang.Unroll

class BoundingBoxUtilsSpec extends Specification {

    @Unroll
    def 'create BBox from point radius (#lon, #lat)'(double lon, double lat, double expectedWest, double expectedSouth, double expectedEast, double expectedNorth) {
        given:
        final double searchRadiusInMeters = 804672 // 500 miles

        when:
        final BoundingBox boundingBox = BoundingBoxUtils.createBoundingBox(new PointRadius(lon, lat, searchRadiusInMeters))

        then:
        with(boundingBox) {
            getWest() == expectedWest
            getSouth() == expectedSouth
            getEast() == expectedEast
            getNorth() == expectedNorth
        }

        where:
        lon  | lat || expectedWest        | expectedSouth      | expectedEast        | expectedNorth
        -180 | -90 || -180.0              | -90.0              | 180.0               | -82.74719114208156
        -180 | 0   || 172.77150843696975  | -7.25280885791844  | -172.77150843696975 | 7.25280885791844
        -180 | 90  || -180.0              | 82.74719114208156  | 180.0               | 90.0
        -179 | -89 || -180.0              | -90.0              | 180.0               | -81.74719114208156
        -179 | 89  || -180.0              | 81.74719114208156  | 180.0               | 90.0
        -122 | -33 || -130.59133613271274 | -40.25280885791844 | -113.40866386728726 | -25.74719114208156
        -122 | 33  || -130.59133613271274 | 25.74719114208156  | -113.40866386728726 | 40.25280885791844
        0    | -90 || -180.0              | -90.0              | 180.0               | -82.74719114208156
        0    | 0   || -7.228491563030235  | -7.25280885791844  | 7.228491563030235   | 7.25280885791844
        0    | 90  || -180.0              | 82.74719114208156  | 180.0               | 90.0
        122  | -33 || 113.40866386728726  | -40.25280885791844 | 130.59133613271274  | -25.74719114208156
        122  | 33  || 113.40866386728726  | 25.74719114208156  | 130.59133613271274  | 40.25280885791844
        179  | -89 || -180.0              | -90.0              | 180.0               | -81.74719114208156
        179  | 89  || -180.0              | 81.74719114208156  | 180.0               | 90.0
        180  | -90 || -180.0              | -90.0              | 180.0               | -82.74719114208156
        180  | 0   || 172.77150843696975  | -7.25280885791844  | -172.77150843696975 | 7.25280885791844
        180  | 90  || -180.0              | 82.74719114208156  | 180.0               | 90.0
    }
}