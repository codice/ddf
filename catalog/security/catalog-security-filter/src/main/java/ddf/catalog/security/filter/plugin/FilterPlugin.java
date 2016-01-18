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
package ddf.catalog.security.filter.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;

/**
 * This {@link AccessPlugin} performs redaction and filtering on {@link QueryResponse} objects as
 * they pass through the framework.
 */
public class FilterPlugin implements AccessPlugin {

    private final Logger logger = LoggerFactory.getLogger(FilterPlugin.class);

    @Override
    public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
        return input;
    }

    @Override
    public UpdateRequest processPreUpdate(UpdateRequest input) throws StopProcessingException {
        return input;
    }

    @Override
    public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
        return input;
    }

    @Override
    public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
        return input;
    }

    @Override
    public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
        if (input.getRequest() == null || input.getRequest()
                .getProperties() == null) {
            throw new StopProcessingException(
                    "Unable to filter contents of current message, no user Subject available.");
        }
        Object securityAssertion = input.getRequest()
                .getProperties()
                .get(SecurityConstants.SECURITY_SUBJECT);
        Subject subject;
        if (securityAssertion instanceof Subject) {
            subject = (Subject) securityAssertion;
            logger.debug("Filter plugin found Subject for query response.");
        } else {
            throw new StopProcessingException(
                    "Unable to filter contents of current message, no user Subject available.");
        }

        List<Result> results = input.getResults();
        List<Result> newResults = new ArrayList<Result>(results.size());
        Metacard metacard;
        KeyValueCollectionPermission securityPermission = new KeyValueCollectionPermission(
                CollectionPermission.READ_ACTION);
        int filteredMetacards = 0;
        for (Result result : results) {
            metacard = result.getMetacard();
            Attribute attr = metacard.getAttribute(Metacard.SECURITY);
            Map<String, Set<String>> map = null;

            if (null != attr) {
                map = (Map<String, Set<String>>) attr.getValue();
            }
            securityPermission.clear();
            if (map != null) {
                Map<String, List<String>> permMap = new HashMap<>();
                for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
                    permMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
                securityPermission.addAll(permMap);
            }
            if (!subject.isPermitted(securityPermission)) {
                filteredMetacards++;
            } else {
                newResults.add(result);
            }
        }

        logger.info("Filtered {} metacards, returned {}", filteredMetacards, newResults.size());
        SecurityLogger.logInfo(
                "Filtered " + filteredMetacards + " metacards, returned " + newResults.size());

        input.getResults()
                .clear();
        input.getResults()
                .addAll(newResults);
        newResults.clear();
        return input;
    }
}
