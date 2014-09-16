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
package org.codice.ddf.catalog.security.logging;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SubjectUtils;
import ddf.security.common.audit.SecurityLogger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * Logs the current operation being performed to the security logger.
 */
public class SecurityLoggingPlugin implements PreQueryPlugin, PreIngestPlugin, PreResourcePlugin {

    private static final String NO_USER = "UNKNOWN";

    private enum CatalogOperationType {
        INGEST, UPDATE, DELETE, QUERY, RESOURCE_REQUEST
    }

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {
        logOperation(CatalogOperationType.INGEST);
        return input;
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        logOperation(CatalogOperationType.UPDATE);
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        logOperation(CatalogOperationType.DELETE);
        return input;
    }

    @Override
    public QueryRequest process(QueryRequest input)
            throws PluginExecutionException, StopProcessingException {
        logOperation(CatalogOperationType.QUERY);
        return input;
    }

    @Override
    public ResourceRequest process(ResourceRequest input)
            throws PluginExecutionException, StopProcessingException {
        logOperation(CatalogOperationType.RESOURCE_REQUEST);
        return input;
    }

    private void logOperation(CatalogOperationType operationType) {
        String user;
        try {
            Subject subject = SecurityUtils.getSubject();
            user = SubjectUtils.getName(subject, NO_USER);
        } catch (Exception e) {
            user = NO_USER;
        }
        SecurityLogger.logInfo("User [" + user + "] performing " + operationType + " operation on catalog.");
    }
}
