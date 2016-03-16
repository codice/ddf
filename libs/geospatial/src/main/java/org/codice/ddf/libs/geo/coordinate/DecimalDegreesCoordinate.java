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
package org.codice.ddf.libs.geo.coordinate;

/**
 * Coordinate that contains latitude/longitude values in Decimal Degrees format
 */
public class DecimalDegreesCoordinate implements Cloneable {

    private Latitude lat;

    private Longitude lon;

    /**
     * Constructor which takes in a latitude and longitude value in decimal degree format
     * <p>
     * Note: an IllegalArgumentException will be thrown if the latitude/longitude values
     * are out of range (latitude must be between -90 and 90 and longitude must be
     * between -180 and 180)
     *
     * @param lat the decimal degree latitude value
     * @param lon the decimal degree longitude value
     */
    public DecimalDegreesCoordinate(double lat, double lon) {
        this.lat = new Latitude(lat);
        this.lon = new Longitude(lon);
    }

    /**
     * Constructor which takes in a Latitude Object and Longitude Object
     * <p>
     * Note: an IllegalArgumentException will be thrown in the case the Latitude
     * or Longitude objects are null.
     *
     * @param lat the Latitude value in the Decimal Degree Coordinate
     * @param lon the longitude value in the Decimal Degree Coordinate
     */
    public DecimalDegreesCoordinate(Latitude lat, Longitude lon) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Latitude and Longitude must be non null");
        }
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Gets the Decimal Degree latitude value
     *
     * @return the decimal degree value of the latitude
     */
    public double getLatitiudeDegrees() {
        return lat.getDecimalDegrees();
    }

    /**
     * Gets the Decimal Degree longitude value
     *
     * @return the decimal degree value of the longitude
     */
    public double getLongitudeDegrees() {
        return lon.getDecimalDegrees();
    }

    /**
     * Gets the Latitude Object for this Decimal Degree Coordinate
     *
     * @return the Latitude Object
     */
    public Latitude getLatitude() {
        return lat;
    }

    /**
     * Gets the Longitude Object for this Decimal Degree Coordinate
     *
     * @return the Longitude Object
     */
    public Longitude getLongitude() {
        return lon;
    }

    @Override
    public String toString() {
        return lat.toString() + " " + lon.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DecimalDegreesCoordinate coord = (DecimalDegreesCoordinate) super.clone();
        coord.lat = lat;
        coord.lon = lon;
        return coord;
    }
}
