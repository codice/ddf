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
import java.util.Map;

import org.codice.ddf.checksum.ChecksumProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.content.data.impl.ContentMetacardType;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.impl.CreateRequestImpl;
import ddf.content.plugin.PluginExecutionException;
import ddf.content.plugin.PreCreateStoragePlugin;

public class Checksum implements PreCreateStoragePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(Checksum.class);

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
            //retrieve the catalogId and corresponding metacard
            //so that we can shove in the checksum into its metadata
            Map<String, Serializable> properties = input.getProperties();

            //calculate checksum so that it can be added as an attribute on metacard
            String checksumAlgorithm = checksumProvider.getChecksumAlgorithm();
            String checksumValue = null;

            try {
                checksumValue = checksumProvider.calculateChecksum(inputStream);
            } catch (IOException e) {
                throw new PluginExecutionException("Error calculating checksum", e);
            } catch (NoSuchAlgorithmException e) {
                throw new PluginExecutionException("Unsupported algorithm", e);
            }

            properties.put(ContentMetacardType.RESOURCE_CHECKSUM_ALGORITHM, checksumAlgorithm);
            properties.put(ContentMetacardType.RESOURCE_CHECKSUM, checksumValue);

            return new CreateRequestImpl(input.getContentItem(), properties);
        } catch (IOException e) {
            throw new PluginExecutionException("Unable to retrieve input stream for content item",
                    e);
        }

    }

}
