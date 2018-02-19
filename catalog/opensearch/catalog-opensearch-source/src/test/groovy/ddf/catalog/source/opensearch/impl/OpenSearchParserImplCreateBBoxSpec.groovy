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
package ddf.catalog.source.opensearch.impl

import ddf.catalog.impl.filter.SpatialDistanceFilter
import ddf.catalog.impl.filter.SpatialFilter
import org.apache.cxf.jaxrs.client.WebClient
import spock.lang.Specification

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.not

class OpenSearchParserImplCreateBBoxSpec extends Specification {

    def 'create BBox from point radius'() {
        given:
        final double searchRadiusInMeters = 804672 // 500 miles

        when:
        final bboxCoords = OpenSearchParserImpl.createBBoxFromPointRadius(lon, lat, searchRadiusInMeters)

        then:
        bboxCoords isPresentAndIs([expectedWest, expectedSouth, expectedEast, expectedNorth] as double[])

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

    def 'create BBox from polygon'() {
        expect:
        OpenSearchParserImpl.createBBoxFromPolygon(["1", "1", "2", "2", "3", "3", "4", "4", "1", "1"] as String[]) == [1, 1, 4, 4]
    }

    def 'populate spatial distance filter box'() {
        given:
        final OpenSearchParserImpl openSearchParserImpl = new OpenSearchParserImpl()
        final WebClient webClient = WebClient.create("http://www.example.com")

        when:
        openSearchParserImpl.populateGeospatial(
                webClient,
                spatialFilter,
                true,
                Arrays.asList(
                        "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                                .split(",")))

        then: 'URL should contain bbox parameter with the expected west,south,east,north value'
        final String urlStr = webClient.getCurrentURI().toString()
        urlStr containsString(OpenSearchParserImpl.GEO_BBOX)
        urlStr containsString(expectedBboxParameterString)

        where:
        spatialFilter                                            || expectedBboxParameterString
        new SpatialDistanceFilter("POINT (1 1)", 1.0)            || "0.9999910154833372,0.9999909866270258,1.0000089845166629,1.0000090133729742"
        new SpatialDistanceFilter("POINT (122 33)", 804672)      || "113.40866386728726,25.74719114208156,130.59133613271274,40.25280885791844"
        new SpatialFilter("POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))") || "1.0,1.0,4.0,4.0"
    }

    def 'populate spatial distance filter box invalid SpatialFilter'() {
        given:
        final OpenSearchParserImpl openSearchParserImpl = new OpenSearchParserImpl()
        final WebClient webClient = WebClient.create("http://www.example.com")

        when:
        openSearchParserImpl.populateGeospatial(
                webClient,
                spatialFilter,
                true,
                Arrays.asList(
                        "q,src,mr,start,count,mt,dn,lat,lon,radius,bbox,polygon,dtstart,dtend,dateName,filter,sort"
                                .split(",")))

        then:
        final String urlStr = webClient.getCurrentURI().toString()
        urlStr not(containsString(OpenSearchParserImpl.GEO_BBOX))

        where:
        spatialFilter << [
                new SpatialDistanceFilter("POLYGON (1 1)", 1.0),
                new SpatialFilter("POINT (1 1)"),
                null]
    }
}