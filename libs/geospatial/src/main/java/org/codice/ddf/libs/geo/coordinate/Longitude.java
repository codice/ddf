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
 * Class which represents a Longitude geo coordinate value
 */
public class Longitude extends Angle {

    private Hemisphere ew;

    /**
     * Constructor which converts a String in DMS format to a Longitude value.  The String format is
     * DDDMMSS[sss]H where:<br/>
     * <p>
     * DDD is Degrees from 000 to 180 (must always be 3 characters long)<br/>
     * MM is Minutes from 00 to 59 (must always be 2 characters long)<br/>
     * SS is Seconds from 00 to 59 (must always be 2 characters long)<br/>
     * sss is optional centiseconds from 000 to 999 (must either not appear or be 3 characters long)<br/>
     * H the East or West Hemisphere representing by the 'E' or 'W' characters respectively
     * <p>
     * <p>
     * Example of String without centiseconds: 0550230E (55 Degrees, 2 minutes and 30 seconds East)<br/>
     * Example of String with centiseconds: 0550230789E (55 Degrees, 2 minutes and 30 seconds and 789 centiseconds East)
     *
     * @param lon the DMS String represention of the Longitude
     * @throws GeoFormatException if the longitude String is not in the valid format and cannot be parsed
     */
    public Longitude(String lon) throws GeoFormatException {
        if (!(lon.length() != 8 || lon.length() != 11) || !(lon.endsWith(Hemisphere.E.toString())
                || lon.endsWith(Hemisphere.W.toString()))) {
            throw new GeoFormatException("Longitude is not formatted correctly should be DDDMMSSH");
        }
        int dd = Integer.parseInt(lon.substring(0, 3));
        int mm = Integer.parseInt(lon.substring(3, 5));
        int ss = Integer.parseInt(lon.substring(5, 7));
        int cc = lon.length() == 10 ? Integer.parseInt(lon.substring(7, 10)) : 0;
        Hemisphere hemi = lon.endsWith(Hemisphere.E.toString()) ? Hemisphere.E : Hemisphere.W;

        init(dd, mm, ss, cc, hemi);
    }

    /**
     * Constructor for a Longitude Coordinate value using Decimal Degrees
     * <p>
     * Note: an IllegalArgumentException will be thrown if the degree value is not in the valid ranges
     *
     * @param degrees the decimal degrees longitude value
     */
    public Longitude(double degrees) {
        super(degrees);
        if (degrees < -180.0 || degrees > 180.0) {
            throw new IllegalArgumentException("Legal ranges: latitude [-180,180]");
        }
        calculateDMS(degrees);
        ew = degrees < 0 ? Hemisphere.W : Hemisphere.E;
    }

    /**
     * Constructor for a Longitude Coordinate value using Degrees Minutes second values
     * <p>
     * Note: an IllegalArgumentException will be thrown if any of the values are not in the valid ranges
     *
     * @param degrees      DMS degree value with valid values of 0 to 180
     * @param minutes      DMS minute value with valid values of 0 to 59
     * @param seconds      DMS second value with valid values of 0 to 59
     * @param centiseconds DMS centisecond value with value values of 0 to 999
     * @param ew           the Hemisphere for the coordinate (East or West)
     */
    public Longitude(int degrees, int minutes, int seconds, int centiseconds, Hemisphere ew) {
        init(degrees, minutes, seconds, centiseconds, ew);
    }

    /**
     * Gets the E or W Hemisphere value (East or West)
     *
     * @return returns the E or W (East or West) Hemisphere
     */
    public Hemisphere getHemisphere() {
        return ew;
    }

    private void init(int degrees, int minutes, int seconds, int centiseconds, Hemisphere ew) {
        calculateDegrees(degrees,
                minutes,
                seconds,
                centiseconds,
                (Hemisphere.E.equals(ew) ? true : false),
                true);
        this.degree = degrees;
        this.minute = minutes;
        this.second = seconds;
        this.centisecond = centiseconds;
        this.ew = ew;
    }

    /**
     * Enumeration for the E (East) and W (West) Hemispheres
     */
    public enum Hemisphere {
        E, W
    }

}
