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
package org.codice.ddf.registry.identification;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import java.security.PrivilegedActionException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.codice.ddf.security.common.Security;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The RegistryIdRetriever collects a set of registry-ids that are currently in the system and makes
 * them available to the IdentificationPlugin for duplication checking.
 *
 * <p>Initially the catalog is queried to initialize the set of registry-ids. After that the list is
 * maintained by the process methods for create and delete responses.
 */
public class RegistryIdRetriever {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegistryIdRetriever.class);

  private static final int PAGE_SIZE = 1000;

  private CatalogFramework catalogFramework;

  private FilterBuilder filterBuilder;

  private Security security;

  public RegistryIdRetriever() {
    security = Security.getInstance();
  }

  @VisibleForTesting
  public RegistryIdRetriever(Security security) {
    this.security = security;
  }

  public RegistryIdInfo getRegistryIdInfo() {
    try {
      List<Metacard> registryMetacards;
      Filter registryFilter =
          filterBuilder.anyOf(
              filterBuilder
                  .attribute(Metacard.TAGS)
                  .is()
                  .equalTo()
                  .text(RegistryConstants.REGISTRY_TAG),
              filterBuilder
                  .attribute(Metacard.TAGS)
                  .is()
                  .equalTo()
                  .text(RegistryConstants.REGISTRY_TAG_INTERNAL));
      QueryImpl query = new QueryImpl(registryFilter);
      query.setPageSize(PAGE_SIZE);
      QueryRequest request = new QueryRequestImpl(query);

      QueryResponse response =
          security.runAsAdminWithException(
              () -> security.runWithSubjectOrElevate(() -> catalogFramework.query(request)));

      if (response == null) {
        throw new PluginExecutionException(
            "Failed to initialize RegistryIdRetriever. Query for registry metacards came back null");
      }

      registryMetacards =
          response.getResults().stream().map(Result::getMetacard).collect(Collectors.toList());

      return new RegistryIdInfo(registryMetacards);
    } catch (PrivilegedActionException | PluginExecutionException e) {
      LOGGER.debug("Error getting registry metacards.");
    }
    return null;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  public void setCatalogFramework(CatalogFramework framework) {
    this.catalogFramework = framework;
  }

  static class RegistryIdInfo {
    private Set<String> registryIds = new HashSet<>();

    private Set<String> localRegistryIds = new HashSet<>();

    private Set<String> remoteMetacardIds = new HashSet<>();

    public RegistryIdInfo(List<Metacard> metacards) {
      for (Metacard mcard : metacards) {
        if (RegistryUtility.isRegistryMetacard(mcard)) {
          registryIds.add(RegistryUtility.getRegistryId(mcard));
          if (RegistryUtility.isLocalNode(mcard)) {
            localRegistryIds.add(RegistryUtility.getRegistryId(mcard));
          }
        } else if (RegistryUtility.isInternalRegistryMetacard(mcard)) {
          remoteMetacardIds.add(
              RegistryUtility.getStringAttribute(
                  mcard, RegistryObjectMetacardType.REMOTE_METACARD_ID, ""));
        }
      }
    }

    public Set<String> getRegistryIds() {
      return registryIds;
    }

    public Set<String> getRemoteMetacardIds() {
      return remoteMetacardIds;
    }

    public Set<String> getLocalRegistryIds() {
      return localRegistryIds;
    }
  }
}
