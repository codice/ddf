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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.shiro.SecurityUtils;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.security.SubjectUtils;

public class HistorianPlugin implements PostIngestPlugin {
    private final CatalogProvider catalogProvider;

    public HistorianPlugin(CatalogProvider catalogProvider) {
        this.catalogProvider = catalogProvider;
    }

    // TODO (RCZ) - Move this enum.... somewhere. or just forget the enum?
    private enum Action {
        CREATED("Created"),
        UPDATED("Updated"),
        DELETED("Deleted");

        private String key;

        Action(String key) {
            this.key = key;
        }

        String getKey() {
            return this.key;
        }

        private static Map<String, Action> keyMap = new HashMap<>();

        static {
            for (Action action : Action.values()) {
                keyMap.put(action.getKey(), action);
            }
        }

        Action getKey(String key) {
            return keyMap.get(key);
        }
    }

    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        List<Metacard> metacards = getVersionedMetacards(input.getCreatedMetacards(),
                Action.CREATED);

        store(metacards);
        return input;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        List<Metacard> inputMetacards = input.getUpdatedMetacards()
                .stream()
                .map(Update::getNewMetacard)
                .collect(Collectors.toList());
        List<Metacard> metacards = getVersionedMetacards(inputMetacards, Action.UPDATED);
        store(metacards);
        return input;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        List<Metacard> metacards = getVersionedMetacards(input.getDeletedMetacards(),
                Action.DELETED);

        store(metacards);
        return input;
    }

    private List<Metacard> getVersionedMetacards(List<Metacard> metacards, Action action) {
        List<Metacard> versioned = new ArrayList<>();

        for (Metacard metacard : metacards) {
            MetacardImpl versionedMetacard = new MetacardImpl(metacard,
                    BasicTypes.VERSION_HISTORY_METACARD);

            versionedMetacard.setAttribute(Metacard.STATE, action.getKey());
            versionedMetacard.setAttribute(Metacard.EDITED_BY,
                    SubjectUtils.getName(SecurityUtils.getSubject()));
            versionedMetacard.setAttribute(Metacard.VERSIONED, Date.from(Instant.now()));
            versionedMetacard.setAttribute(Metacard.METACARD_ID, metacard.getId());
            versionedMetacard.setAttribute(Metacard.ID,
                    UUID.randomUUID()
                            .toString()
                            .replace("-", ""));
            // TODO (RCZ) - Do we want to overwrite or add?
            versionedMetacard.setAttribute(Metacard.TAGS, Metacard.HISTORY_TAG);

            versioned.add(versionedMetacard);
        }

        return versioned;
    }

    private void store(List<Metacard> metacards) throws PluginExecutionException {
        try {
            catalogProvider.create(new CreateRequestImpl(metacards));
        } catch (IngestException e) {
            throw new PluginExecutionException(e);
        }
    }
}
