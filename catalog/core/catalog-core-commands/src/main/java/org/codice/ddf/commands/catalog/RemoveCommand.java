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

import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.slf4j.LoggerFactory;

/** Deletes records by ID. */
@Service
@Command(
  scope = CatalogCommands.NAMESPACE,
  name = "remove",
  description = "Deletes records from the Catalog."
)
public class RemoveCommand extends CqlCommands {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemoveCommand.class);

  private static final String IDS_LIST_ARGUMENT_NAME = "IDs";

  @Argument(
    name = IDS_LIST_ARGUMENT_NAME,
    description = "The id(s) of the document(s) by which to filter.",
    index = 0,
    multiValued = true,
    required = false
  )
  List<String> ids = null;

  @Option(
    name = "--cache",
    required = false,
    multiValued = false,
    description = "Only remove cached entries."
  )
  boolean cache = false;

  @Override
  protected Object executeWithSubject() throws Exception {
    if (CollectionUtils.isEmpty(ids) && !hasFilter()) {
      printErrorMessage(
          "Nothing to remove. Either the "
              + IDS_LIST_ARGUMENT_NAME
              + " argument or another filter must be specified. Please, use the catalog:removeall command if the goal is to delete all records from the Catalog.");
      return null;
    }

    if (this.cache) {
      return executeRemoveFromCache();
    } else {
      return executeRemoveFromStore();
    }
  }

  private Object executeRemoveFromCache() throws Exception {
    String[] idsArray = new String[ids.size()];
    idsArray = ids.toArray(idsArray);
    getCacheProxy().removeById(idsArray);

    List idsList = Arrays.asList(ids);

    printSuccessMessage(idsList + " successfully removed from cache.");

    LOGGER.info(idsList + " removed from cache using catalog:remove command");

    return null;
  }

  private Object executeRemoveFromStore() throws Exception {
    CatalogFacade catalogProvider = getCatalog();

    if (hasFilter()) {
      QueryImpl query = new QueryImpl(getFilter());

      query.setRequestsTotalResultsCount(true);
      query.setPageSize(-1);

      Map<String, Serializable> properties = new HashMap<>();
      properties.put("mode", "native");

      SourceResponse queryResponse = catalogProvider.query(new QueryRequestImpl(query, properties));

      final List<String> idsFromFilteredQuery =
          queryResponse
              .getResults()
              .stream()
              .map(result -> result.getMetacard().getId())
              .collect(Collectors.toList());

      if (ids == null) {
        ids = idsFromFilteredQuery;
      } else {
        ids =
            ids.stream()
                .filter(id -> idsFromFilteredQuery.contains(id))
                .collect(Collectors.toList());
      }
    }

    final int numberOfMetacardsToRemove = ids.size();
    if (numberOfMetacardsToRemove > 0) {
      printSuccessMessage("Found " + numberOfMetacardsToRemove + " metacards to remove.");
    } else {
      printErrorMessage("No records found meeting filter criteria.");
      return null;
    }

    DeleteRequestImpl request =
        new DeleteRequestImpl(ids.toArray(new String[numberOfMetacardsToRemove]));
    DeleteResponse response = catalogProvider.delete(request);

    if (response.getDeletedMetacards().size() > 0) {
      printSuccessMessage(ids + " successfully deleted.");
      LOGGER.info(ids + " removed using catalog:remove command");
    } else {
      printErrorMessage(ids + " could not be deleted.");
      LOGGER.info(ids + " could not be deleted using catalog:remove command");
    }

    return null;
  }
}
