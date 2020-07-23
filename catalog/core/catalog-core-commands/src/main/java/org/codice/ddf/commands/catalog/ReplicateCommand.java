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

import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
    scope = CatalogCommands.NAMESPACE,
    name = "replicate",
    description = "Replicates Metacards from a Federated Source into the Catalog.")
public class ReplicateCommand extends DuplicateCommands {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReplicateCommand.class);

  @Argument(
      name = "Source Id",
      description = "The ID of the Source to replicate the data from.",
      index = 0,
      multiValued = false,
      required = false)
  String sourceId;

  @Override
  protected Object executeWithSubject() throws Exception {
    if (batchSize > MAX_BATCH_SIZE || batchSize < 1) {
      console.println("Batch Size must be between 1 and " + MAX_BATCH_SIZE + ".");
      return null;
    }

    final CatalogFacade catalog = getCatalog();
    final CatalogFacade framework = new Framework(catalogFramework);
    Set<String> sourceIds = framework.getSourceIds();

    while (true) {
      if (StringUtils.isBlank(sourceId) || !sourceIds.contains(sourceId)) {
        console.println("Please enter the Source ID you would like to replicate:");
        for (String id : sourceIds) {
          console.println("\t" + id);
        }
      } else {
        break;
      }
      sourceId = session.readLine("ID:  ", null);
    }

    start = System.currentTimeMillis();

    console.println("Starting replication.");

    duplicateInBatches(framework, catalog, getFilter(), sourceId);

    console.println();
    long end = System.currentTimeMillis();
    String completed =
        String.format(
            " %d record(s) replicated; %d record(s) failed; completed in %3.3f seconds.",
            ingestedCount.get(), failedCount.get(), (end - start) / MS_PER_SECOND);
    LOGGER.debug("Replication Complete: {}", completed);
    console.println(completed);

    return null;
  }
}
