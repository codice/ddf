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

package org.codice.ddf.commands.cache;

import java.util.Arrays;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

@Command(scope = CacheCommands.NAMESPACE, name = "remove", description = "Deletes a metacard from the cache.")
public class RemoveCommand extends CacheCommands {

    @Argument(name = "IDs", description = "The id(s) of the metacard(s) (space delimited) to be deleted from the cache.", index = 0, multiValued = true, required = false)
    String[] ids = null;

    @Override
    protected Object doExecute() throws Exception {

        if (ids == null) {
            printErrorMessage("Nothing to remove.");
        }

        getCacheProxy().removeById(ids);

        printSuccessMessage(Arrays.asList(ids) + " successfully removed from cache.");

        return null;
    }

}
