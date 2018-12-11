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
package ddf.migration.workspace.query.separation;

import static org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants.WORKSPACE_QUERIES;
import static org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants.WORKSPACE_TAG;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.util.impl.ResultIterable;
import ddf.migration.api.DataMigratable;
import ddf.security.Subject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.codice.ddf.security.common.Security;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceQueryMigration implements DataMigratable {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceQueryMigration.class);

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private final InputTransformer xmlInputTransformer;

  private static final Security SECURITY = Security.getInstance();

  public WorkspaceQueryMigration(
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      InputTransformer xmlInputTransformer) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.xmlInputTransformer = xmlInputTransformer;
  }

  @Override
  public void migrate() {
    Subject systemSubject = SECURITY.runAsAdmin(SECURITY::getSystemSubject);

    if (systemSubject == null) {
      LOGGER.debug("An error occurred while attempting to run this migration as admin");
      return;
    }

    systemSubject.execute(this::migrateWorkspaces);
  }

  @VisibleForTesting
  void migrateWorkspaces() {
    LOGGER.info("Beginning workspace query data migration");
    System.out.println("Started: Workspace Query Data Migration");

    Filter workspaceFilter =
        filterBuilder.allOf(
            filterBuilder.attribute(Core.METACARD_TAGS).like().text(WORKSPACE_TAG),
            filterBuilder.attribute(WORKSPACE_QUERIES).is().like().text("*"));

    Query query = new QueryImpl(workspaceFilter);
    QueryRequest queryRequest = new QueryRequestImpl(query);

    ResultIterable.resultIterable(catalogFramework::query, queryRequest)
        .stream()
        .map(Result::getMetacard)
        .filter(Objects::nonNull)
        .forEach(this::migrateWorkspaceMetacard);

    LOGGER.info("Completed workspace query data migration");
    System.out.println("Completed: Workspace Query Data Migration");
  }

  private void migrateWorkspaceMetacard(Metacard workspaceMetacard) {
    LOGGER.info("Migrating workspace metacard with id [{}]", workspaceMetacard.getId());

    final Attribute queriesAttribute = workspaceMetacard.getAttribute(WORKSPACE_QUERIES);
    final List<Serializable> queriesAsXml = queriesAttribute.getValues();

    LOGGER.info(
        "Beginning migration of {} query metacards from workspace [{}]",
        queriesAsXml.size(),
        workspaceMetacard.getId());

    List<Metacard> queryMetacards =
        queriesAsXml
            .stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(xml -> xml.getBytes(StandardCharsets.UTF_8))
            .map(ByteArrayInputStream::new)
            .map(this::xmlToMetacard)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    LOGGER.info("Creating query metacards for workspace [{}]", workspaceMetacard.getId());

    CreateResponse response = createMetacards(queryMetacards);

    if (response != null) {
      List<String> createdMetacardIds =
          response.getCreatedMetacards().stream().map(Metacard::getId).collect(Collectors.toList());

      LOGGER.info("Created query metacards with IDs {}", createdMetacardIds);

      workspaceMetacard.setAttribute(
          new AttributeImpl(WORKSPACE_QUERIES, (Serializable) createdMetacardIds));

      updateMetacard(workspaceMetacard);

      LOGGER.info(
          "Updated workspace metacard [{}] with query metacard associations {}",
          workspaceMetacard.getId(),
          createdMetacardIds);
    } else {
      LOGGER.debug("An error occurred while creating query metacards");
    }
  }

  private UpdateResponse updateMetacard(Metacard metacard) {
    return AccessController.doPrivileged(
        (PrivilegedAction<UpdateResponse>)
            () -> {
              try {
                return catalogFramework.update(new UpdateRequestImpl(metacard.getId(), metacard));
              } catch (IngestException | SourceUnavailableException e) {
                LOGGER.debug("Error updating workspace metacard [{}]", metacard.getId(), e);
              }
              return null;
            });
  }

  private CreateResponse createMetacards(List<Metacard> metacards) {
    return AccessController.doPrivileged(
        (PrivilegedAction<CreateResponse>)
            () -> {
              try {
                return catalogFramework.create(new CreateRequestImpl(metacards));
              } catch (IngestException | SourceUnavailableException e) {
                LOGGER.debug("Error while creating query metacards {}", metacards, e);
              }
              return null;
            });
  }

  private Metacard xmlToMetacard(InputStream inputStream) {
    try {
      Metacard metacard = xmlInputTransformer.transform(inputStream);
      metacard.setAttribute(new AttributeImpl(Core.METACARD_TAGS, "query"));

      LOGGER.info("Successfully parsed query metacard with id [{}]", metacard.getId());

      return metacard;
    } catch (IOException | CatalogTransformerException e) {
      LOGGER.debug("Error parsing query metacard input stream", e);

      return null;
    } finally {
      try {
        inputStream.close();
      } catch (IOException e) {
        LOGGER.debug("An error occurred while closing the input stream", e);
      }
    }
  }
}
