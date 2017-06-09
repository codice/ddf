package org.codice.ddf.catalog.ui.query.geofeature;

public class BoundingBoxFeature extends Feature {
    public BoundingBoxFeature() {
        this.type = "bbox";
    }

    public double getNorth() {
        return north;
    }

    public void setNorth(double north) {
        this.north = north;
    }

    public double getSouth() {
        return south;
    }

    public void setSouth(double south) {
        this.south = south;
    }

    public double getEast() {
        return east;
    }

    public void setEast(double east) {
        this.east = east;
    }

    public double getWest() {
        return west;
    }

    public void setWest(double west) {
        this.west = west;
    }

    private double north;
    private double south;
    private double east;
    private double west;
}
