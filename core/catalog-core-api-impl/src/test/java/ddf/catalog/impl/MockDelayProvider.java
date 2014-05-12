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
package ddf.catalog.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.ContentType;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.CatalogProvider;

public class MockDelayProvider extends MockSource implements CatalogProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockDelayProvider.class);

    private MockMemoryProvider provider;

    private long createDelayMillis = 0;

    private long updateDelayMillis = 0;

    private long deleteDelayMillis = 0;

    private long queryDelayMillis = 0;

    private String sourceId = "mockDelayProvider";

    public MockDelayProvider(String shortName, String title, String version, String organization,
            Set<ContentType> catalogTypes, boolean isAvailable, Date lastAvailability) {
        super(shortName, title, version, organization, catalogTypes, isAvailable, lastAvailability);
        provider = new MockMemoryProvider(shortName, title, version, organization, catalogTypes,
                isAvailable, lastAvailability);
    }

    @Override
    public CreateResponse create(CreateRequest request) {
        try {
            Thread.sleep(createDelayMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to sleep during create.", e);
        }
        return provider.create(request);
    }

    @Override
    public UpdateResponse update(UpdateRequest request) {
        try {
            Thread.sleep(updateDelayMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to sleep during update.", e);
        }
        return provider.update(request);
    }

    @Override
    public DeleteResponse delete(DeleteRequest request) {
        try {
            Thread.sleep(deleteDelayMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to sleep during delete.", e);
        }
        return provider.delete(request);
    }

    @Override
    public SourceResponse query(QueryRequest query) {
        try {
            Thread.sleep(queryDelayMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to sleep during query.", e);
        }
        return provider.query(query);
    }

    public int size() {
        return provider.size();
    }

    public boolean hasReceivedQuery() {
        return provider.hasReceivedQuery();
    }

    public boolean hasReceivedRead() {
        return provider.hasReceivedRead();
    }

    public boolean hasReceivedCreate() {
        return provider.hasReceivedCreate();
    }

    public boolean hasReceivedUpdate() {
        return provider.hasReceivedUpdate();
    }

    public boolean hasReceivedDelete() {
        return provider.hasReceivedDelete();
    }

    public boolean hasReceivedUpdateByIdentifier() {
        return provider.hasReceivedUpdateByIdentifier();
    }

    public boolean hasReceivedDeleteByIdentifier() {
        return provider.hasReceivedDeleteByIdentifier();
    }

    @Override
    public String getId() {
        return sourceId;
    }

    @Override
    public void maskId(String sourceId) {
        this.sourceId = sourceId;

    }

    @Override
    public Set<ContentType> getContentTypes() {
        return new HashSet<ContentType>();

    }

    public long getCreateDelayMillis() {
        return createDelayMillis;
    }

    public void setCreateDelayMillis(long createDelayMillis) {
        this.createDelayMillis = createDelayMillis;
    }

    public long getUpdateDelayMillis() {
        return updateDelayMillis;
    }

    public void setUpdateDelayMillis(long updateDelayMillis) {
        this.updateDelayMillis = updateDelayMillis;
    }

    public long getDeleteDelayMillis() {
        return deleteDelayMillis;
    }

    public void setDeleteDelayMillis(long deleteDelayMillis) {
        this.deleteDelayMillis = deleteDelayMillis;
    }

    public long getQueryDelayMillis() {
        return queryDelayMillis;
    }

    public void setQueryDelayMillis(long queryDelayMillis) {
        this.queryDelayMillis = queryDelayMillis;
    }

}
