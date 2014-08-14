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
package ddf.catalog.security.plugin;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security-based plugin that looks for a subject using SecurityUtils and adds it to the current
 * operation's properties map.
 */
public class SecurityPlugin implements PreQueryPlugin, PreIngestPlugin, PreResourcePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityPlugin.class);

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {
        setSubject(input);
        return input;
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        setSubject(input);
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        setSubject(input);
        return input;
    }

    @Override
    public QueryRequest process(QueryRequest input)
            throws PluginExecutionException, StopProcessingException {
        setSubject(input);
        return input;
    }

    @Override
    public ResourceRequest process(ResourceRequest input)
            throws PluginExecutionException, StopProcessingException {
        setSubject(input);
        return input;
    }

    private void setSubject(Operation operation) {
        try {
            Subject subject = SecurityUtils.getSubject();
            if (subject instanceof ddf.security.Subject) {
                operation.getProperties()
                        .put(SecurityConstants.SECURITY_SUBJECT, (ddf.security.Subject) subject);
                LOGGER.debug(
                        "Copied security subject from SecurityUtils  to operation property for legacy and multi-thread support.");
            } else {
                LOGGER.debug(
                        "Security subject was not of type ddf.security.Subject, cannot add to current operation. It may still be accessible from SecurityUtils for supporting services.");
            }
        } catch (Exception e) {
            LOGGER.debug("No security subject found, cannot add to current operation.");
        }
    }

}
