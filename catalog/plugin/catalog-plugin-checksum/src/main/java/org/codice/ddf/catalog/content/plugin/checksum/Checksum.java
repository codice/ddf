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
 */
package org.codice.ddf.catalog.content.plugin.checksum;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.codice.ddf.checksum.ChecksumProvider;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.plugin.PreCreateStoragePlugin;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.plugin.PluginExecutionException;

public class Checksum implements PreCreateStoragePlugin {
    private final ChecksumProvider checksumProvider;

    public Checksum(ChecksumProvider checksumProvider) {
        this.checksumProvider = checksumProvider;
    }

    @Override
    public CreateStorageRequest process(CreateStorageRequest input)
            throws PluginExecutionException {
        if (input == null) {
            throw new IllegalArgumentException("CreateRequest input cannot be null");
        }

        for (ContentItem contentItem : input.getContentItems()) {
            try (InputStream inputStream = contentItem.getInputStream()) {
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

                addChecksumAttributes(contentItem.getMetacard(), checksumAlgorithm, checksumValue);
            } catch (IOException e) {
                throw new PluginExecutionException(
                        "Unable to retrieve input stream for content item", e);
            }
        }

        return input;
    }

    private void addChecksumAttributes(Metacard metacard, final String checksumAlgorithm,
            final String checksumValue) {
        metacard.setAttribute(
                new AttributeImpl(Metacard.RESOURCE_CHECKSUM_ALGORITHM, checksumAlgorithm));
        metacard.setAttribute(new AttributeImpl(Metacard.RESOURCE_CHECKSUM, checksumValue));
    }
}
