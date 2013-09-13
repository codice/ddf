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
package ddf.sdk.plugin.postquery;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.MetacardValidator;

/**
 * Validates the query results from a query as a PostQueryPLugin
 * 
 * @author Shaun Morris, Lockheed Martin
 * 
 */
public class DummyValidationPostQueryPlugin implements PostQueryPlugin {
    private static Logger LOGGER = LoggerFactory.getLogger(DummyValidationPostQueryPlugin.class
            .getName());

    private MetacardValidator validator;

    public DummyValidationPostQueryPlugin(MetacardValidator validator) {
        this.validator = validator;
    }

    public QueryResponse process(QueryResponse input) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "process()";
        LOGGER.debug("ENTERING: " + methodName);

        if (input != null) {
            List<Result> cards = input.getResults();

            // Validate each metacard in the results
            for (Result card : cards) {
                // Catch validation errors and warnings on each card
                try {
                    LOGGER.debug("validating card {}", card.getMetacard().getId());
                    validator.validate(card.getMetacard());
                } catch (ValidationException e) {
                    LOGGER.error(e.getMessage());

                    LOGGER.info("Errors: {}", e.getErrors());
                    LOGGER.info("Warnings: {}", e.getWarnings());
                }
            }
        }

        LOGGER.debug("EXITING: " + methodName);

        return input;
    }

}
