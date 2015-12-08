/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.sdk.cometd;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to download a product from the DDF.
 * Since this could be a long running process it is advised that it is run in a separate thread.
 */
public class DownloadManager implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadManager.class);
    private final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();

    private final URL url;
    private final String outputFileName;

    /**
     * Creates a DownloadManager for a product within the DDF
     * @param url - product url for retrieval
     * @param outputFileName - name of output file
     * @throws MalformedURLException
     */
    public DownloadManager(String url, String outputFileName) throws MalformedURLException {
        this.url = new URL(url);
        this.outputFileName = outputFileName;
    }

    @Override
    public void run() {
        String mimeType = null;
        try (ReadableByteChannel byteChannel = Channels.newChannel(url.openStream())) {
            mimeType = url.openConnection().getContentType();
            String fileExtension = allTypes.forName(mimeType).getExtension();
            LOGGER.debug("downloading product from: " + url.toString());
            LOGGER.debug("mimetype is: " + mimeType);
            LOGGER.debug("File Extension is: " + fileExtension);
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFileName + fileExtension)) {
                fileOutputStream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                LOGGER.error("Error opening stream for {}", outputFileName, e);
            }
        } catch (IOException e) {
            LOGGER.error("Error downloading file from url: {}", url, e);
        } catch (MimeTypeException e) {
            LOGGER.error("Error determining file extension from mimetype: {}", mimeType, e);
        }
    }
}
