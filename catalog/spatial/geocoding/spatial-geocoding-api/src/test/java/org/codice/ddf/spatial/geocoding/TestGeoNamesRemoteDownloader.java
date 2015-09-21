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

package org.codice.ddf.spatial.geocoding;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;

public class TestGeoNamesRemoteDownloader {

    private static final String MALFORMED_URL_1 =
            "http://malformedurl/";

    private static final String MALFORMED_URL_2 =
            "http://";

    private static final String VALID_URL =
            "http://download.geonames.org/export/dump/";

    private static final String GEONAMES_ZIP_FILE =
            "AD.zip";

    private static final String GEONAMES_TXT_FILE =
            "admin1CodesASCII.txt";

    GeoNamesRemoteDownloader geoNamesRemoteDownloader;

    @Before
    public void setUp() {
        geoNamesRemoteDownloader = new GeoNamesRemoteDownloader();
        geoNamesRemoteDownloader.setIndexLocation(new File(".").getAbsolutePath());
    }

    @Test
    public void testExtractZipFromValidUrlWithProgressCallback() {

        final ProgressCallback mockProgressCallback = mock(ProgressCallback.class);
        String resultPath = geoNamesRemoteDownloader.getResourceFromUrl(VALID_URL + GEONAMES_ZIP_FILE,
                mockProgressCallback);
        geoNamesRemoteDownloader.deleteDownloadedFile(resultPath);

    }

    @Test
    public void testExtractZipFromValidUrl() {
        String resultPath = geoNamesRemoteDownloader.getResourceFromUrl(VALID_URL + GEONAMES_ZIP_FILE, null);
        geoNamesRemoteDownloader.deleteDownloadedFile(resultPath);
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testExtractTxtFromValidUrl() {
        geoNamesRemoteDownloader.getResourceFromUrl(VALID_URL + GEONAMES_TXT_FILE, null);
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testExtractNullValues() {
        geoNamesRemoteDownloader.getResourceFromUrl(null, null);
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testExtractFileFromMalformedUrl1() {
        try {
            geoNamesRemoteDownloader.getResourceFromUrl(MALFORMED_URL_1, null);
        } catch (GeoNamesRemoteDownloadException e) {
            assertThat(e.getCause(), instanceOf(UnknownHostException.class));
            throw e;
        }
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testExtractFileFromMalformedUrl2() {
        geoNamesRemoteDownloader.getResourceFromUrl(MALFORMED_URL_2, null);
    }

    @Test(expected = GeoNamesRemoteDownloadException.class)
    public void testExtractFileFromUrlInvalidFile404() {
        geoNamesRemoteDownloader.getResourceFromUrl(VALID_URL + "s.zip", null);
    }


}
