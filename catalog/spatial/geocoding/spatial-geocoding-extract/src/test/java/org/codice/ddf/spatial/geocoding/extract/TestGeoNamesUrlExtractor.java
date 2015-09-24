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

package org.codice.ddf.spatial.geocoding.extract;

import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryCreator;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.junit.Before;
import org.junit.Test;

public class TestGeoNamesUrlExtractor {

    GeoNamesUrlExtractor geoNamesRemoteDownloader;

    @Before
    public void setUp() {
        geoNamesRemoteDownloader = new GeoNamesUrlExtractor();

    }

    @Test
    public void testTemp() {

        geoNamesRemoteDownloader.setGeoEntryCreator(new GeoEntryCreator() {
            @Override
            public GeoEntry createGeoEntry(String line) {
                return null;
            }
        });
        geoNamesRemoteDownloader.getGeoEntriesStreaming("AD", new GeoEntryExtractor.ExtractionCallback() {
            @Override
            public void extracted(GeoEntry newEntry) {

            }

            @Override
            public void updateProgress(int progress) {

            }
        });

        /*

        String url = "http://localhost:8993";
        WebClient client = WebClient.create(url);
        client.path(url);


        Response response = client.get();
        */
    }


}
