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
package ddf.catalog.security.audit;

import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import ddf.security.common.audit.SecurityLogger;
import org.apache.shiro.subject.Subject;

/**
 * Security-based plugin that Audits Resource Retrieval requests.
 */
public class RetrieveResourceAuditPlugin implements PreResourcePlugin, PostResourcePlugin {

    @Override
    public ResourceRequest process(ResourceRequest input)
            throws PluginExecutionException, StopProcessingException {
        if (input != null) {
            Object subjectObj = input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj instanceof Subject) {
                String username = SubjectUtils.getName((Subject) subjectObj);
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
                String username = SubjectUtils.getName((Subject) subjectObj);
                String msg =
                        "User [" + username + "] successfully retrieved resource [" + input
                                .getRequest()
                                .getAttributeValue()
                                + "] by [" + input.getRequest().getAttributeName()
                                + "] with file name [" + input.getResource().getName() + "].";
                SecurityLogger.logInfo(msg);
            }
        }
        return input;
    }

}
