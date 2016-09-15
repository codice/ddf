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
package org.codice.ddf.spatial.geocoding.plugin;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.spatial.geocoder.GeoCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Location;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.impl.ServiceSelector;

/**
 * The {@code GeoCoderPlugin} is responsible for adding a {@link Location#COUNTRY_CODE} in ISO 3166-1 format
 * to {@code Metacard}s that have the {@link Metacard#GEOGRAPHY} attribute.
 */
public class GeoCoderPlugin implements PreIngestPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoCoderPlugin.class);

    private static final String RADIUS_IN_KM = "radiusInKm";

    private static ServiceSelector<GeoCoder> geoCoderFactory;

    private int radiusInKm = 10;

    public GeoCoderPlugin(ServiceSelector<GeoCoder> geoCoderFactory) {
        if (geoCoderFactory == null) {
            throw new IllegalArgumentException(
                    "GeoCoderPlugin(): constructor argument 'geoCoderFactory' may not be null.");
        }

        this.geoCoderFactory = geoCoderFactory;
    }

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (input.getMetacards() == null) {
            return input;
        }

        for (Metacard metacard : input.getMetacards()) {
            Optional<String> wktLocation = Optional.ofNullable(metacard.getLocation());

            if (wktLocation.isPresent()) {
                if (countryCodeNeeded(metacard)) {
                    GeoCoder geocoder = geoCoderFactory.getService();

                    if (geocoder != null) {
                        Optional<String> alpha3CountryCode =
                                geocoder.getCountryCode(wktLocation.get(), radiusInKm);

                        if (alpha3CountryCode.isPresent()) {
                            setCountryCodeAttribute(metacard, alpha3CountryCode.get());
                            LOGGER.trace(
                                    "Setting metacard country code to {} for metacard with id {}",
                                    alpha3CountryCode.get(),
                                    metacard.getId());
                        }
                    }
                }
            }
        }
        return input;
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        // TODO: Implement when AdaptedMetacard updated to use taxonomy
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    public void updateConfiguration(Map<String, Object> properties) {
        LOGGER.trace("Updating GeoCoderPlugin search radius");

        Optional.of(properties)
                .map(p -> p.get(RADIUS_IN_KM))
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .ifPresent(this::setRadiusInKm);
    }

    public void setRadiusInKm(int radius) {
        this.radiusInKm = radius;
    }

    public int getRadius() {
        return radiusInKm;
    }

    private boolean countryCodeNeeded(Metacard metacard) {
        return metacard.getAttribute(Location.COUNTRY_CODE) == null;
    }

    private void setCountryCodeAttribute(Metacard metacard, Serializable countryCode) {
        metacard.setAttribute(new AttributeImpl(Location.COUNTRY_CODE, countryCode));
    }
}
