/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.catalog.ui.query.geofeature;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.geotools.geometry.jts.spatialschema.geometry.DirectPositionImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.junit.Before;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;

public class GazetteerFeatureServiceTest {
    private static final GeoEntry GEO_ENTRY_1 = new GeoEntry.Builder().name("Philadelphia")
            .latitude(40)
            .longitude(-71)
            .featureCode("PPL")
            .population(1000000)
            .alternateNames("")
            .build();

    private static final GeoEntry GEO_ENTRY_2 = new GeoEntry.Builder().name("Camden")
            .latitude(40)
            .longitude(-70)
            .featureCode("PPLC")
            .population(10000000)
            .alternateNames("")
            .build();

    private static final List<GeoEntry> QUERYABLE_RESULTS = Arrays.asList(GEO_ENTRY_1, GEO_ENTRY_2);

    private static final String TEST_QUERY = "example";

    private GazetteerFeatureService gazetteerFeatureService;

    private GeoEntryQueryable geoEntryQueryable;

    @Before
    public void setUp() {
        geoEntryQueryable = mock(GeoEntryQueryable.class);
        gazetteerFeatureService = new GazetteerFeatureService();
        gazetteerFeatureService.setGeoEntryQueryable(geoEntryQueryable);
    }

    @Test
    public void testGetSuggestedFeatureNames() throws GeoEntryQueryException {
        final int maxResults = 2;
        doReturn(QUERYABLE_RESULTS).when(geoEntryQueryable)
                .query(TEST_QUERY, maxResults);

        List<String> results = gazetteerFeatureService.getSuggestedFeatureNames(TEST_QUERY,
                maxResults);
        assertThat(results, contains(GEO_ENTRY_1.getName(), GEO_ENTRY_2.getName()));
    }

    @Test
    public void testGetFeatureByName() throws GeoEntryQueryException {
        final double north = 1;
        final double south = -2;
        final double east = 3;
        final double west = -4;
        GeoResult geoResult = new GeoResult();
        geoResult.setFullName(GEO_ENTRY_1.getName());
        final DirectPosition northWest = new DirectPositionImpl(west, north);
        final DirectPosition southEast = new DirectPositionImpl(east, south);
        geoResult.setBbox(Arrays.asList(new PointImpl(northWest), new PointImpl(southEast)));

        gazetteerFeatureService = new GazetteerFeatureService() {
            @Override
            protected GeoResult getGeoResultFromGeoEntry(GeoEntry entry) {
                return geoResult;
            }
        };
        gazetteerFeatureService.setGeoEntryQueryable(geoEntryQueryable);

        doReturn(QUERYABLE_RESULTS).when(geoEntryQueryable)
                .query(TEST_QUERY, 1);

        BoundingBoxFeature feature = (BoundingBoxFeature) gazetteerFeatureService.getFeatureByName(
                TEST_QUERY);
        assertThat(feature.getName(), is(geoResult.getFullName()));
        assertThat(feature.getNorth(), is(north));
        assertThat(feature.getSouth(), is(south));
        assertThat(feature.getEast(), is(east));
        assertThat(feature.getWest(), is(west));
    }
}