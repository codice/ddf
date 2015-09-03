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

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;

/**
 * IngestPlugin is a PreIngestPlugin that restricts the create/update/delete operations
 * on the catalog to a group defined by a set of configurable user attributes.
 */
public class IngestPlugin implements PreIngestPlugin {

    private String permissionsString;

    private List<KeyValuePermission> permissions = new ArrayList<>();

    public CreateRequest process(CreateRequest createRequest)
            throws PluginExecutionException, StopProcessingException {
        authenticateUser(CollectionPermission.CREATE_ACTION);
        return createRequest;
    }

    public UpdateRequest process(UpdateRequest updateRequest)
            throws PluginExecutionException, StopProcessingException {
        authenticateUser(CollectionPermission.UPDATE_ACTION);
        return updateRequest;
    }

    public DeleteRequest process(DeleteRequest deleteRequest)
            throws PluginExecutionException, StopProcessingException {
        authenticateUser(CollectionPermission.DELETE_ACTION);
        return deleteRequest;
    }

    /**
     * Getter used by the framework to populate the configuration ui
     *
     * @return
     */
    public String getPermissionsString() {
        return this.permissionsString;
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
     * @param permString
     */
    public void setPermissionsString(String permString) {
        this.permissionsString = permString;
        parsePermissionsFromString(this.permissionsString);
    }

    /**
     * Authenticates the current Subject against the configured ingest permissions.
     * If the subject doesn't have the right credentials than a StopProcessingException
     * will be thrown
     *
     * @throws StopProcessingException
     */
    private void authenticateUser(String action) throws StopProcessingException {

        Subject subject = SecurityUtils.getSubject();
        KeyValueCollectionPermission kvcp = new KeyValueCollectionPermission(action, permissions);
        if (subject == null || !subject.isPermitted(kvcp)) {
            throw new StopProcessingException(
                    "User is not authorized to create, update or delete records");
        }
    }

    /**
     * Parses a string representation of permission attributes the
     * same way as the web policy context manager
     *
     * @param permString String to parse
     */
    private void parsePermissionsFromString(String permString) {
        permissions.clear();
        if (StringUtils.isNotEmpty(permString)) {
            if (permString.startsWith("{") && permString.endsWith("}")) {
                if (permString.length() == 2) {
                    permString = "";
                } else {
                    permString = permString.substring(1, permString.length() - 1);
                }
            }
            String[] attributesArray = permString.split(";");

            for (String attribute : attributesArray) {
                String[] parts = attribute.split("=");
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
