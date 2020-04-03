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
package org.codice.ddf.persistence.commands;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.codice.ddf.persistence.PersistenceException;

@Service
@Command(
    scope = "store",
    name = "delete",
    description = "Deletes entries from the persistence store.")
public class StoreDeleteCommand extends AbstractStoreCommand {

  @Reference Session session;

  @Override
  public void storeCommand() throws PersistenceException {

    List<Map<String, Object>> results = persistentStore.get(type, cql);
    if (!results.isEmpty()) {
      console.println(results.size() + " results matched cql.");
      String message = "\nAre you sure you want to delete? (yes/no): ";
      while (true) {
        try {
          String confirmation = session.readLine(message, null);
          if ("yes".equalsIgnoreCase(confirmation.toLowerCase())) {
            int numDeleted = persistentStore.delete(type, cql);
            console.println("Successfully deleted " + numDeleted + " items.");
            break;
          } else if ("no".equalsIgnoreCase(confirmation)) {
            console.println("Delete canceled. No entries were deleted.");
            break;
          }
        } catch (IOException ioe) {
          break;
        }
      }

    } else {
      console.println("0 results matched cql statement. No items were deleted.");
    }
  }
}
