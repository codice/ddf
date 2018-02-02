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

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.not

class OpenSearchParserImplCreateBBoxSpec extends Specification {

    def 'create BBox from point radius'() {
        given:
        final double searchRadiusInMeters = 804672 // 500 miles

        expect:
        OpenSearchParserImpl.createBBoxFromPointRadius(lon, lat, searchRadiusInMeters) == [expectedWest, expectedSouth, expectedEast, expectedNorth]

        where:
        lon  | lat || expectedWest        | expectedSouth       | expectedEast        | expectedNorth
        -180 | -90 || -180.0              | -90.0               | 180.0               | -82.76341084722164
        -180 | 0   || 172.76341084722162  | -7.236589152778366  | -172.76341084722162 | 7.236589152778366
        -180 | 90  || -180.0              | 82.76341084722164   | 180.0               | 90.0
        -179 | -89 || -180.0              | -90.0               | 180.0               | -81.76341084722164
        -179 | 89  || -180.0              | 81.76341084722164   | 180.0               | 90.0
        -122 | -33 || -130.63841021419668 | -40.236589152778365 | -113.36158978580332 | -25.763410847221635
        -122 | 33  || -130.63841021419668 | 25.763410847221635  | -113.36158978580332 | 40.236589152778365
        0    | -90 || -180.0              | -90.0               | 180.0               | -82.76341084722164
        0    | 0   || -7.236589152778366  | -7.236589152778366  | 7.236589152778366   | 7.236589152778366
        0    | 90  || -180.0              | 82.76341084722164   | 180.0               | 90.0
        122  | -33 || 113.36158978580332  | -40.236589152778365 | 130.63841021419668  | -25.763410847221635
        122  | 33  || 113.36158978580332  | 25.763410847221635  | 130.63841021419668  | 40.236589152778365
        179  | -89 || -180.0              | -90.0               | 180.0               | -81.76341084722164
        179  | 89  || -180.0              | 81.76341084722164   | 180.0               | 90.0
        180  | -90 || -180.0              | -90.0               | 180.0               | -82.76341084722164
        180  | 0   || 172.76341084722162  | -7.236589152778366  | -172.76341084722162 | 7.236589152778366
        180  | 90  || -180.0              | 82.76341084722164   | 180.0               | 90.0
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

        then:
        final String urlStr = webClient.getCurrentURI().toString()
        urlStr containsString(OpenSearchParserImpl.GEO_BBOX)

        where:
        spatialFilter << [
                new SpatialDistanceFilter("POINT (1 1)", 1.0),
                new SpatialDistanceFilter("POINT (122 33)", 1.0),
                new SpatialFilter("POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))")]
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