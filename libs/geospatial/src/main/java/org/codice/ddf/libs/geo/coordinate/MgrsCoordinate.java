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
 * Coordinate that handles data specified in the MGRS format
 */
public class MgrsCoordinate implements Cloneable {

    private double x;

    private double y;

    private int zone;

    private char band;

    private char gridColID;

    private char gridRowID;

    /**
     * Constructor which converts a String in MGRS format to a MgrsCoordinate Object.  The String format is
     * ZoneBandGridEastingNorthing:<br/>
     * <p>
     * Zone is the integer zone <br/>
     * Band is the latitude band<br/>
     * Grid is the 2 character Grid (Column then Row)
     * Easting is the x Coordinate in MGRS format<br/>
     * Northing is the y Coordinate in MGRS format<br/>
     * <p>
     * Note: Northing and Easting values should have the same number of characters
     * <p>
     * <p>
     * Example of MGRS String is 38SMB4595686195
     *
     * @param mgrsString the MGRS String representation of the Coordinate
     * @throws GeoFormatException if the MGRS String is not in the valid format and cannot be parsed
     */
    public MgrsCoordinate(String mgrsString) throws GeoFormatException {
        try {
            char myBand = mgrsString.charAt(1);
            String afterBand = null;
            int myZone = 0;
            if (myBand >= 'A' && myBand <= 'Z') {
                myZone = Integer.parseInt(String.valueOf(mgrsString.charAt(0)));
                afterBand = mgrsString.substring(2);
            } else {
                myBand = mgrsString.charAt(2);
                myZone = Integer.parseInt(mgrsString.substring(0, 2));
                afterBand = mgrsString.substring(3);
            }
            char myCol = afterBand.charAt(0);
            char myRow = afterBand.charAt(1);

            String coords = afterBand.substring(2);

            int index = coords.length() / 2;
            int myX = Integer.parseInt(coords.substring(0, index));
            int myY = Integer.parseInt(coords.substring(index));
            init(myX, myY, myZone, myBand, myCol, myRow);
        } catch (Exception e) {
            throw new GeoFormatException("MGRS String is formatted incorrectly : " + mgrsString);
        }
    }

    /**
     * Construct an MGRS Geometry
     *
     * @param x     the easting value
     * @param y     the northing value
     * @param zone  the integer zone
     * @param band  the latitude band
     * @param colID the column ID character of the grid
     * @param rowID the row ID character of the grid
     */
    public MgrsCoordinate(double x, double y, int zone, char band, char colID, char rowID) {
        init(x, y, zone, band, colID, rowID);
    }

    /**
     * Returns the MGRS Zone
     *
     * @return the MGRS zone number
     */
    public int getZone() {
        return zone;
    }

    /**
     * Gets the latitude Band for the MGRS coordinate
     *
     * @return the band
     */
    public char getBand() {
        return band;
    }

    /**
     * Gets the Grid Column ID character value
     *
     * @return the columnId
     */
    public char getGridColumnID() {
        return gridColID;
    }

    /**
     * Gets the Grid Row ID character value
     *
     * @return the row ID
     */
    public char getGridRowID() {
        return gridRowID;
    }

    /**
     * Gets the X value (Easting)
     *
     * @return the x (easting) value
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the Y value (Northing)
     *
     * @return the y (northing) value
     */
    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "Zone: " + zone + " Band: " + band + " " + gridColID + gridRowID + " " + x + " " + y;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        MgrsCoordinate coord = (MgrsCoordinate) super.clone();
        coord.x = x;
        coord.y = y;
        coord.zone = zone;
        coord.band = band;
        coord.gridColID = gridColID;
        coord.gridRowID = gridRowID;
        return coord;
    }

    private void init(double x, double y, int zone, char band, char colID, char rowID) {
        this.x = x;
        this.y = y;
        this.zone = zone;
        this.band = band;
        this.gridColID = colID;
        this.gridRowID = rowID;
    }

}
