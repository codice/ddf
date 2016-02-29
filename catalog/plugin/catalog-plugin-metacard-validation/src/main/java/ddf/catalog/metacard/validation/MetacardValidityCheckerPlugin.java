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

package ddf.catalog.metacard.validation;

import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_ERRORS;
import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_WARNINGS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.UnsupportedQueryException;

public class MetacardValidityCheckerPlugin implements PreQueryPlugin {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetacardValidityCheckerPlugin.class);

    protected final FilterBuilder filterBuilder;

    protected final FilterAdapter filterAdapter;

    private boolean showInvalidMetacards;

    public MetacardValidityCheckerPlugin(FilterBuilder filterBuilder, FilterAdapter filterAdapter) {
        this.filterBuilder = filterBuilder;
        this.filterAdapter = filterAdapter;
    }

    @Override
    public QueryRequest process(QueryRequest input)
            throws PluginExecutionException, StopProcessingException {
        QueryRequest queryRequest;
        try {
            if (!showInvalidMetacards && !filterAdapter.adapt(input.getQuery(),
                    new ValidationQueryDelegate())) {
                QueryImpl query = new QueryImpl(filterBuilder.allOf(input.getQuery(),
                        filterBuilder.attribute(VALIDATION_ERRORS)
                                .is()
                                .empty(),
                        filterBuilder.attribute(VALIDATION_WARNINGS)
                                .is()
                                .empty()));
                queryRequest = new QueryRequestImpl(query,
                        input.isEnterprise(),
                        input.getSourceIds(),
                        input.getProperties());
            } else {
                // return the existing query with invalid metacards
                queryRequest = input;
            }
        } catch (UnsupportedQueryException e) {
            LOGGER.debug("This attribute filter is not supported by ValidationQueryDelegate.", e);
            throw new StopProcessingException(e.getMessage());
        }
        return queryRequest;
    }

    public boolean getShowInvalidMetacards() {
        return showInvalidMetacards;
    }

    public void setShowInvalidMetacards(boolean showInvalidMetacards) {
        this.showInvalidMetacards = showInvalidMetacards;
    }
}
