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
 * Class which represents a Latitude geo coordinate value
 */
public class Latitude extends Angle {

    private Hemisphere ns;

    /**
     * Constructor which converts a String in DMS format to a Latitude value.  The String format is
     * DDMMSS[sss]H where:<br/>
     * <p>
     * DD is Degrees from 000 to 90 (must always be 2 characters long)<br/>
     * MM is Minutes from 00 to 59 (must always be 2 characters long)<br/>
     * SS is Seconds from 00 to 59 (must always be 2 characters long)<br/>
     * sss is optional centiseconds from 000 to 999 (must either not appear or be 3 characters long)<br/>
     * H the North or South Hemisphere representing by the 'N' or 'S' characters respectively
     * <p>
     * <p>
     * Example of String without centiseconds: 0550230E (55 Degrees, 2 minutes and 30 seconds North)<br/>
     * Example of String with centiseconds: 0550230789E (55 Degrees, 2 minutes and 30 seconds and 789 centiseconds North)
     *
     * @param lat the DMS String representation of the Longitude
     * @throws GeoFormatException if the longitude String is not in the valid format and cannot be parsed
     */
    public Latitude(String lat) throws GeoFormatException {
        if (!(lat.length() != 7 || lat.length() != 10) || !(lat.endsWith("N")
                || lat.endsWith("S"))) {
            throw new GeoFormatException("Latitude is not formatted correctly should be DDMMSSH");
        }
        int dd = Integer.parseInt(lat.substring(0, 2));
        int mm = Integer.parseInt(lat.substring(2, 4));
        int ss = Integer.parseInt(lat.substring(4, 6));
        int cc = lat.length() == 10 ? Integer.parseInt(lat.substring(6, 9)) : 0;
        Hemisphere hemi = lat.endsWith("N") ? Hemisphere.N : Hemisphere.S;

        init(dd, mm, ss, cc, hemi);
    }

    /**
     * Constructor for a Latitude Coordinate value using Decimal Degrees
     * <p>
     * Note: an IllegalArgumentException will be thrown if the degree value is not in the valid ranges
     *
     * @param degrees the decimal degrees longitude value
     */
    public Latitude(double degrees) {
        super(degrees);
        if (degrees < -90.0 || degrees > 90.0) {
            throw new IllegalArgumentException("Legal ranges: latitude [-90,90]");
        }
        calculateDMS(degrees);
        ns = degrees < 0 ? Hemisphere.S : Hemisphere.N;
    }

    /**
     * Constructor for a Latitude Coordinate value using Degrees Minutes second values
     * <p>
     * Note: an IllegalArgumentException will be thrown if any of the values are not in the valid ranges
     *
     * @param degrees      DMS degree value with valid values of 0 to 90
     * @param minutes      DMS minute value with valid values of 0 to 59
     * @param seconds      DMS second value with valid values of 0 to 59
     * @param centiseconds DMS centisecond value with value values of 0 to 999
     * @param ns           the Hemisphere for the coordinate (North or South)
     */
    public Latitude(int degrees, int minutes, int seconds, int centiseconds, Hemisphere ns)
            throws GeoFormatException {
        init(degrees, minutes, seconds, centiseconds, ns);
    }

    /**
     * Gets the N or S Hemisphere value (North or South)
     *
     * @return returns the E or W (North or South) Hemisphere
     */
    public Hemisphere getHemisphere() {
        return ns;
    }

    private void init(int degrees, int minutes, int seconds, int centiseconds, Hemisphere ns) {
        calculateDegrees(degrees,
                minutes,
                seconds,
                centiseconds,
                (Hemisphere.N.equals(ns) ? true : false),
                false);
        this.degree = degrees;
        this.minute = minutes;
        this.second = seconds;
        this.centisecond = centiseconds;
        this.ns = ns;
    }

    /**
     * Enumeration for the N (North) and S (South) Hemispheres
     */
    public enum Hemisphere {
        N, S
    }

}
