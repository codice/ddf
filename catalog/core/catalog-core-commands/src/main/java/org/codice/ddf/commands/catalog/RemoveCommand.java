/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.filter.Filter;
import org.slf4j.LoggerFactory;

import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;

/**
 * Deletes records by ID.
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "remove", description = "Deletes a record from the Catalog.")
public class RemoveCommand extends CatalogCommands {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemoveCommand.class);

    @Argument(name = "IDs", description = "The id(s) of the document(s) (space delimited) to be deleted.", index = 0, multiValued = true, required = false)
    List<String> ids = null;

    @Option(name = "--cql", required = false, aliases = {}, multiValued = false, description =
            "Remove Metacards that match a CQL Filter expressions. It is recommended to use the search command first to see which metacards will be removed.\n"
                    + "CQL Examples:\n" + "\tTextual:   search --cql \"title like 'some text'\"\n"
                    + "\tTemporal:  search --cql \"modified before 2012-09-01T12:30:00Z\"\n"
                    + "\tSpatial:   search --cql \"DWITHIN(location, POINT (1 2) , 10, kilometers)\"\n"
                    + "\tComplex:   search --cql \"title like 'some text' AND modified before 2012-09-01T12:30:00Z\"")
    String cqlFilter = null;

    @Option(name = "--cache", required = false, multiValued = false, description = "Only remove cached entries.")
    boolean cache = false;

    @Override
    protected Object executeWithSubject() throws Exception {
        if (CollectionUtils.isEmpty(ids) && cqlFilter == null) {
            printErrorMessage("Nothing to remove.");
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

        if (cqlFilter != null) {
            Filter filter = CQL.toFilter(cqlFilter);

            QueryImpl query = new QueryImpl(filter);

            query.setRequestsTotalResultsCount(true);
            query.setPageSize(-1);

            Map<String, Serializable> properties = new HashMap<>();
            properties.put("mode", "native");

            SourceResponse queryResponse = catalogProvider.query(new QueryRequestImpl(query,
                    properties));

            if (queryResponse.getResults()
                    .isEmpty()) {
                printErrorMessage("No records found using CQL expression.");
                return null;
            }

            List<String> tmpIds = new ArrayList<>();
            if (ids != null) {
                tmpIds.addAll(ids);
            }
            tmpIds.addAll(queryResponse.getResults()
                    .stream()
                    .map(result -> result.getMetacard()
                            .getId())
                    .collect(Collectors.toList()));
            ids = tmpIds;
        }

        printSuccessMessage("Found " + ids.size() + " metacards to remove.");

        DeleteRequestImpl request = new DeleteRequestImpl(ids.toArray(new String[ids.size()]));

        DeleteResponse response = catalogProvider.delete(request);

        if (response.getDeletedMetacards()
                .size() > 0) {
            printSuccessMessage(ids + " successfully deleted.");
            LOGGER.info(ids + " removed using catalog:remove command");
        } else {
            printErrorMessage(ids + " could not be deleted.");
            LOGGER.error(ids + " could not be deleted using catalog:remove command");
        }

        return null;
    }
}
