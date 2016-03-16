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
 * Abstract class that is the base for Latitude and Longitude coordinate values
 */
abstract class Angle {

    protected double decimalDegrees;

    protected int degree;

    protected int minute;

    protected int second;

    protected int centisecond;

    /**
     * Constructor which sets value to 0
     */
    public Angle() {
        this(0);
    }

    /**
     * Constructor for Decimal Degree Coordinate values
     *
     * @param decimalDegrees the Decimal Degree Coordinate value
     */
    public Angle(double decimalDegrees) {
        calculateDMS(decimalDegrees);
        this.decimalDegrees = decimalDegrees;
    }

    /**
     * Calculates the DMS value for the Decimal Degree Coordinate value
     *
     * @param decimalDegrees the Decimal Degree value
     */
    protected void calculateDMS(double decimalDegrees) {
        degree = Math.abs((int) decimalDegrees);

        minute = (int) ((Math.abs(decimalDegrees) - degree) * 60);

        second = (int) ((((Math.abs(decimalDegrees) - degree) * 60) - minute) * 60);

        centisecond = (int) (Math.min(99,
                Math.round((((((Math.abs(decimalDegrees) - degree) * 60) - minute) * 60) - second)
                        * 100)));
    }

    /**
     * Calculates the Decimal Degree value for the DMS values
     * <p>
     * Note: an IllegalArgumentException will be thrown if any of the values are not in the valid ranges
     *
     * @param degrees      the DMS Degree value
     * @param minutes      the DMS Minute value
     * @param seconds      the DMS Second value
     * @param centiseconds the DMS Centisecond value
     * @param isPositive   boolean value stating whether the coordinate value is positive or negative
     * @param isLongitude  boolean value stating whether the coordinate value is for Longitude (used in coordinate validation)
     */
    protected void calculateDegrees(int degrees, int minutes, int seconds, int centiseconds,
            boolean isPositive, boolean isLongitude) {
        if (isLongitude) {
            if (degrees < 0 || degrees > 180) {
                throw new IllegalArgumentException("Degree value is invalid; valid values [0,180]");
            }
        } else {
            if (degrees < 0 || degrees > 90) {
                throw new IllegalArgumentException("Degree value is invalid; valid values [0,90]");
            }
        }
        if (minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Minutes value is invalid; valid values [0,59]");
        }
        if (seconds < 0 || seconds > 59) {
            throw new IllegalArgumentException("Seconds value is invalid; valid values [0,59]");
        }
        if (centiseconds < 0 || centiseconds > 999) {
            throw new IllegalArgumentException("Centiseconds value is invalid; valid values [0,999]");
        }
        decimalDegrees = degrees + minutes / 60.0 + seconds / 3600.0 + centiseconds / 360000.0;
        if (!isPositive) {
            decimalDegrees = (decimalDegrees * -1);
        }
    }

    /**
     * @return the Decimal degree value for the coordinate
     */
    public double getDecimalDegrees() {
        return decimalDegrees;
    }

    /**
     * @return the DMS degree for the coordinate
     */
    public int getDegree() {
        return degree;
    }

    /**
     * @return the DMS minute for the coordinate
     */
    public int getMinute() {
        return minute;
    }

    /**
     * @return the DMS second for the coordinate
     */
    public int getSecond() {
        return second;
    }

    /**
     * @return the DMS centiseconds for the coordinate
     */
    public int getCentiseconds() {
        return centisecond;
    }
}
