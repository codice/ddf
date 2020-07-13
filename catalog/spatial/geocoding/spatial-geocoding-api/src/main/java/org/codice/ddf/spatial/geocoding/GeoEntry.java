/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.geocoding;

/** A {@code GeoEntry} represents a GeoNames entry. */
public class GeoEntry {
  private final String name;

  private final Double latitude;

  private final Double longitude;

  private final String featureCode;

  private final long population;

  private final String alternateNames;

  private final String countryCode;

  private final String importLocation;

  private final int gazetteerSort;

  private final String featureClass;

  private GeoEntry(final Builder builder) {
    name = builder.name;
    latitude = builder.latitude;
    longitude = builder.longitude;
    featureCode = builder.featureCode;
    population = builder.population;
    alternateNames = builder.alternateNames;
    countryCode = builder.countryCode;
    importLocation = builder.importLocation;
    gazetteerSort = builder.gazetteerSort;
    featureClass = builder.featureClass;
  }

  public static class Builder {
    private String name;

    private Double latitude;

    private Double longitude;

    private String featureCode;

    private long population;

    private String alternateNames;

    private String countryCode;

    private String importLocation;

    private int gazetteerSort;

    private String featureClass;

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder latitude(final Double latitude) {
      this.latitude = latitude;
      return this;
    }

    public Builder longitude(final Double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder featureCode(final String featureCode) {
      this.featureCode = featureCode;
      return this;
    }

    public Builder featureClass(final String featureClass) {
      this.featureClass = featureClass;
      return this;
    }

    public Builder population(final long population) {
      this.population = population;
      return this;
    }

    public Builder gazetteerSort(final Integer gazetteerSort) {
      this.gazetteerSort = gazetteerSort.intValue();
      return this;
    }

    public Builder alternateNames(final String alternateNames) {
      this.alternateNames = alternateNames;
      return this;
    }

    public Builder countryCode(final String countryCode) {
      this.countryCode = countryCode;
      return this;
    }

    public Builder importLocation(final String importLocation) {
      this.importLocation = importLocation;
      return this;
    }

    public GeoEntry build() {
      return new GeoEntry(this);
    }

    @Override
    public String toString() {
      return "Builder{" +
          "name='" + name + '\'' +
          ", latitude=" + latitude +
          ", longitude=" + longitude +
          ", featureCode='" + featureCode + '\'' +
          ", population=" + population +
          ", alternateNames='" + alternateNames + '\'' +
          ", countryCode='" + countryCode + '\'' +
          ", importLocation='" + importLocation + '\'' +
          ", gazetteerSort=" + gazetteerSort +
          ", featureClass='" + featureClass + '\'' +
          '}';
    }
  }

  public String getName() {
    return name;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
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

  public String getCountryCode() {
    return countryCode;
  }

  public String getImportLocation() {
    return importLocation;
  }

  public String getFeatureClass() {
    return featureClass;
  }

  @Override
  public String toString() {
    return "GeoEntry{" +
        "name='" + name + '\'' +
        ", latitude=" + latitude +
        ", longitude=" + longitude +
        ", featureCode='" + featureCode + '\'' +
        ", population=" + population +
        ", alternateNames='" + alternateNames + '\'' +
        ", countryCode='" + countryCode + '\'' +
        ", importLocation='" + importLocation + '\'' +
        ", gazetteerSort=" + gazetteerSort +
        ", featureClass='" + featureClass + '\'' +
        '}';
  }
}
