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

import ddf.catalog.data.Attribute
import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.AttributeImpl
import ddf.catalog.data.impl.MetacardImpl
import ddf.catalog.data.types.Core
import ddf.catalog.data.types.Location
import ddf.catalog.operation.CreateRequest
import ddf.catalog.operation.UpdateRequest
import ddf.catalog.plugin.PluginExecutionException
import ddf.catalog.util.impl.ServiceSelector
import org.codice.ddf.spatial.geocoder.GeoCoder
import spock.lang.Specification

import java.util.AbstractMap.SimpleEntry
import java.util.Map.Entry

import static org.mockito.Matchers.anyString

class GeoCoderPluginSpec extends Specification {

    private static Optional<String> countryCode = Optional.of('NO')

    private static String locationWKT = 'POINT(10.402439 63.418399)'

    private static String invalidLocationWkt = 'POINT(350.0 350.0)'

    private CreateRequest createRequest

    private UpdateRequest updateRequest

    private ServiceSelector<GeoCoder> geocoderFactory

    private GeoCoderPlugin geoCoderPlugin;

    def setup() {
        createRequest = Mock(CreateRequest)
        updateRequest = Mock(UpdateRequest)
    }

    def 'test null geoCoderFactory'() {
        when:
        new GeoCoderPlugin(null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'test geo located metacard CreateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false);
        createRequest.getMetacards() >> getTestMetacards(new AttributeImpl(Core.LOCATION, locationWKT))

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE).getValue() == countryCode.get()
    }

