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

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.util.impl.ResultIterable;
import ddf.security.SubjectIdentity;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.security.AccessControlUtil;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscribedWorkspaceNotifier implements PostQueryPlugin, PostFederatedQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscribedWorkspaceNotifier.class);

  private SubjectIdentity subjectIdentity;

  private EmailNotifier emailNotifierService;

  private FilterBuilder filterBuilder;

  private CatalogFramework catalogFramework;

  public SubscribedWorkspaceNotifier(
      SubjectIdentity subjectIdentity,
      EmailNotifier emailNotifierService,
      FilterBuilder filterBuilder,
      CatalogFramework catalogFramework) {
    this.subjectIdentity = subjectIdentity;
    this.emailNotifierService = emailNotifierService;
    this.filterBuilder = filterBuilder;
    this.catalogFramework = catalogFramework;
  }

  @Override
  public QueryResponse process(QueryResponse queryResponse) throws PluginExecutionException {
    if (queryResponse == null) {
      throw new PluginExecutionException("Cannot process null queryResponse");
    }
    if (queryResponse.getResults().isEmpty()) {
      return queryResponse;
    }
    String queryId = (String) queryResponse.getRequest().getProperties().get("id");
    if (queryId == null | queryId.isEmpty()) {
      LOGGER.debug("Null or empty query ID.");
      return queryResponse;
    }
    Metacard metacard = getWorkspaceMetacard(queryId);
    if (metacard == null) {
      LOGGER.debug("Could not retrieve workspace metacard.");
      return queryResponse;
    }
    WorkspaceMetacardImpl workspaceMetacard = WorkspaceMetacardImpl.from(metacard);
    String queryIssuerEmail = subjectIdentity.getUniqueIdentifier(SecurityUtils.getSubject());
    String workspaceOwnerEmail = AccessControlUtil.getOwner(workspaceMetacard);
    if (!queryIssuerEmail.equals(workspaceOwnerEmail)) {
      LOGGER.debug("Query was not issued by the owner of the workspace.");
      return queryResponse;
    }
    emailNotifierService.sendEmailsForWorkspace(workspaceMetacard, queryResponse.getHits());
    return queryResponse;
  }

  private Metacard getWorkspaceMetacard(String queryId) {
    Query query = new QueryImpl(getWorkspaceQueryFilters(queryId));
    QueryRequest queryRequest = new QueryRequestImpl(query);
    List<Result> resultList =
        ResultIterable.resultIterable(catalogFramework::query, queryRequest)
            .stream()
            .collect(Collectors.toList());

    return resultList
        .stream()
        .map(Result::getMetacard)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private Filter getWorkspaceQueryFilters(String queryId) {
    return filterBuilder.allOf(
        filterBuilder.attribute(Core.METACARD_TAGS).is().text(WorkspaceConstants.WORKSPACE_TAG),
        filterBuilder.attribute(WorkspaceConstants.WORKSPACE_QUERIES).like().text(queryId));
  }
}
