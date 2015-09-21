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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.cxf.jaxrs.client.WebClient;

import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryCreator;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.ProgressCallback;

import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;

public class GeoNamesUrlExtractor implements GeoEntryExtractor {

    private static final int BUFFER_SIZE = 4096;

    private String url = "http://download.geonames.org/export/dump";

    private WebClient webClient;

    private GeoEntryCreator geoEntryCreator;

    public void setGeoEntryCreator(final GeoEntryCreator geoEntryCreator) {
        this.geoEntryCreator = geoEntryCreator;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public List<GeoEntry> getGeoEntries(String resource, final ProgressCallback progressCallback) {
        final List<GeoEntry> geoEntryList = new ArrayList<>(1000);

        final ExtractionCallback extractionCallback = new ExtractionCallback() {
            @Override
            public void extracted(final GeoEntry newEntry) {
                geoEntryList.add(newEntry);
            }

            @Override
            public void updateProgress(final int progress) {
                if (progressCallback != null) {
                    progressCallback.updateProgress(progress);
                }
            }
        };

        if (!FilenameUtils.getExtension(resource).equals("")) {
            throw new GeoNamesRemoteDownloadException(
                    "Invalid file extension found.  The given input must be a zip filename.");
        }

        getGeoEntriesStreaming(resource, extractionCallback);

        return geoEntryList;
    }

    @Override
    public void getGeoEntriesStreaming(String resource, ExtractionCallback extractionCallback) {
        if (extractionCallback == null) {
            throw new IllegalArgumentException("You must pass a non-null callback.");
        }

        if (resource.equals("all")) {
            resource = "allCountries";
        }

        ByteSource byteSource = getResourceFromUrl(url + "/" + resource + ".zip",
                extractionCallback);
        extractionCallback.updateProgress(0);
        ByteSource byteOutput = unzipFileByteSource(byteSource);

        try {
            InputStream inputStream = byteOutput.openBufferedStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);

            long totalFileSize = byteOutput.size();
            double bytesRead = 0.0;

            for (String line; (line = reader.readLine()) != null; ) {
                extractionCallback.extracted(extractGeoEntry(line));
                bytesRead += line.getBytes().length;
                extractionCallback
                        .updateProgress((int) Math.floor((bytesRead / totalFileSize) * 100));
            }
            extractionCallback.updateProgress(100);

            reader.close();
            inputStreamReader.close();
            inputStream.close();

        } catch (IOException | NullPointerException e) {
            throw new GeoEntryExtractionException("Unable to open stream for " + resource, e);
        }
    }

    /**
     * Download a GeoNames .zip file from a remote location
     *
     * @param url - the full url of the file to download
     *            ( ex. http://http://download.geonames.org/export/dump/AD.zip )
     *
     * @param progressCallback -  the callback to receive updates about the indexing progress, may be
     *                          null if you don't want any updates
     */
    private ByteSource getResourceFromUrl(String url, final ProgressCallback progressCallback)
            throws GeoNamesRemoteDownloadException {
        int responseCode = 0;

        try {

            Response response = createConnection(url);
            responseCode = response.getStatus();

            int totalFileSize = response.getLength();

            InputStream inputStream = getInputStreamFromClient(webClient);
            FileBackedOutputStream fileOutputStream = new FileBackedOutputStream(BUFFER_SIZE);

            double totalBytesRead = 0.0;
            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (progressCallback != null) {
                    progressCallback.updateProgress(
                            (int) Math.ceil((totalBytesRead / totalFileSize) * 100));
                }
            }

            ByteSource byteSource = fileOutputStream.asByteSource();
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();
            closeConnection();

            return byteSource;

        } catch (Exception e) {
            throw new GeoNamesRemoteDownloadException(
                    "Unable to download " + url + ".  Server responded with : " + responseCode,
                    e);
        }
    }

    public Response createConnection(String url) {
        webClient = WebClient.create(url);
        webClient.path(url);
        Response response = webClient.get();
        return response;
    }

    public InputStream getInputStreamFromClient(WebClient client) {
        return client.get(InputStream.class);
    }

    public void closeConnection() {
        webClient.close();
    }

    /**
     *  Unzips the ByteSource of a .zip file
     *
     * @param byteSource - the bytesource to be unzipped
     * @return - the unzipped bytesource
     */

    public ByteSource unzipFileByteSource(ByteSource byteSource) {
        try {
            ZipInputStream zipInputStream = new ZipInputStream(byteSource.openBufferedStream());
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {

                // GeoNames <filename>.zip files will contain <filename>.txt and readme.txt
                if (!zipEntry.getName().equals("readme.txt")) {

                    FileBackedOutputStream bufferedOutputStream = new FileBackedOutputStream(
                            BUFFER_SIZE);

                    byte data[] = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = zipInputStream.read(data, 0, BUFFER_SIZE)) != -1) {
                        bufferedOutputStream.write(data, 0, bytesRead);
                    }

                    ByteSource zipByteSource = bufferedOutputStream.asByteSource();
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                    return zipByteSource;
                }
            }
            zipInputStream.close();

        } catch (IOException | NullPointerException e) {
            throw new GeoEntryExtractionException("Unable to extract zip file : ", e);
        }
        throw new GeoEntryExtractionException("Unable to extract zip file.");
    }

    private GeoEntry extractGeoEntry(final String line) {
        return geoEntryCreator.createGeoEntry(line);
    }
}
