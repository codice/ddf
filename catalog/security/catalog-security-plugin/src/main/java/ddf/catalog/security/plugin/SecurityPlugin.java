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
package ddf.catalog.security.plugin;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;

/**
 * Security-based plugin that looks for a subject using SecurityUtils and adds it to the current
 * operation's properties map.
 */
public class SecurityPlugin implements AccessPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityPlugin.class);

    @Override
    public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
        setSubject(input);
        return input;
    }

    @Override
    public UpdateRequest processPreUpdate(UpdateRequest input) throws StopProcessingException {
        setSubject(input);
        return input;
    }

    @Override
    public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
        setSubject(input);
        return input;
    }

    @Override
    public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
        setSubject(input);
        return input;
    }

    @Override
    public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
        return input;
    }

    private void setSubject(Operation operation) {
        try {
            Object requestSubject = operation.getProperties()
                    .get(SecurityConstants.SECURITY_SUBJECT);
            Subject subject = null;
            if (!(requestSubject instanceof ddf.security.Subject)) {
                subject = SecurityUtils.getSubject();
                if (subject instanceof ddf.security.Subject) {
                    operation.getProperties()
                            .put(SecurityConstants.SECURITY_SUBJECT,
                                    (ddf.security.Subject) subject);
                    LOGGER.debug(
                            "Copied security subject from SecurityUtils  to operation property for legacy and multi-thread support.");
                } else {
                    LOGGER.debug(
                            "Security subject was not of type ddf.security.Subject, cannot add to current operation. It may still be accessible from SecurityUtils for supporting services.");
                }
            }
        } catch (Exception e) {
            LOGGER.debug("No security subject found, cannot add to current operation.");
        }
    }
}
