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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GeoNamesRemoteDownloader  {

    private static final int BUFFER_SIZE = 4096;

    private String indexLocation = System.getProperty("user.dir") + "/data/tmp";

    /**
     * Used for testing purposes.
     *
     * @param indexLocation
     */

    public void setIndexLocation(String indexLocation) {
        this.indexLocation = indexLocation;
    }

    /**
     * Download a GeoNames .zip file from a remote location
     *
     * @param url - the URL of the GeoNames zip file ( ex: http://download.geonames.org/export/dump/AD.zip )
     *
     * @param progressCallback -  the callback to receive updates about the indexing progress, may be
     *                          null if you don't want any updates
     */
    public String getResourceFromUrl(String url, final ProgressCallback progressCallback)
            throws GeoNamesRemoteDownloadException {

        URL downloadUrl;
        HttpURLConnection httpURLConnection;
        int responseCode = 0;

        try {
            downloadUrl = new URL(url);
            httpURLConnection = (HttpURLConnection) downloadUrl.openConnection();
            responseCode = httpURLConnection.getResponseCode();
        } catch (MalformedURLException e) {
            throw new GeoNamesRemoteDownloadException(url + " is not a valid URL.", e);
        } catch (IOException | RuntimeException e) {
            throw new GeoNamesRemoteDownloadException("Could not connect to " + url + " : " + responseCode, e);
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {

            try {
                String[] pathBuffer = url.split("/");
                String fileName = pathBuffer[pathBuffer.length - 1];

                if (!fileName.endsWith(".zip")) {
                    throw new GeoNamesRemoteDownloadException(
                            "The intended file to download and import must be a .zip file.");
                }

                InputStream inputStream = httpURLConnection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(indexLocation + "/" + fileName);

                int totalFileSize = httpURLConnection.getContentLength();
                double totalBytesRead = 0.0;
                int bytesRead = -1;
                byte[] buffer = new byte[BUFFER_SIZE];

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    if (progressCallback != null) {
                        progressCallback.updateProgress((int)Math.ceil((totalBytesRead / totalFileSize) * 100));
                    }
                }

                fileOutputStream.close();
                inputStream.close();

                return (indexLocation + "/" + fileName);

            } catch (IOException e) {
                throw new GeoNamesRemoteDownloadException("Unable to open InputStream at " + indexLocation , e);
            }
        } else {
            throw new GeoNamesRemoteDownloadException("Unable to download from " + url + ".\nServer responded with : " + responseCode);
        }
    }

    /**
     * Delete a file from the path.
     *
     * @param path - the path to the file to remove from the filesystem.
     */
    public void deleteDownloadedFile(String path) {
        File file = new File(path);
        if(!file.delete()) {
            throw new GeoNamesRemoteDownloadException("Unable to remove file from " + path + ".");
        }
    }
}
