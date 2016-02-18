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
package ddf.catalog.plugin.pdf.thumbnail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.pdf.PdfThumbnailGenerator;
import ddf.content.operation.UpdateResponse;
import ddf.content.operation.impl.UpdateResponseImpl;
import ddf.content.plugin.ContentPlugin;
import ddf.content.plugin.PluginExecutionException;
import ddf.content.plugin.PostUpdateStoragePlugin;

public class PdfThumbnailPlugin implements PostUpdateStoragePlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfThumbnailPlugin.class);

    private final PdfThumbnailGenerator pdfThumbnailGenerator;

    public PdfThumbnailPlugin(PdfThumbnailGenerator pdfThumbnailGenerator) {

        this.pdfThumbnailGenerator = pdfThumbnailGenerator;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        byte[] thumbnailBytes = null;

        try (InputStream contentStream = input.getUpdatedContentItem()
                .getInputStream()) {
            thumbnailBytes = pdfThumbnailGenerator.generatePdfThumbnail(contentStream);
        } catch (IOException e) {
            LOGGER.debug("Could not generate PDF Thumbnail.", e);
            return input;
        }

        Map<String, Serializable> properties = input.getProperties();
        if (!properties.containsKey(ContentPlugin.STORAGE_PLUGIN_METACARD_ATTRIBUTES)) {
            properties.put(ContentPlugin.STORAGE_PLUGIN_METACARD_ATTRIBUTES,
                    new HashMap<String, Serializable>());
        }

        @SuppressWarnings("unchecked")
        final Map<String, Serializable> map = (Map<String, Serializable>) properties.get(
                ContentPlugin.STORAGE_PLUGIN_METACARD_ATTRIBUTES);
        map.put(Metacard.THUMBNAIL, thumbnailBytes);

        return new UpdateResponseImpl(input.getRequest(),
                input.getUpdatedContentItem(),
                input.getResponseProperties(),
                properties);
    }
}
