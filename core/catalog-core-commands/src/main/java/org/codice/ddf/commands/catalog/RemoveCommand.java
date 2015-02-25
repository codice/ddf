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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;

import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.filter.Filter;

/**
 * Deletes records by ID.
 * 
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "remove", description = "Deletes a record from the Catalog.")
public class RemoveCommand extends CatalogCommands {

    @Argument(name = "IDs", description = "The id(s) of the document(s) (space delimited) to be deleted.", index = 0, multiValued = true, required = false)
    List<String> ids = null;

    @Option(name = "--cql", required = false, aliases = {}, multiValued = false, description =
            "Remove Metacards that match a CQL Filter expressions. It is recommended to use the search command first to see which metacards will be removed.\n"
                    + "CQL Examples:\n"
                    + "\tTextual:   search --cql \"title like 'some text'\"\n"
                    + "\tTemporal:  search --cql \"modified before 2012-09-01T12:30:00Z\"\n"
                    + "\tSpatial:   search --cql \"DWITHIN(location, POINT (1 2) , 10, kilometers)\"\n"
                    + "\tComplex:   search --cql \"title like 'some text' AND modified before 2012-09-01T12:30:00Z\"")
    String cqlFilter = null;

    @Override
    protected Object doExecute() throws Exception {

        CatalogFacade catalogProvider = getCatalog();

        if (cqlFilter != null) {
            Filter filter = null;
            filter = CQL.toFilter(cqlFilter);

            QueryImpl query = new QueryImpl(filter);

            query.setRequestsTotalResultsCount(true);
            query.setPageSize(-1);

            Map<String, Serializable> properties = new HashMap<>();
            properties.put("mode", "native");

            SourceResponse queryResponse = catalogProvider.query(new QueryRequestImpl(query,
                    properties));

            if (queryResponse.getResults().isEmpty()) {
                printErrorMessage("No records found using CQL expression.");
                return null;
            }
            printSuccessMessage("Found " + queryResponse.getResults().size() + " metacards to remove.");
            ids = new ArrayList<String>();
            for (Result result : queryResponse.getResults()) {
                ids.add(result.getMetacard().getId());
            }

        }

        if (ids == null) {
            printErrorMessage("Nothing to remove.");
            return null;
        }

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
