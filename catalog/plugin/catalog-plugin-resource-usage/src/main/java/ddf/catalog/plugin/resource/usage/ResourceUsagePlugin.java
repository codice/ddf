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
package ddf.catalog.plugin.resource.usage;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectUtils;

public class ResourceUsagePlugin implements PreResourcePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUsagePlugin.class);

    private AttributesStore attributesStore;

    public ResourceUsagePlugin(AttributesStore attributesStore) {
        this.attributesStore = attributesStore;
    }

    @Override
    public ResourceRequest process(ResourceRequest input)
            throws PluginExecutionException, StopProcessingException {
        long resourceSize = 0L;

        if (input != null) {

            Object sizeObj = input.getPropertyValue(Metacard.RESOURCE_SIZE);
            if (sizeObj != null && sizeObj instanceof String) {
                try {
                    LOGGER.debug("resource-size: {} bytes ", (String) sizeObj);
                    resourceSize = Long.parseLong((String) sizeObj);
                } catch (NumberFormatException nfe) {
                    LOGGER.info("Unable to parse {} into long.  Ignoring resource size.",
                            (String) sizeObj);
                }

                if (resourceSize > 0) {
                    LOGGER.debug("Resource request for {}:{}.",
                            input.getAttributeName(),
                            input.getAttributeValue());

                    String username = getUsernameFromSubject(input.getPropertyValue(
                            SecurityConstants.SECURITY_SUBJECT));

                    if (StringUtils.isNotEmpty(username)) {

                        try {
                            attributesStore.updateUserDataUsage(username, resourceSize);
                        } catch (PersistenceException e) {
                            LOGGER.info("Persistence exception updating user {} data usage",
                                    username,
                                    e);
                        }
                    }
                }
            }

        }

        return input;

    }

    private String getUsernameFromSubject(Object subjectObj) {
        String username = null;
        if (subjectObj instanceof Subject) {
            username = SubjectUtils.getName((Subject) subjectObj, null, true);
        }
        return username;
    }

}
