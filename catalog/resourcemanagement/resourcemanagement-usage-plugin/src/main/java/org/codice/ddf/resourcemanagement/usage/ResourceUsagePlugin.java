/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.resourcemanagement.usage;

import ddf.catalog.Constants;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.util.impl.Requests;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUsagePlugin implements PreResourcePlugin, PostResourcePlugin {

  private boolean monitorLocalSources;

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUsagePlugin.class);

  private AttributesStore attributesStore;

  public ResourceUsagePlugin(AttributesStore attributesStore) {
    this.attributesStore = attributesStore;
  }

  @Override
  public ResourceRequest process(ResourceRequest input)
      throws PluginExecutionException, StopProcessingException {
    long resourceSize = 0L;

    if (input != null && (Requests.isEnterprise(input) || monitorLocalSources)) {
      Object sizeObj = input.getPropertyValue(Core.RESOURCE_SIZE);
      if (sizeObj instanceof String) {
        try {
          LOGGER.debug("resource-size: {} bytes ", (String) sizeObj);
          resourceSize = Long.parseLong((String) sizeObj);
        } catch (NumberFormatException nfe) {
          LOGGER.debug("Unable to parse {} into long.  Ignoring resource size.", (String) sizeObj);
        }

        if (resourceSize > 0) {
          LOGGER.debug(
              "Resource request for {}:{}.", input.getAttributeName(), input.getAttributeValue());

          String username =
              getUsernameFromSubject(input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT));
          if (StringUtils.isNotEmpty(username)) {
            try {
              long currentUserDataUsage = attributesStore.getCurrentDataUsageByUser(username);
              long userDataLimit = attributesStore.getDataLimitByUser(username);
              if ((userDataLimit >= 0) && ((currentUserDataUsage + resourceSize) > userDataLimit)) {
                throw new DataUsageLimitExceededException(
                    username + ": data usage limit exceeded. (" + userDataLimit + " bytes)");
              }
            } catch (PersistenceException pex) {
              LOGGER.debug("Persistence exception updating user {} data usage", username, pex);
            }
          }
        }
      }
    }
    return input;
  }

  @Override
  public ResourceResponse process(ResourceResponse input)
      throws PluginExecutionException, StopProcessingException {
    long resourceSize = 0L;
    if (input != null && (isEnterprise(input) || monitorLocalSources)) {
      Object sizeObj = input.getPropertyValue(Core.RESOURCE_SIZE);
      if (sizeObj instanceof String) {
        try {
          resourceSize = Long.parseLong((String) sizeObj);
        } catch (NumberFormatException nfe) {
          LOGGER.debug("Unable to parse {} into long.  Ignoring resource size.", (String) sizeObj);
        }

        if (resourceSize > 0) {

          String username =
              getUsernameFromSubject(input.getPropertyValue(SecurityConstants.SECURITY_SUBJECT));

          if (StringUtils.isNotEmpty(username)) {

            try {
              attributesStore.updateUserDataUsage(username, resourceSize);
            } catch (PersistenceException pex) {
              LOGGER.debug("Persistence exception updating user {} data usage", username, pex);
            }
          }
        }
      }
    }
    return input;
  }

  private boolean isEnterprise(Response response) {
    return response.hasProperties()
        && response.getPropertyValue(Constants.REMOTE_DESTINATION_KEY) != null
        && (boolean) response.getPropertyValue(Constants.REMOTE_DESTINATION_KEY);
  }

  public void setMonitorLocalSources(boolean monitorLocalSources) {
    this.monitorLocalSources = monitorLocalSources;
  }

  public boolean getMonitorLocalSources() {
    return this.monitorLocalSources;
  }

  private String getUsernameFromSubject(Object subjectObj) {
    String username = null;
    if (subjectObj instanceof Subject) {
      username = SubjectUtils.getName((Subject) subjectObj, null, true);
    }
    return username;
  }
}
