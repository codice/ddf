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

import org.codice.ddf.libs.geo.GeoFormatException;

/**
 * Coordinate that stores and provides access to data using Degrees Minutes Seconds format
 */
public class DmsCoordinate {

    private Latitude latitude = null;

    private Longitude longitude = null;

    /**
     * Constructor which takes in a Latitude Object and Longitude Object
     * <p>
     * Note: an IllegalArgumentException will be thrown in the case the Latitude
     * or Longitude objects are null.
     *
     * @param lat the Latitude value in the Decimal Degree Coordinate
     * @param lon the longitude value in the Decimal Degree Coordinate
     */
    public DmsCoordinate(Latitude lat, Longitude lon) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Latitude and Longitude must be non null");
        }
        this.latitude = lat;
        this.longitude = lon;
    }

    /**
     * Constructor which converts a String in DMS format to a Latitude/Longitude paise.  The String format is
     * DDMMSS[sss]HDDMMSS[sss]H where (latitude is first):<br/>
     * <p>
     * DD is Degrees from 00 to 90 (must always be 2 characters long) representing the latitude
     * DDD is Degrees from 000 to 180 (must always be 3 characters long) representing longitude<br/>
     * MM is Minutes from 00 to 59 (must always be 2 characters long)<br/>
     * SS is Seconds from 00 to 59 (must always be 2 characters long)<br/>
     * sss is optional centiseconds from 000 to 999 (must either not appear or be 3 characters long)<br/>
     * H the North, South, East or West Hemisphere representing by the 'N', 'S;, 'E' or 'W' characters respectively
     * <p>
     * <p>
     * Example of String without centiseconds: 224530N0550230E (approx 22.75 degrees north and 55.40 degrees east)<br/>
     *
     * @param lon the DMS String representation of the Latitude Longitude pair
     * @throws GeoFormatException if the DMS String is not in the valid format and cannot be parsed
     */
    public DmsCoordinate(String dmsString) throws GeoFormatException {
        try {
            int index = Math.max(dmsString.indexOf('N'), dmsString.indexOf('S'));
            if (index < 0) {
                throw new GeoFormatException(
                        "Degrees, Minutes, Seconds are formatted incorrectly: " + dmsString);
            }
            latitude = new Latitude(dmsString.substring(0, ++index));
            longitude = new Longitude(dmsString.substring(index));
        } catch (Exception e) {
            throw new GeoFormatException(
                    "Degrees, Minutes, Seconds are formatted incorrectly: " + dmsString, e);
        }
    }

    /**
     * Gets the Latitude Object for this Decimal Degree Coordinate
     *
     * @return the Latitude Object
     */
    public Latitude getLatitude() {
        return latitude;
    }

    /**
     * Gets the Longitude Object for this Decimal Degree Coordinate
     *
     * @return the Longitude Object
     */
    public Longitude getLongitude() {
        return longitude;
    }

}
