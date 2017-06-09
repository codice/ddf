package org.codice.ddf.catalog.ui.query.geofeature;

public class PointRadiusFeature extends Feature {
    public PointRadiusFeature() {
        this.type = "point-radius";
    }

    public Coordinate getCenter() {
        return center;
    }

    public void setCenter(Coordinate center) {
        this.center = center;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    private Coordinate center;
    private double radius;
}
