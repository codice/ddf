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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.codice.ddf.spatial.geocoding.create.GeoNamesCreator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;


public class TestGeoNamesFileExtractor extends TestBase {
    private static final String VALID_TEXT_FILE_PATH =
            TestGeoNamesFileExtractor.class.getResource("/geonames/valid.txt").getPath();

    private static final String INVALID_TEXT_FILE_PATH =
            TestGeoNamesFileExtractor.class.getResource("/geonames/invalid.txt").getPath();

    private static final String VALID_ZIP_FILE_PATH =
            TestGeoNamesFileExtractor.class.getResource("/geonames/valid_zip.zip").getPath();

    private static final String UNSUPPORTED_FILE_PATH =
            TestGeoNamesFileExtractor.class.getResource("/geonames/foo.rtf").getPath();

    private GeoNamesFileExtractor geoNamesFileExtractor;

    private static final String URL = "http://example.com";

    @Before
    public void setUp() {
        geoNamesFileExtractor = Mockito.spy(new GeoNamesFileExtractor());
        geoNamesFileExtractor.setGeoEntryCreator(new GeoNamesCreator());
        geoNamesFileExtractor.setUrl(URL);

    }

    private void verifyGeoEntryList(final List<GeoEntry> geoEntryList) {
        assertEquals(3, geoEntryList.size());

        verifyGeoEntry(geoEntryList.get(0), "Kingman", 35.18944, -114.05301, "PPLA2", 28068,
                "IGM,Kingman,Kingmen,Kingmun");
        verifyGeoEntry(geoEntryList.get(1), "Lake Havasu City", 34.4839, -114.32245, "PPL", 52527,
                "HII,Lejk Khavasu Siti,Lejk-Gavasu-Siti");
        verifyGeoEntry(geoEntryList.get(2), "Marana", 32.43674, -111.22538, "PPL", 34961, "MZJ");
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

        geoNamesFileExtractor.pushGeoEntriesToExtractionCallback(fileLocation, extractionCallback);

        verify(extractionCallback, atLeastOnce()).updateProgress(anyInt());
        verify(extractionCallback, times(3)).extracted(geoEntryArgumentCaptor.capture());

        final List<GeoEntry> capturedGeoEntryList = geoEntryArgumentCaptor.getAllValues();

        verifyGeoEntryList(capturedGeoEntryList);
    }

    @Test
    public void testExtractFromValidTextFileAllAtOnce() {
        testFileExtractionAllAtOnce(VALID_TEXT_FILE_PATH, mock(ProgressCallback.class));
    }

    @Test
    public void testExtractFromValidTextFileStreaming() {
        testFileExtractionStreaming(VALID_TEXT_FILE_PATH);
    }

    @Test
    public void testExtractFromValidZipFileAllAtOnce() {
        testFileExtractionAllAtOnce(VALID_ZIP_FILE_PATH, null);
        // Delete the extracted text file.
        FileUtils.deleteQuietly(
                new File(FilenameUtils.removeExtension(VALID_ZIP_FILE_PATH) + ".txt"));
    }

    @Test
    public void testExtractFromValidZipFileStreaming() {
        testFileExtractionStreaming(VALID_ZIP_FILE_PATH);
        // Delete the extracted text file.
        FileUtils.deleteQuietly(
                new File(FilenameUtils.removeExtension(VALID_ZIP_FILE_PATH) + ".txt"));
    }

    @Test
    public void testExtractFromTextFileWrongFormat() {
        try {
            geoNamesFileExtractor.getGeoEntries(INVALID_TEXT_FILE_PATH, null);
            fail("Should have thrown a GeoEntryExtractionException because 'invalid.txt' is not " +
                    "formatted in the expected way.");
        } catch (GeoEntryExtractionException e) {
            assertThat(e.getCause(), instanceOf(IndexOutOfBoundsException.class));
        }
    }

    @Test
    public void testExtractFromNonexistentFile() {
        try {
            geoNamesFileExtractor.getGeoEntries(
                    FilenameUtils.getFullPath(VALID_TEXT_FILE_PATH) + "fake.txt", null);
            fail("Should have thrown a GeoEntryExtractionException because 'fake.txt' does not " +
                    "exist.");
        } catch (GeoEntryExtractionException e) {
            assertThat(e.getCause(), instanceOf(FileNotFoundException.class));
        }
    }

    @Test(expected = GeoEntryExtractionException.class)
    public void testExtractFromUnsupportedFileType() {
        geoNamesFileExtractor.getGeoEntries(UNSUPPORTED_FILE_PATH, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullExtractionCallback() {
        geoNamesFileExtractor.pushGeoEntriesToExtractionCallback(VALID_TEXT_FILE_PATH, null);
    }

    @Test
    public void testG() throws IOException {
        setMockConnection(200, 12345);
        setMockInputStream("/geonames/AU.zip");
        testFileExtractionStreaming("AU");

    }

    @Test(expected = GeoEntryExtractionException.class)
    public void testInvalidFileExtensionInUrl() throws IOException {
        setMockConnection(404, 12345);
        setMockInputStream("/geonames/valid.txt");
        geoNamesFileExtractor.getGeoEntries("valid", mock(ProgressCallback.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoEntriesStreamingNullCallback() throws IOException {
        setMockConnection(404, 12345);
        setMockInputStream("/geonames/nonexistentfile.txt");
        geoNamesFileExtractor.pushGeoEntriesToExtractionCallback("nonexistentfile.txt", null);
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testNonExistentFile() throws IOException {
        setMockConnection(404, 12345);
        setMockInputStream("/geonames/s.txt");
        geoNamesFileExtractor.getGeoEntries("s", mock(ProgressCallback.class));
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testBadStreamForFile() throws IOException {
        setMockConnection(404, 12345);
        doReturn(null).when(geoNamesFileExtractor)
                .getUrlInputStreamFromWebClient();
        geoNamesFileExtractor.getGeoEntries("valid", mock(ProgressCallback.class));
    }

    private void setMockInputStream(String filePath) throws FileNotFoundException {
        FileInputStream fileInputStream = getFileInputStream(filePath);
        doReturn(fileInputStream).when(geoNamesFileExtractor).getUrlInputStreamFromWebClient();
    }

    private void setMockConnection(int responseCode, int length) {
        Response mockResponse = mock(Response.class);
        doReturn(responseCode).when(mockResponse).getStatus();
        doReturn(length).when(mockResponse).getLength();
        doReturn(mockResponse).when(geoNamesFileExtractor).createConnection(anyString());
        doNothing().when(geoNamesFileExtractor).closeConnection();
    }

    private FileInputStream getFileInputStream(String fileName) {
        File file;
        FileInputStream fileInputStream;
        try {
            file = new File(TestGeoNamesFileExtractor.class.getResource(fileName).getPath());
            fileInputStream = new FileInputStream(file);
        } catch (NullPointerException | FileNotFoundException e) {
            fileInputStream = null;
        }
        return fileInputStream;
    }
}
