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
package ddf.content.plugin.checksum;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.checksum.ChecksumProvider;

import ddf.content.data.impl.ContentMetacardType;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.impl.CreateRequestImpl;
import ddf.content.plugin.ContentPlugin;
import ddf.content.plugin.PluginExecutionException;
import ddf.content.plugin.PreCreateStoragePlugin;

public class Checksum implements PreCreateStoragePlugin {
    private final ChecksumProvider checksumProvider;

    public Checksum(ChecksumProvider checksumProvider) {
        this.checksumProvider = checksumProvider;
    }

    @Override
    public CreateRequest process(CreateRequest input) throws PluginExecutionException {
        if (input == null) {
            throw new IllegalArgumentException("CreateRequest input cannot be null");
        }

        try (InputStream inputStream = input.getContentItem()
                .getInputStream()) {
            //calculate checksum so that it can be added as an attribute on metacard
            String checksumAlgorithm = checksumProvider.getChecksumAlgorithm();
            String checksumValue;

            try {
                checksumValue = checksumProvider.calculateChecksum(inputStream);
            } catch (IOException e) {
                throw new PluginExecutionException("Error calculating checksum", e);
            } catch (NoSuchAlgorithmException e) {
                throw new PluginExecutionException("Unsupported algorithm", e);
            }

            final Map<String, Serializable> properties = input.getProperties();
            addChecksumAttributes(properties, checksumAlgorithm, checksumValue);

            return new CreateRequestImpl(input.getContentItem(), properties);
        } catch (IOException e) {
            throw new PluginExecutionException("Unable to retrieve input stream for content item",
                    e);
        }
    }

    private void addChecksumAttributes(final Map<String, Serializable> properties,
            final String checksumAlgorithm, final String checksumValue) {
        if (!properties.containsKey(ContentPlugin.STORAGE_PLUGIN_METACARD_ATTRIBUTES)) {
            properties.put(ContentPlugin.STORAGE_PLUGIN_METACARD_ATTRIBUTES,
                    new HashMap<String, Serializable>());
        }

        @SuppressWarnings("unchecked")
        final Map<String, Serializable> attributeMap = (Map<String, Serializable>) properties.get(
                ContentPlugin.STORAGE_PLUGIN_METACARD_ATTRIBUTES);
        attributeMap.put(ContentMetacardType.RESOURCE_CHECKSUM_ALGORITHM, checksumAlgorithm);
        attributeMap.put(ContentMetacardType.RESOURCE_CHECKSUM, checksumValue);
    }
}
