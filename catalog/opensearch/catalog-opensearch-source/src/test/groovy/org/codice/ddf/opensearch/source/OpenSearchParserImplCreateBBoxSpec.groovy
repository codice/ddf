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

import org.apache.cxf.jaxrs.client.WebClient
import org.codice.ddf.opensearch.OpenSearchConstants
import spock.lang.Specification

import static org.hamcrest.Matchers.containsString

class OpenSearchParserImplCreateBBoxSpec extends Specification {

    def 'create BBox from point radius'() {
        given:
        final OpenSearchParser openSearchParser = new OpenSearchParserImpl()
        final WebClient webClient = WebClient.create("http://www.example.com")
        final double searchRadiusInMeters = 804672 // 500 miles
        final PointRadiusSearch pointRadiusSearch = new PointRadiusSearch(lon, lat, searchRadiusInMeters)

        when:
        openSearchParser.populatePointRadiusParameters(
                webClient,
                pointRadiusSearch,
                true,
                Arrays.asList(
                        "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                                .split(",")))

        then: 'the web client URI should contain bbox parameter with the expected west,south,east,north value'
        final String urlStr = webClient.getCurrentURI().toString()
        urlStr containsString(OpenSearchConstants.BBOX)
        urlStr containsString(expectedBboxParameterString)

        where:
        lon  | lat || expectedBboxParameterString
        -180 | -90 || "-180.0,-90.0,180.0,-82.74719114208156"
        -180 | 0   || "172.77150843696975,-7.25280885791844,-172.77150843696975,7.25280885791844"
        -180 | 90  || "-180.0,82.74719114208156,180.0,90.0"
        -179 | -89 || "-180.0,-90.0,180.0,-81.74719114208156"
        -179 | 89  || "-180.0,81.74719114208156,180.0,90.0"
        -122 | -33 || "-130.59133613271274,-40.25280885791844,-113.40866386728726,-25.74719114208156"
        -122 | 33  || "-130.59133613271274,25.74719114208156,-113.40866386728726,40.25280885791844"
        0    | -90 || "-180.0,-90.0,180.0,-82.74719114208156"
        0    | 0   || "-7.228491563030235,-7.25280885791844,7.228491563030235,7.25280885791844"
        0    | 90  || "-180.0,82.74719114208156,180.0,90.0"
        122  | -33 || "113.40866386728726,-40.25280885791844,130.59133613271274,-25.74719114208156"
        122  | 33  || "113.40866386728726,25.74719114208156,130.59133613271274,40.25280885791844"
        179  | -89 || "-180.0,-90.0,180.0,-81.74719114208156"
        179  | 89  || "-180.0,81.74719114208156,180.0,90.0"
        180  | -90 || "-180.0,-90.0,180.0,-82.74719114208156"
        180  | 0   || "172.77150843696975,-7.25280885791844,-172.77150843696975,7.25280885791844"
        180  | 90  || "-180.0,82.74719114208156,180.0,90.0"
    }
}