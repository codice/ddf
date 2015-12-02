/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.security.audit;

import org.apache.shiro.subject.Subject;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import ddf.security.common.audit.SecurityLogger;

/**
 * Security-based plugin that Audits Resource Retrieval requests.
 */
public class AuditPlugin implements PreIngestPlugin, PostIngestPlugin, PreQueryPlugin,
        PostQueryPlugin, PreFederatedQueryPlugin, PostFederatedQueryPlugin, PreResourcePlugin, PostResourcePlugin {

    /* Audit logs ingest requests
     */
    @Override
    public CreateRequest process(CreateRequest input)
        throws PluginExecutionException, StopProcessingException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] is attempting to ingest resource [" + input.toString() + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] is attempting to update resource [" + input.UPDATE_BY_ID + "].";
                SecurityLogger.logInfo(msg);

            }
        }
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] is attempting to delete resource [" + input.DELETE_BY_ID + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

    /*
    * Audit logs ingest responses.
     */
    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] successfully created [" + input.getCreatedMetacards() +"].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] successfully updated [" + input.getUpdatedMetacards() + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] successfully deleted [" + input.getDeletedMetacards() + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

    /*
     * Audit logs Query requests.
     */

    @Override
    public QueryRequest process(QueryRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] is attempting to query [" + input.getQuery() + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

    /*
    * Audit logs Query and federated query responses.
     */
    @Override
    public QueryResponse process(QueryResponse input) throws PluginExecutionException, StopProcessingException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] successfully executed query [" + input.getResults() + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }



    /*
     * Audit logs Federated query requests.
     */
    @Override
    public QueryRequest process(Source source, QueryRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (input != null && source != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] is attempting to query [" + input.getQuery() + "] from [" + source.getId() + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

    /*
     * Audit logs Resource requests
     */
    @Override
    public ResourceRequest process(ResourceRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] is attempting to retrieve resource [" + input
                        .getAttributeValue() + "] by [" + input.getAttributeName() + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

    @Override
    public ResourceResponse process(ResourceResponse input)
            throws PluginExecutionException, StopProcessingException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj, false);
                String msg = "User [" + username + "] successfully retrieved resource [" + input
                        .getRequest().getAttributeValue() + "] by [" + input.getRequest()
                        .getAttributeName() + "] with file name [" + input.getResource().getName()
                        + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }
}