    def 'test metacard already has country code CreateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)
        createRequest.getMetacards() >> getTestMetacards(new AttributeImpl(Location.COUNTRY_CODE, "USA"), new AttributeImpl(Core.LOCATION, locationWKT))

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE).getValue() == "USA"
    }

    def 'test metacard has no geography CreateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)
        createRequest.getMetacards() >> getTestMetacards()

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def 'test no metacards CreateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)
        createRequest.getMetacards() >> null

        def response = geoCoderPlugin.process(createRequest)

        expect:
        response.getMetacards() == null
    }

    def 'test no geocoder CreateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, true)
        createRequest.getMetacards() >> getTestMetacards(new AttributeImpl(Core.LOCATION, locationWKT))

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def 'test no valid country code CreateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(Optional.empty(), false)
        createRequest.getMetacards() >> getTestMetacards(new AttributeImpl(Core.LOCATION, locationWKT))

        def response = geoCoderPlugin.process(createRequest)
        def processedMetacard = response.getMetacards().get(0)

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def 'test geo located metacard UpdateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false);
        updateRequest.getUpdates() >> getTestUpdates(new AttributeImpl(Core.LOCATION, locationWKT))

        def response = geoCoderPlugin.process(updateRequest)
        def processedMetacard = response.getUpdates().get(0).getValue()

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE).getValue() == countryCode.get()
    }

    def 'test metacard already has country code UpdateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)
        updateRequest.getUpdates() >> getTestUpdates(new AttributeImpl(Location.COUNTRY_CODE, "USA"), new AttributeImpl(Core.LOCATION, locationWKT))

        def response = geoCoderPlugin.process(updateRequest)
        def processedMetacard = response.getUpdates().get(0).getValue()

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE).getValue() == "USA"
    }

    def 'test metacard has no geography UpdateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)
        updateRequest.getUpdates() >> getTestUpdates()

        def response = geoCoderPlugin.process(updateRequest)
        def processedMetacard = response.getUpdates().get(0).getValue()

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def 'test no metacards UpdateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, false)
        updateRequest.getUpdates() >> null

        def response = geoCoderPlugin.process(updateRequest)

        expect:
        response.getUpdates() == null
    }

    def 'test no geocoder UpdateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(countryCode, true)
        updateRequest.getUpdates() >> getTestUpdates(new AttributeImpl(Core.LOCATION, locationWKT))

        def response = geoCoderPlugin.process(updateRequest)
        def processedMetacard = response.getUpdates().get(0).getValue()

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def 'test no valid country code UpdateRequest'() {
        setup:
        geoCoderPlugin = initGeoCoderPlugin(Optional.empty(), false)
        updateRequest.getUpdates() >> getTestUpdates(new AttributeImpl(Core.LOCATION, locationWKT))

        def response = geoCoderPlugin.process(updateRequest)
        def processedMetacard = response.getUpdates().get(0).getValue()

        expect:
        processedMetacard.getAttribute(Location.COUNTRY_CODE) == null
    }

    def 'test update configuration'() {
        setup:
        def map = new HashMap<String, Object>()
        map.put(GeoCoderPlugin.RADIUS_IN_KM, 15)

        geoCoderPlugin = initGeoCoderPlugin(countryCode, false);

        geoCoderPlugin.updateConfiguration(map)

        expect:
        geoCoderPlugin.getRadius() == 15
    }

    def 'test update with null configuration'() {
        setup:
        def map = new HashMap<String, Object>()

        when: "geocoderplugin is provided a null configuration"
        map.put(GeoCoderPlugin.RADIUS_IN_KM, 15)

        geoCoderPlugin = initGeoCoderPlugin(countryCode, false);

        geoCoderPlugin.updateConfiguration(map)
        geoCoderPlugin.updateConfiguration(null)

        then: "the null configuration is ignored"
        geoCoderPlugin.getRadius() == 15
    }

    def 'test invalid geo location Metacard CreateRequest'() {
        setup:
        GeoCoderPlugin errorGeoCoderPlugin = initErrorGeoCoderPlugin(countryCode, false);
        createRequest.getMetacards() >> getTestMetacards(new AttributeImpl(Core.LOCATION, invalidLocationWkt))

        when:
        errorGeoCoderPlugin.process(createRequest)

        then:
        thrown(PluginExecutionException)
    }

    def 'test invalid geo location Metacard UpdateRequest'() {
        setup:
        GeoCoderPlugin errorGeoCoderPlugin = initErrorGeoCoderPlugin(countryCode, false);
        updateRequest.getUpdates() >> getTestUpdates(new AttributeImpl(Core.LOCATION, invalidLocationWkt))

        when:
        errorGeoCoderPlugin.process(updateRequest)

        then:
        thrown(PluginExecutionException)
    }

    def initGeoCoderPlugin(Optional<String> countryCode, boolean overrideDefaultGeocoder) {
        GeoCoder geocoder = (overrideDefaultGeocoder == true) ? null : Mock(GeoCoder) {
            getCountryCode(_ as String, _ as Integer) >> countryCode
        }

        geocoderFactory = Mock(ServiceSelector) {
            getService() >> geocoder
        }

        return new GeoCoderPlugin(geocoderFactory)
    }

    def initErrorGeoCoderPlugin(Optional<String> countryCode, boolean overrideDefaultGeocoder) {
        GeoCoder geocoder = (overrideDefaultGeocoder == true) ? null : Mock(GeoCoder) {
            getCountryCode(_ as String, _ as Integer) >> {
                _ -> throw new PluginExecutionException("Invalid Location")
            }
        }

        geocoderFactory = Mock(ServiceSelector) {
            getService() >> geocoder
        }

        return new GeoCoderPlugin(geocoderFactory)
    }

    def getTestUpdates(Attribute... attributes) {
        def metacard = new MetacardImpl();

        Arrays.stream(attributes).forEach { attribute ->
            metacard.setAttribute(attribute)
        }

        List<Entry<Serializable, Metacard>> updates = new ArrayList<Entry<Serializable, Metacard>>()
        updates.add(new SimpleEntry<Serializable, Metacard>(anyString(), metacard));

        return updates
    }

    def getTestMetacards(Attribute... attributes) {
        def metacard = new MetacardImpl();

        Arrays.stream(attributes).forEach { attribute ->
            metacard.setAttribute(attribute)
        }

        List<Metacard> metacards = Collections.singletonList(metacard)

        return metacards
    }
}
