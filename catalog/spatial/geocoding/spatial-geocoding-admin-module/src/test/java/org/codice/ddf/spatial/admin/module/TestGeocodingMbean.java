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

package org.codice.ddf.spatial.admin.module;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.codice.ddf.spatial.admin.module.service.Geocoding;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestGeocodingMbean {

    private Geocoding geocoding;

    private GeoEntryExtractor geoEntryExtractor;

    private GeoEntryIndexer geoEntryIndexer;

    private static final String PATH = System.getProperty("user.dir") + "/src/test/resources/";

    private static final String CONTENT_FILE = "0000-0000-0000-0000";

    private static final String TRUE = "true";

    private static final String FALSE = "false";

    private static final String URL = "http://example.com";

    @Before
    public void setUp()  {
        geocoding = new Geocoding();
        geoEntryExtractor = Mockito.mock(GeoEntryExtractor.class);
        geoEntryIndexer = Mockito.mock(GeoEntryIndexer.class);
        geocoding.setGeoEntryIndexer(geoEntryIndexer);
        geocoding.setGeoEntryExtractor(geoEntryExtractor);

    }

    @Test
    public void testGettersAndSetters() {
        assertThat(0, equalTo(geocoding.progressCallback()));
        geocoding.setProgress(50);
        assertThat(50, equalTo(geocoding.progressCallback()));
        assertThat(geoEntryIndexer, equalTo(geocoding.getGeoEntryIndexer()));
        assertThat(geoEntryExtractor, equalTo(geocoding.getGeoEntryExtractor()));

    }

    @Test
    public void testUpdateGeoIndexWithUrl() {
        assertTrue(geocoding.updateGeoIndexWithUrl(URL, TRUE));
    }

    @Test
    public void testUpdateGeoIndexWithFilePath() {
        assertTrue(geocoding.updateGeoIndexWithFilePath(PATH, CONTENT_FILE, FALSE));
    }

    @Test
    public void testUpdateIndexExtractionException() throws GeoEntryIndexingException, GeoEntryExtractionException,
            GeoNamesRemoteDownloadException {
        Mockito.doThrow(new GeoEntryExtractionException("Error Extracting")).when(geoEntryIndexer)
                .updateIndex(Mockito.anyString(), Mockito.any(GeoEntryExtractor.class),
                        Mockito.anyBoolean(), Mockito.any(ProgressCallback.class));
        try {
            assertFalse(geocoding.updateGeoIndexWithFilePath(PATH, CONTENT_FILE, TRUE));
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("Error Extracting"));
        }
    }

    @Test
    public void testUpdateIndexRemoteDownloadException() throws GeoEntryIndexingException, GeoEntryExtractionException,
            GeoNamesRemoteDownloadException {
        Mockito.doThrow(new GeoNamesRemoteDownloadException("Error Downloading")).when(geoEntryIndexer)
                .updateIndex(Mockito.anyString(), Mockito.any(GeoEntryExtractor.class),
                        Mockito.anyBoolean(), Mockito.any(ProgressCallback.class));
        try {
            assertFalse(geocoding.updateGeoIndexWithFilePath(PATH, CONTENT_FILE, TRUE));
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("Error Downloading"));
        }
    }

    @Test
    public void testUpdateIndexIndexingException() throws GeoEntryIndexingException, GeoEntryExtractionException,
            GeoNamesRemoteDownloadException {
        Mockito.doThrow(new GeoEntryIndexingException("Error Indexing")).when(geoEntryIndexer)
                .updateIndex(Mockito.anyString(), Mockito.any(GeoEntryExtractor.class),
                        Mockito.anyBoolean(), Mockito.any(ProgressCallback.class));
        try {
            assertFalse(geocoding.updateGeoIndexWithFilePath(PATH, CONTENT_FILE, TRUE));
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("Error Indexing"));
        }
    }
}
