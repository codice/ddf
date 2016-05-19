/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * </p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.metacard.versioning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.core.versioning.HistoryMetacardImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.Subject;
import ddf.security.common.util.Security;

public class HistorianPlugin implements PostIngestPlugin, PreIngestPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistorianPlugin.class);

    private static final String HISTORY_METACARDS = "history-metacards";

    private final CatalogFramework catalogFramework;

    public HistorianPlugin(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        List<Metacard> historyMetacards = input.getCreatedMetacards()
                .stream()
                .filter(m -> HistoryMetacardImpl.getVersionHistoryMetacardType()
                        .getName()
                        .equals(m.getMetacardType()
                                .getName()))
                .collect(Collectors.toList());

        if (historyMetacards.isEmpty()) {
            return input;
        }

        List<Metacard> resultMetacards = new ArrayList<>(input.getCreatedMetacards());
        resultMetacards.removeAll(historyMetacards);

        Map<String, Serializable> properties =
                input.getProperties() != null ? input.getProperties() : new HashMap<>();
        properties.put(HISTORY_METACARDS, new ArrayList<Serializable>(historyMetacards));
        return new CreateResponseImpl(input.getRequest(),
                properties,
                resultMetacards,
                input.getProcessingErrors());
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        List<Metacard> inputMetacards = input.getUpdatedMetacards()
                .stream()
                .map(Update::getNewMetacard)
                .collect(Collectors.toList());
        getVersionedMetacards(inputMetacards, HistoryMetacardImpl.Action.UPDATED);
        return input;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        getVersionedMetacards(input.getDeletedMetacards(), HistoryMetacardImpl.Action.DELETED);
        return input;
    }

    private void getVersionedMetacards(List<Metacard> metacards,
            final HistoryMetacardImpl.Action action) throws PluginExecutionException {
        final List<Metacard> versionedMetacards = metacards.stream()
                .filter(metacard -> !metacard.getMetacardType()
                        .equals(HistoryMetacardImpl.getVersionHistoryMetacardType()))
                .map(metacard -> new HistoryMetacardImpl(metacard,
                        action,
                        SecurityUtils.getSubject()))
                .collect(Collectors.toList());

        if (versionedMetacards.isEmpty()) {
            return;
        }

        Subject system = Security.getSystemSubject();
        if (system == null) {
            LOGGER.warn("Could not get system subject to create versioned metacards.");
            return;
        }

        system.execute(() -> {
            this.store(versionedMetacards);
            return true;
        });
    }

    private void store(List<Metacard> metacards) throws PluginExecutionException {
        try {
            catalogFramework.create(new CreateRequestImpl(metacards));
        } catch (SourceUnavailableException | IngestException e) {
            throw new PluginExecutionException(e);
        }
    }

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {
        List<Metacard> updatedMetacardList = input.getMetacards()
                .stream()
                .filter(m -> !HistoryMetacardImpl.getVersionHistoryMetacardType()
                        .equals(m.getMetacardType()))
                .map(m -> new HistoryMetacardImpl(m,
                        HistoryMetacardImpl.Action.CREATED,
                        SecurityUtils.getSubject()))
                .collect(Collectors.toList());
        updatedMetacardList.addAll(input.getMetacards());
        return new CreateRequestImpl(updatedMetacardList, input.getProperties());
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }
}
