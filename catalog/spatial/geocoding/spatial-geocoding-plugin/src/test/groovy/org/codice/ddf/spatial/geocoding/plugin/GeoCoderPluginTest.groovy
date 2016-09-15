/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.geocoding.plugin

import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.AttributeImpl
import ddf.catalog.data.impl.MetacardImpl
import ddf.catalog.data.types.Core
import ddf.catalog.data.types.Location
import ddf.catalog.operation.CreateRequest
import ddf.catalog.util.impl.ServiceSelector
import org.codice.ddf.spatial.geocoder.GeoCoder
import spock.lang.Specification

class GeoCoderPluginTest extends Specification {

    private static Optional<String> countryCode = Optional.of('NO')

    private static String locationWKT = 'POINT(10.402439 63.418399)'

    private CreateRequest createRequest

    private ServiceSelector<GeoCoder> geocoderFactory

    private List<Metacard> metacards

    private Metacard metacard

    private GeoCoderPlugin geoCoderPlugin;

    def setup() {
        createRequest = Mock(CreateRequest)
    }

    def 'test geo located metacard CreateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false);

        metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Core.LOCATION, locationWKT))

        metacards = Collections.singletonList(metacard)
        createRequest.getMetacards() >> metacards

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE).getValue().equals(countryCode.get())
    }

    def 'test null geoCoderFactory'() {
        when:
        new GeoCoderPlugin(null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'test update configuration'() {
        setup:
        def map = new HashMap<String, Object>()
        map.put("radiusInKm", 15)

        geoCoderPlugin = initGeoCoderPlugin(countryCode, false);

        geoCoderPlugin.updateConfiguration(map)

        expect:
        geoCoderPlugin.getRadius() == 15
    }

    def 'test metacard already has country code'() {
        setup:
        metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Location.COUNTRY_CODE, "USA"))
        metacard.setAttribute(new AttributeImpl(Core.LOCATION, locationWKT))

        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)

        metacards = Collections.singletonList(metacard)
        createRequest.getMetacards() >> metacards

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE).getValue().equals("USA")
    }

    def 'test metacard has no geography'() {
        setup:
        metacard = new MetacardImpl();

        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)

        metacards = Collections.singletonList(metacard)
        createRequest.getMetacards() >> metacards

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def 'test no metacards'() {
        setup:
        createRequest.getMetacards() >> null

        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)

        def response = geoCoderPlugin.process(createRequest)

        expect:
        response.getMetacards() == null
    }

    def 'test no geocoder'() {
        setup:
        metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Core.LOCATION, locationWKT))

        geoCoderPlugin = initGeoCoderPlugin(countryCode, true)

        metacards = Collections.singletonList(metacard)
        createRequest.getMetacards() >> metacards

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def 'test no valid country code'() {
        setup:
        metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Core.LOCATION, locationWKT))

        geoCoderPlugin = initGeoCoderPlugin(Optional.empty(), false)

        metacards = Collections.singletonList(metacard)
        createRequest.getMetacards() >> metacards

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def initGeoCoderPlugin(Optional<String> countryCode, boolean overridDefaultGeocoder) {
        GeoCoder geocoder = (overridDefaultGeocoder == true) ? null : Mock(GeoCoder) {
            getCountryCode(_ as String, _ as Integer) >> countryCode
        }

        geocoderFactory = Mock(ServiceSelector) {
            getService() >> geocoder
        }

        return new GeoCoderPlugin(geocoderFactory)
    }
}
