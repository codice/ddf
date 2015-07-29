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
 **/

package org.codice.ddf.spatial.geocoding;

/**
 * A {@code GeoEntry} represents a GeoNames entry.
 */
public class GeoEntry {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final String featureCode;
    private final long population;
    private final String alternateNames;

    private GeoEntry(final Builder builder) {
        name = builder.name;
        latitude = builder.latitude;
        longitude = builder.longitude;
        featureCode = builder.featureCode;
        population = builder.population;
        alternateNames = builder.alternateNames;
    }

    public static class Builder {
        private String name;
        private double latitude;
        private double longitude;
        private String featureCode;
        private long population;
        private String alternateNames;

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder latitude(final double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(final double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder featureCode(final String featureCode) {
            this.featureCode = featureCode;
            return this;
        }

        public Builder population(final long population) {
            this.population = population;
            return this;
        }

        public Builder alternateNames(final String alternateNames) {
            this.alternateNames = alternateNames;
            return this;
        }

        public GeoEntry build() {
            return new GeoEntry(this);
        }
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public long getPopulation() {
        return population;
    }

    public String getAlternateNames() {
        return alternateNames;
    }
}
