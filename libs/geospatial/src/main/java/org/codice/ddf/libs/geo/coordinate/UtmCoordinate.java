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
import org.codice.ddf.libs.geo.conversion.DecimalDegreesConverter.LatZones;
import org.codice.ddf.libs.geo.conversion.UtmConverter;

/**
 * Coordinate that handles data specified in the UTM format
 */
public class UtmCoordinate implements Cloneable {

    private double northing;

    private double easting;

    private int zone;

    private Character band;

    private boolean isNorthernHemisphere;

    /**
     * Constructor to create a UTM Coordinate
     *
     * @param zone     the UTM Zone
     * @param band     the UTM band
     * @param northing the northing value
     * @param easting  the easting value
     */
    public UtmCoordinate(int zone, char band, double easting, double northing) {
        this.northing = northing;
        this.easting = easting;
        this.zone = zone;
        this.band = band;
        this.isNorthernHemisphere = isNorthernHemisphere(band);
    }

    /**
     * Constructor which converts a String in UTM format to a MgrsCoordinate Object.  The String format is
     * ZoneHemisphereEastingNorthing:<br/>
     * <p>
     * Zone is the integer zone <br/>
     * Hemisphere can either be 'N' or 'S' for Northern or southern hemisphere respectively<br/>
     * Easting is the x Coordinate in UTM format<br/>
     * Northing is the y Coordinate in UTM format<br/>
     * <p>
     * Note: Northing and Easting values should have the same number of characters
     * <p>
     * <p>
     * Example of UTM String is 38N04459563686195
     *
     * @param utmString the UTM String representation of the Coordinate
     * @throws GeoFormatException if the UTM String is not in the valid format and cannot be parsed
     */
    public UtmCoordinate(String utmString) throws GeoFormatException {
        try {
            char hemisphere = utmString.charAt(1);
            String afterBand = null;

            if (hemisphere == 'N' || hemisphere == 'S') {
                zone = Integer.parseInt(String.valueOf(utmString.charAt(0)));
                afterBand = utmString.substring(2);
            } else {
                hemisphere = utmString.charAt(2);
                zone = Integer.parseInt(utmString.substring(0, 2));
                afterBand = utmString.substring(3);
            }
            int index = afterBand.length() / 2;
            easting = Integer.parseInt(afterBand.substring(0, index));
            northing = Integer.parseInt(afterBand.substring(index));
            if (hemisphere == 'N') {
                this.isNorthernHemisphere = true;
            } else if (hemisphere == 'S') {
                this.isNorthernHemisphere = false;
            } else {
                throw new GeoFormatException(
                        "Invalid UTM String, hemisphere must be either 'N' or 'S': " + hemisphere);
            }

        } catch (GeoFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new GeoFormatException("Could not parse UTM Coordinate" + utmString, e);
        }
    }

    /**
     * Gets the UTM Integer zone
     *
     * @return the zone
     */
    public int getZone() {
        return zone;
    }

    /**
     * Gets the UTM band
     *
     * @return the band
     */
    public char getBand() {
        if (band == null) {
            band = computeBand();
        }
        return band;
    }

    private char computeBand() {
        DecimalDegreesCoordinate ddCoord = UtmConverter.convertToDecimalDegrees(this);
        LatZones zones = new LatZones();
        return zones.getLatZone(ddCoord.getLatitiudeDegrees());
    }

    /**
     * Gets the Northing value of the UTM Coordinate
     *
     * @return the northing value
     */
    public double getNorthing() {
        return northing;
    }

    /**
     * Gets the Easting value of the URM Coordinate
     *
     * @return the easting value
     */
    public double getEasting() {
        return easting;
    }

    @Override
    public String toString() {
        return "Zone: " + zone + " Hemisphere: " + (isNorthernHemisphere ? "N" : "S") + " "
                + northing + " " + easting;

    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        UtmCoordinate coord = (UtmCoordinate) super.clone();
        coord.zone = zone;
        coord.band = band;
        coord.northing = northing;
        coord.easting = easting;
        return coord;
    }

    public boolean isNorthHemisphere() {
        return isNorthernHemisphere;
    }

    /**
     * Returns true if the latitude band is in the northern hemisphere
     *
     * @return true if in the northern hemisphere, false otherwise
     */
    private boolean isNorthernHemisphere(char myBand) {
        return myBand > 'M' ? true : false;
    }

}
