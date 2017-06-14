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
package org.codice.ddf.catalog.ui.query.geofeature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A stub implementation of FeatureService for testing purposes.
 * Returns one each of point-radius, bbox, and polygon features.
 */
public class StubFeatureService implements FeatureService {
    @Override
    public List<String> getSuggestedFeatureNames(String query, int maxResults) {
        List<String> results = new ArrayList<>();
        results.add("Test City - Phoenix");
        results.add("Test Country - Cuba");
        results.add("Test Region - Middle East");
        return results;
    }

    @Override
    public Feature getFeatureByName(String name) {
        Map<String, Feature> testFeatures = new HashMap<>();

        PointRadiusFeature phoenix = new PointRadiusFeature();
        phoenix.setName("Phoenix");
        phoenix.setCenter(new Coordinate(33.44838, -112.07404));
        phoenix.setRadius(30000);
        testFeatures.put("Test City - Phoenix", phoenix);

        BoundingBoxFeature middleEast = new BoundingBoxFeature();
        middleEast.setName("Middle East");
        middleEast.setWest(23.66943501079561);
        middleEast.setSouth(10.1826011067396);
        middleEast.setEast(62.56861656036253);
        middleEast.setNorth(36.21281871581509);
        testFeatures.put("Test Region - Middle East", middleEast);

        PolygonFeature cuba = new PolygonFeature();
        cuba.setName("Cuba");
        cuba.getCoordinates()
                .add(new Coordinate(-84.45177649431972, 22.566860395584598));
        cuba.getCoordinates()
                .add(new Coordinate(-81.22049819973255, 23.151223194857025));
        cuba.getCoordinates()
                .add(new Coordinate(-78.33332630083491, 22.373993777823344));
        cuba.getCoordinates()
                .add(new Coordinate(-75.65962223615223, 20.874065153901327));
        cuba.getCoordinates()
                .add(new Coordinate(-73.96747830600287, 20.064148228217206));
        cuba.getCoordinates()
                .add(new Coordinate(-77.58581850351827, 19.820889048504203));
        cuba.getCoordinates()
                .add(new Coordinate(-77.14552576535779, 20.59720084940181));
        cuba.getCoordinates()
                .add(new Coordinate(-78.64729534854865, 21.020438856136817));
        cuba.getCoordinates()
                .add(new Coordinate(-78.68050097510336, 21.64255439065588));
        cuba.getCoordinates()
                .add(new Coordinate(-81.8530819011617, 21.992616168072885));
        cuba.getCoordinates()
                .add(new Coordinate(-81.74794632922017, 22.490754797413675));
        cuba.getCoordinates()
                .add(new Coordinate(-84.52024070939801, 21.799147967566434));
        cuba.getCoordinates()
                .add(new Coordinate(-84.45177649431972, 22.566860395584598));
        testFeatures.put("Test Country - Cuba", cuba);

        return testFeatures.get(name);
    }
}
