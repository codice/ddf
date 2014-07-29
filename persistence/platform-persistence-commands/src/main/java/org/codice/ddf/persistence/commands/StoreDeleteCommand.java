/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.persistence.commands;

import jline.console.ConsoleReader;
import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.persistence.PersistenceException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Command(scope = "store", name = "delete",
        description = "Deletes entries from the persistence store.")
public class StoreDeleteCommand extends AbstractStoreCommand {

    @Override
    public void storeCommand() throws PersistenceException {

        List<Map<String, Object>> results = persistentStore.get(type, cql);
        if (!results.isEmpty()) {
            console.println(results.size() + " results matched cql.");
            String message = "\nAre you sure you want to delete? (yes/no): ";
            ConsoleReader reader = (ConsoleReader) session.get(".jline.reader");
            while (true) {
                try {
                    String confirmation = reader.readLine(message);
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
