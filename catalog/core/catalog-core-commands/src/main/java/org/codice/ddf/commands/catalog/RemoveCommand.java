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
package org.codice.ddf.commands.catalog;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.util.impl.ResultIterable;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.util.CatalogCommandException;
import org.geotools.filter.text.cql2.CQLException;
import org.slf4j.LoggerFactory;

/** Deletes records by ID. */
@Service
@Command(
    scope = CatalogCommands.NAMESPACE,
    name = "remove",
    description = "Deletes records from the Catalog.")
public class RemoveCommand extends CqlCommands {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemoveCommand.class);

  private static final String IDS_LIST_ARGUMENT_NAME = "IDs";

  private static final int BATCH_SIZE = 250;

  @Argument(
      name = IDS_LIST_ARGUMENT_NAME,
      description = "The ID(s) of documents to remove",
      index = 0,
      multiValued = true,
      required = false)
  Set<String> ids = new HashSet<>();

  @Option(
      name = "--cache",
      required = false,
      multiValued = false,
      description = "Only remove cached entries.")
  boolean cache = false;

  @Override
  protected Object executeWithSubject() throws Exception {
    if (CollectionUtils.isEmpty(ids) && !hasFilter()) {
      printErrorMessage(
          "No IDs or filter provided"
              + "\nPlease use the catalog:removeall command if the goal is to remove all records from the Catalog.");
      return null;
    }

    if (this.cache) {
      return executeRemoveFromCache();
    } else {
      return executeRemoveFromStore();
    }
  }

  @SuppressWarnings(
      "squid:S00112" /* Non-generic exception would require importing and embedding SolrServerException */)
  private Object executeRemoveFromCache() throws Exception {

    if (hasFilter()) {
      printErrorMessage("Cache does not support filtering. Remove filter parameter.");
      return null;
    }

    if (CollectionUtils.isNotEmpty(ids)) {
      getCacheProxy().removeById(ids.toArray(new String[0]));
      printSuccessMessage(ids + " successfully removed from cache");
      LOGGER.info("{} removed from cache by catalog:remove command", ids);
    }

    return null;
  }

  private int deletedIdsPassedAsArguments() throws IngestException, SourceUnavailableException {

    DeleteRequestImpl deleteRequest = new DeleteRequestImpl(ids.toArray(new String[0]));

    LOGGER.debug("Attempting to delete {} metacards by ID", ids.size());
    DeleteResponse deleteResponse = getCatalog().delete(deleteRequest);
    return deleteResponse.getDeletedMetacards().size();
  }

  private Object executeRemoveFromStore() throws CatalogCommandException {
    try {
      int batchCount = 0;
      int deletedCount = 0;
      if (CollectionUtils.isNotEmpty(ids) && !hasFilter()) {
        deletedCount = deletedIdsPassedAsArguments();
      }

      if (hasFilter()) {
        QueryRequestImpl queryRequest = new QueryRequestImpl(getQuery(), false);
        String[] idsToDelete = getNextQueryBatch(queryRequest);
        while (idsToDelete.length > 0) {
          if (CollectionUtils.isNotEmpty(ids)) {
            idsToDelete =
                Arrays.asList(idsToDelete).stream()
                    .filter(id -> ids.contains(id))
                    .toArray(String[]::new);
          }
          DeleteRequestImpl deleteRequest = new DeleteRequestImpl(idsToDelete);
          LOGGER.debug(
              "Attempting to delete {} metacards from batch {}", idsToDelete.length, ++batchCount);
          DeleteResponse deleteResponse = catalogFramework.delete(deleteRequest);
          deletedCount += deleteResponse.getDeletedMetacards().size();

          idsToDelete = getNextQueryBatch(queryRequest);
        }
      }
      if (deletedCount > 0) {
        printSuccessMessage(deletedCount + " documents successfully deleted.");
        LOGGER.debug("{} documents removed using catalog:remove command", deletedCount);
      } else {
        printErrorMessage("No documents match provided IDs or filter");
        LOGGER.debug("No documents deleted using the catalog:remove command");
      }
    } catch (IngestException | SourceUnavailableException | ParseException | CQLException e) {
      throw new CatalogCommandException("Error executing catalog:remove", e);
    }
    return null;
  }

  private String[] getNextQueryBatch(QueryRequest queryRequest) {
    return ResultIterable.resultIterable(catalogFramework, queryRequest, BATCH_SIZE).stream()
        .filter(Objects::nonNull)
        .map(Result::getMetacard)
        .filter(Objects::nonNull)
        .map(Metacard::getId)
        .distinct()
        .toArray(String[]::new);
  }

  private QueryImpl getQuery() throws ParseException, CQLException {
    QueryImpl query = new QueryImpl(getFilter());
    Map<String, Serializable> properties = new HashMap<>();
    properties.put("mode", "native");
    return query;
  }
}
