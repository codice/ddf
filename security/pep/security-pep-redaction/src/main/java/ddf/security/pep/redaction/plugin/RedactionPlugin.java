/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.pep.redaction.plugin;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.KeyValueCollectionPermission;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This {@link PostQueryPlugin} performs redaction and filtering on {@link QueryResponse} objects as they pass through
 * the framework.
 *
 * @author tustisos
 */
public class RedactionPlugin implements PostQueryPlugin
{

    private final Logger logger = LoggerFactory.getLogger(RedactionPlugin.class);

    /**
     * Processes a {@link ddf.catalog.operation.QueryResponse} after the execution of the {@link ddf.catalog.operation.Query}.
     *
     * @param input the {@link ddf.catalog.operation.QueryResponse} to process
     * @return the value of the processed {@link ddf.catalog.operation.QueryResponse} to pass to the next
     *         {@link ddf.catalog.plugin.PostQueryPlugin}, or if this is the last
     *         {@link ddf.catalog.plugin.PostQueryPlugin} to be called
     * @throws ddf.catalog.plugin.PluginExecutionException
     *          thrown when an error occurs while processing the {@link ddf.catalog.operation.QueryResponse}
     * @throws ddf.catalog.plugin.StopProcessingException
     *          thrown to halt processing when a critical issue occurs during processing.
     *          This is intended to prevent other plugins from processing as well.
     */
    @Override
    public QueryResponse process(QueryResponse input) throws PluginExecutionException, StopProcessingException
    {
        if(input.getRequest() != null && input.getRequest().getProperties() != null)
        {
            Object securityAssertion = input.getRequest().getProperties().get(SecurityConstants.SECURITY_SUBJECT);
            Subject subject;
            if(securityAssertion instanceof Subject)
            {
                subject = (Subject) securityAssertion;
                logger.debug("Redaction plugin found Subject for query response.");
            }
            else
            {
                throw new StopProcessingException("Unable to redact contents of current message, no user Subject available.");
            }

            List<Result> results = input.getResults();
            List<Result> newResults = new ArrayList<Result>(results.size());
            Metacard metacard;
            KeyValueCollectionPermission securityPermission = new KeyValueCollectionPermission();
            for(Result result : results)
            {
                metacard = result.getMetacard();
                Attribute attr = metacard.getAttribute(Metacard.SECURITY);
                Map<String, List<String>> map = (Map<String, List<String>>) attr.getValue();
                if(map != null && !map.isEmpty())
                {
                    securityPermission = new KeyValueCollectionPermission(map);
                }
                if(!subject.isPermitted(securityPermission))
                {
                    logger.debug("Filtering metacard {}", metacard.getId());
                    SecurityLogger.logInfo("Filtering metacard "+metacard.getId());
                }
                else
                {
                    SecurityLogger.logInfo("Allowing metacard "+metacard.getId());
                    newResults.add(result);
                }
            }

            input.getResults().clear();
            input.getResults().addAll(newResults);
            newResults.clear();
        }
        return input;
    }
}
