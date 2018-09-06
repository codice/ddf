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
package org.codice.ddf.catalog.ui.query.monitor.email;

import static org.terracotta.modules.ehcache.ToolkitInstanceFactoryImpl.LOGGER;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SubjectIdentity;
import java.util.Objects;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.security.AccessControlUtil;
import org.opengis.filter.And;

public class SubscribedWorkspaceNotifier implements PostQueryPlugin, PostFederatedQueryPlugin {

  private static SubjectIdentity subjectIdentity;

  private static EmailNotifier emailNotifierService;

  private static FilterBuilder filterBuilder;

  private static CatalogFramework catalogFramework;

  @SuppressWarnings("unused")
  public void setSubjectIdentity(SubjectIdentity subjectIdentity) {
    this.subjectIdentity = subjectIdentity;
  }

  @SuppressWarnings("unused")
  public void setEmailNotifierService(EmailNotifier emailNotifierService) {
    this.emailNotifierService = emailNotifierService;
  }

  @SuppressWarnings("unused")
  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  @SuppressWarnings("unused")
  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  @Override
  public QueryResponse process(QueryResponse queryResponse) throws PluginExecutionException {
    if (queryResponse == null) {
      throw new PluginExecutionException("Cannot process null queryResponse");
    }
    if (!queryResponse.getResults().isEmpty()) {
      String queryId = (String) queryResponse.getRequest().getProperties().get("id");
      if (queryId != null) {
        Metacard metacard = getWorkspaceMetacard(queryId);
        WorkspaceMetacardImpl workspaceMetacard = WorkspaceMetacardImpl.from(metacard);
        // if (WorkspaceMetacardImpl.isWorkspaceMetacard(workspaceMetacard)) {
        String queryIssuerEmail = subjectIdentity.getUniqueIdentifier(SecurityUtils.getSubject());
        String workspaceOwnerEmail = AccessControlUtil.getOwner(workspaceMetacard);
        if (workspaceOwnerEmail.equals(queryIssuerEmail)) {
          emailNotifierService.sendEmailsForWorkspace(workspaceMetacard, 1L);
        }
        // }
      }
    }
    return queryResponse;
  }

  private Metacard getWorkspaceMetacard(String queryId) {
    Query query = new QueryImpl(getWorkspaceQueryFilters(queryId));
    QueryRequest queryRequest = new QueryRequestImpl(query);

    Metacard workspaceMetacard = null;
    try {
      QueryResponse queryResponse = catalogFramework.query(queryRequest);
      workspaceMetacard =
          queryResponse
              .getResults()
              .stream()
              .map(Result::getMetacard)
              .filter(Objects::nonNull)
              .findFirst()
              .orElse(null);

    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      LOGGER.debug("Error querying for query id: {}.", queryId, e);
    }
    return workspaceMetacard;
  }

  private And getWorkspaceQueryFilters(String queryId) {
    return filterBuilder.allOf(
        filterBuilder.attribute(Core.METACARD_TAGS).is().text(WorkspaceConstants.WORKSPACE_TAG),
        filterBuilder.attribute(WorkspaceConstants.WORKSPACE_QUERIES).is().text(queryId));
  }
}
