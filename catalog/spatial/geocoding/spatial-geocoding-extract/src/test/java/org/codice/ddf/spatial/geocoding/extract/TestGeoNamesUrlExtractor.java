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
 **/

package org.codice.ddf.spatial.geocoding.extract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.codice.ddf.spatial.geocoding.create.GeoNamesCreator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;

public class TestGeoNamesUrlExtractor extends TestBase {

    GeoNamesUrlExtractor geoNamesRemoteDownloader;

    @Before
    public void setUp() {

        geoNamesRemoteDownloader = Mockito.spy(new GeoNamesUrlExtractor());
        geoNamesRemoteDownloader.setGeoEntryCreator(new GeoNamesCreator());
        geoNamesRemoteDownloader.setUrl("http://localhost");
    }

    private void verifyGeoEntryList(final List<GeoEntry> geoEntryList) {

        assertEquals(3, geoEntryList.size());

        verifyGeoEntry(geoEntryList.get(0), "Kingman", 35.18944, -114.05301, "PPLA2", 28068,
                "IGM,Kingman,Kingmen,Kingmun");
        verifyGeoEntry(geoEntryList.get(1), "Lake Havasu City", 34.4839, -114.32245, "PPL", 52527,
                "HII,Lejk Khavasu Siti,Lejk-Gavasu-Siti");
        verifyGeoEntry(geoEntryList.get(2), "Marana", 32.43674, -111.22538, "PPL", 34961, "MZJ");
    }

    private void setMockInputStream(String filePath) throws FileNotFoundException {

        File file;
        FileInputStream fileInputStream;
        try {
            file = new File(TestGeoNamesFileExtractor.class.getResource(filePath).getPath());
            fileInputStream = new FileInputStream(file);
        } catch (NullPointerException e) {
            fileInputStream = null;
        }

        doReturn(fileInputStream).when(geoNamesRemoteDownloader)
                .getInputStreamFromClient(any(WebClient.class));

    }

    private void setMockResponseForConnection(int responseCode, int length) {

        Response mockResponse = mock(Response.class);
        doReturn(responseCode).when(mockResponse).getStatus();
        doReturn(length).when(mockResponse).getLength();
        doReturn(mockResponse).when(geoNamesRemoteDownloader).createConnection(anyString());
        doNothing().when(geoNamesRemoteDownloader).closeConnection();
    }

    private ByteSource getByteSourceFromFile(String fileName) throws IOException {

        File file;
        FileInputStream fileInputStream;
        try {
            file = new File(TestGeoNamesFileExtractor.class.getResource(fileName).getPath());
            fileInputStream = new FileInputStream(file);
        } catch (NullPointerException e) {
            fileInputStream = null;
        }

        FileBackedOutputStream fileBackedOutputStream;

        if (fileInputStream != null) {
            fileBackedOutputStream = new FileBackedOutputStream(4096);
            int bytesRead = -1;
            byte[] buffer = new byte[4096];

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                fileBackedOutputStream.write(buffer, 0, bytesRead);
            }
            return fileBackedOutputStream.asByteSource();
        } else {
            return null;
        }
    }

    @Test
    public void testGetGEoEntriesValidZip() throws IOException {

        setMockResponseForConnection(200, 12345);
        setMockInputStream("/geonames/valid_zip.zip");
        final List<GeoEntry> geoEntryList = geoNamesRemoteDownloader
                .getGeoEntries("valid_zip", mock(ProgressCallback.class));
        verifyGeoEntryList(geoEntryList);
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testInvalidFileExtension() throws IOException {

        setMockResponseForConnection(404, 12345);
        setMockInputStream("/geonames/valid.txt");
        geoNamesRemoteDownloader.getGeoEntries("valid.txt", mock(ProgressCallback.class));
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testNonExistentFile() throws IOException {

        setMockResponseForConnection(404, 12345);
        setMockInputStream("/geonames/nonexistentfile.txt");
        geoNamesRemoteDownloader.getGeoEntries("nonexistentfile", mock(ProgressCallback.class));
    }

    @Test
    public void testAllKeyword() throws IOException {

        setMockResponseForConnection(404, 12345);
        setMockInputStream("/geonames/nonexistentfile.txt");
        try {
            geoNamesRemoteDownloader.getGeoEntries("all", mock(ProgressCallback.class));

        } catch (GeoNamesRemoteDownloadException e) {
            assertTrue(e.toString().contains("allCountries.zip"));
        }
    }

    @Test
    public void testUnzipStreamValidFile() throws IOException {

        ByteSource byteSource = getByteSourceFromFile("/geonames/valid_zip.zip");
        geoNamesRemoteDownloader.unzipFileByteSource(byteSource);
    }

    @Test(expected = GeoEntryExtractionException.class)
    public void testUnzipStreamInvalidFile() throws IOException {

        ByteSource byteSource = getByteSourceFromFile("/geonames/nofile.zip");
        geoNamesRemoteDownloader.unzipFileByteSource(byteSource);

    }

}
