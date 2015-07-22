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

import static org.codice.ddf.spatial.geocoding.GeoEntryExtractor.ExtractionCallback;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.codice.ddf.spatial.geocoding.create.GeoNamesCreator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TestGeoNamesFileExtractor extends TestBase {
    private static final String ABSOLUTE_PATH = new File(".").getAbsolutePath();

    private static final String TEST_PATH = "/src/test/resources/geonames/";

    private GeoNamesFileExtractor geoNamesFileExtractor;

    @Before
    public void setUp() {
        geoNamesFileExtractor = new GeoNamesFileExtractor();
        geoNamesFileExtractor.setGeoEntryCreator(new GeoNamesCreator());
    }

    private void verifyGeoEntryList(final List<GeoEntry> geoEntryList) {
        assertEquals(3, geoEntryList.size());

        verifyGeoEntry(geoEntryList.get(0), "Kingman", 35.18944, -114.05301, "PPLA2", 28068);
        verifyGeoEntry(geoEntryList.get(1), "Lake Havasu City", 34.4839, -114.32245, "PPL", 52527);
        verifyGeoEntry(geoEntryList.get(2), "Marana", 32.43674, -111.22538, "PPL", 34961);
    }

    private void testFileExtractionAllAtOnce(final String fileLocation,
            final ProgressCallback mockProgressCallback) {
        final List<GeoEntry> geoEntryList =
                geoNamesFileExtractor.getGeoEntries(fileLocation, mockProgressCallback);

        if (mockProgressCallback != null) {
            verify(mockProgressCallback, atLeastOnce()).updateProgress(anyInt());
        }

        verifyGeoEntryList(geoEntryList);
    }

    private void testFileExtractionStreaming(final String fileLocation) {
        final ExtractionCallback extractionCallback = mock(ExtractionCallback.class);

        final ArgumentCaptor<GeoEntry> geoEntryArgumentCaptor =
                ArgumentCaptor.forClass(GeoEntry.class);

        geoNamesFileExtractor.getGeoEntriesStreaming(fileLocation, extractionCallback);

        verify(extractionCallback, atLeastOnce()).updateProgress(anyInt());
        verify(extractionCallback, times(3)).extracted(geoEntryArgumentCaptor.capture());

        final List<GeoEntry> capturedGeoEntryList = geoEntryArgumentCaptor.getAllValues();

        verifyGeoEntryList(capturedGeoEntryList);
    }

    @Test
    public void testExtractFromValidTextFileAllAtOnce() {
        testFileExtractionAllAtOnce(ABSOLUTE_PATH + TEST_PATH + "valid.txt",
                mock(ProgressCallback.class));
    }

    @Test
    public void testExtractFromValidTextFileStreaming() {
        testFileExtractionStreaming(ABSOLUTE_PATH + TEST_PATH + "valid.txt");
    }

    @Test
    public void testExtractFromValidZipFileAllAtOnce() {
        testFileExtractionAllAtOnce(ABSOLUTE_PATH + TEST_PATH + "valid_zip.zip", null);
        // Delete the extracted text file.
        FileUtils.deleteQuietly(new File(ABSOLUTE_PATH + TEST_PATH + "valid_zip.txt"));
    }

    @Test
    public void testExtractFromValidZipFileStreaming() {
        testFileExtractionStreaming(ABSOLUTE_PATH + TEST_PATH + "valid_zip.zip");
        // Delete the extracted text file.
        FileUtils.deleteQuietly(new File(ABSOLUTE_PATH + TEST_PATH + "valid_zip.txt"));
    }

    @Test
    public void testExtractFromTextFileWrongFormat() {
        try {
            geoNamesFileExtractor.getGeoEntries(ABSOLUTE_PATH + TEST_PATH + "invalid.txt", null);
            fail("Should have thrown a GeoEntryExtractionException because 'invalid.txt' is not " +
                    "formatted in the expected way.");
        } catch (GeoEntryExtractionException e) {
            assertThat(e.getCause(), instanceOf(IndexOutOfBoundsException.class));
        }
    }

    @Test
    public void testExtractFromNonexistentFile() {
        try {
            geoNamesFileExtractor.getGeoEntries(ABSOLUTE_PATH + TEST_PATH + "fake.txt", null);
            fail("Should have thrown a GeoEntryExtractionException because 'fake.txt' does not " +
                    "exist.");
        } catch (GeoEntryExtractionException e) {
            assertThat(e.getCause(), instanceOf(FileNotFoundException.class));
        }
    }

    @Test
    public void testExtractFromUnsupportedFileType() {
        try {
            geoNamesFileExtractor.getGeoEntries(ABSOLUTE_PATH + TEST_PATH + "fake.tar", null);
            fail("Should have thrown a GeoEntryExtractionException because 'fake.tar' is not a " +
                    ".txt or a .zip.");
        } catch (GeoEntryExtractionException e) {
            assertEquals("Input must be a .txt or a .zip.", e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullExtractionCallback() {
        geoNamesFileExtractor.getGeoEntriesStreaming(ABSOLUTE_PATH + TEST_PATH + "fake.txt", null);
    }
}
