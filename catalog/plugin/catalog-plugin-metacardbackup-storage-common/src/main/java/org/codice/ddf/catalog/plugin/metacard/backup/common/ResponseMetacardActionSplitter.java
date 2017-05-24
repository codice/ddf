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
package org.codice.ddf.catalog.plugin.metacard.backup.common;

import java.util.ArrayList;
import java.util.List;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;

/**
 * Takes the Response and returns the metacards.
 */
public class ResponseMetacardActionSplitter {

    public List<Metacard> split(Response response) {
        List<Metacard> metacards = new ArrayList<>();
        if (response instanceof CreateResponse) {
            CreateResponse createResponse = (CreateResponse) response;
            metacards.addAll(createResponse.getCreatedMetacards());
        } else if (response instanceof UpdateResponse) {
            UpdateResponse updateResponse = (UpdateResponse) response;
            List<Update> updates = updateResponse.getUpdatedMetacards();
            for (Update update : updates) {
                metacards.add(update.getNewMetacard());
            }
        } else if (response instanceof DeleteResponse) {
            DeleteResponse deleteResponse = (DeleteResponse) response;
            metacards.addAll(deleteResponse.getDeletedMetacards());
        }
        return metacards;
    }
}
