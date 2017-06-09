package org.codice.ddf.catalog.ui.query.geofeature;

import java.util.ArrayList;
import java.util.List;

public class PolygonFeature extends Feature {
    public PolygonFeature() {
        this.type = "polygon";
        this.coordinates = new ArrayList<>();
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    private List<Coordinate> coordinates;
}
