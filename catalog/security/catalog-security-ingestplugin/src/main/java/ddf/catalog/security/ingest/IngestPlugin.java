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
package ddf.catalog.security.ingest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.common.util.Security;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValuePermission;

/**
 * IngestPlugin is a PreIngestPlugin that restricts the create/update/delete operations
 * on the catalog to a group defined by a set of configurable user attributes.
 */
public class IngestPlugin implements PreIngestPlugin {

    private String[] permissionStrings;

    private List<KeyValuePermission> permissions = new ArrayList<>();

    public CreateRequest process(CreateRequest createRequest)
            throws PluginExecutionException, StopProcessingException {
        if (!Security.authorizeCurrentUser(CollectionPermission.CREATE_ACTION, permissions)) {
            throw new StopProcessingException("User is not authorized to create records");
        }
        return createRequest;
    }

    public UpdateRequest process(UpdateRequest updateRequest)
            throws PluginExecutionException, StopProcessingException {
        if (!Security.authorizeCurrentUser(CollectionPermission.UPDATE_ACTION, permissions)) {
            throw new StopProcessingException("User is not authorized to update records");
        }
        return updateRequest;
    }

    public DeleteRequest process(DeleteRequest deleteRequest)
            throws PluginExecutionException, StopProcessingException {
        if (!Security.authorizeCurrentUser(CollectionPermission.DELETE_ACTION, permissions)) {
            throw new StopProcessingException("User is not authorized to delete records");
        }
        return deleteRequest;
    }

    /**
     * Getter used by the framework to populate the configuration ui
     *
     * @return
     */
    public String[] getPermissionStrings() {
        if(permissionStrings != null) {
            return Arrays.copyOf(this.permissionStrings, permissionStrings.length);
        }
        return null;
    }

    /**
     * Get the KeyValuePermission that have been parsed from the permissions string
     *
     * @return
     */
    public List<KeyValuePermission> getPermissions() {
        return Collections.unmodifiableList(permissions);
    }

    /**
     * Setter used by the ui to set the permissions/attributes
     *
     * @param permStrings
     */
    public void setPermissionStrings(String[] permStrings) {
        if(permStrings != null) {
            this.permissionStrings = Arrays.copyOf(permStrings, permStrings.length);
            parsePermissionsFromString(permStrings);
        }
    }

    /**
     * Parses a string  array representation of permission attributes
     *
     * @param permStrings String array of permissions to parse
     */
    private void parsePermissionsFromString(String[] permStrings) {
        permissions.clear();
        if (permStrings != null) {
            for (String perm : permStrings) {
                String[] parts = perm.split("=");
                if (parts.length == 2) {
                    String attributeName = parts[0];
                    String attributeValue = parts[1];
                    permissions.add(new KeyValuePermission(attributeName,
                            Arrays.asList(attributeValue)));
                }
            }
        }
    }
}
