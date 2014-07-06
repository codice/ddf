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
package org.codice.ddf.commands.catalog;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;

import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;

/**
 * Deletes records by ID.
 * 
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "remove", description = "Deletes a record from the Catalog.")
public class RemoveCommand extends CatalogCommands {

    @Argument(name = "IDs", description = "The id(s) of the document(s) (space delimited) to be deleted.", index = 0, multiValued = true, required = true)
    List<String> ids = null;

    @Override
    protected Object doExecute() throws Exception {

        if (ids == null) {
            return null;
        }

        CatalogFacade catalogProvider = getCatalog();

        DeleteRequestImpl request = new DeleteRequestImpl(ids.toArray(new String[0]));

        DeleteResponse response = catalogProvider.delete(request);

        if (response.getDeletedMetacards().size() > 0) {
            printSuccessMessage(ids + " successfully deleted.");
        } else {
            printErrorMessage(ids + " could not be deleted.");
        }

        return null;

    }
}
