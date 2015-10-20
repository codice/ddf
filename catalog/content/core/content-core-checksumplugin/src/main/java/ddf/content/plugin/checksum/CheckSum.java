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
import java.util.Map;

import org.codice.ddf.checksum.CheckSumProvider;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.content.data.impl.ContentMetacardType;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.impl.CreateRequestImpl;
import ddf.content.plugin.PluginExecutionException;
import ddf.content.plugin.PreCreateStoragePlugin;

public class CheckSum implements PreCreateStoragePlugin {

    private static final XLogger LOGGER = new XLogger(LoggerFactory.getLogger(CheckSum.class));
    private final CheckSumProvider checkSumProvider;

    public CheckSum(CheckSumProvider checkSumProvider) {
        this.checkSumProvider = checkSumProvider;

    }

    @Override
    public CreateRequest process(CreateRequest input) throws PluginExecutionException {
        InputStream inputStream;

        try {
            inputStream = input.getContentItem().getInputStream();
        } catch (IOException e) {
            LOGGER.error("Unable to retrieve input stream for metacard");
            return null;
        }

        //retrieve the catalogId and corresponding metacard
        //so that we can shove in the checksum into its metadata
        Map<String,Serializable> properties = input.getProperties();

        //calculate checksum so that it can be added as an attribute on metacard
        String checkSumAlgorithm  = checkSumProvider.getCheckSumAlgorithm();
        String checkSumValue = checkSumProvider.calculateCheckSum(inputStream);

        properties.put(ContentMetacardType.RESOURCE_CHECKSUM_ALGORITHM,checkSumAlgorithm);
        properties.put(ContentMetacardType.RESOURCE_CHECKSUM, checkSumValue);

        CreateRequest request = new CreateRequestImpl(input.getContentItem(),properties);
        return request;
    }

    @Override
    public ddf.content.operation.UpdateRequest process(ddf.content.operation.UpdateRequest input)
            throws PluginExecutionException {
        return  input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input) throws PluginExecutionException {
        return  input;
    }
}
