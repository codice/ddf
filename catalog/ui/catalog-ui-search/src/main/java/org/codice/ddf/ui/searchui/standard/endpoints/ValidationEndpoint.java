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
package org.codice.ddf.ui.searchui.standard.endpoints;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.boon.json.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;

@Path("/validate")
public class ValidationEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationEndpoint.class);


    private final AttributeValidatorRegistry attributeValidatorRegistry;

    private final EndpointUtil endpointUtil;

    public ValidationEndpoint(AttributeValidatorRegistry attributeValidatorRegistry, EndpointUtil endpointUtil) {
        this.attributeValidatorRegistry = attributeValidatorRegistry;
        this.endpointUtil = endpointUtil;
    }

    @PUT
    @Path("/attribute/{attribute}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response validateAttribute(@Context HttpServletResponse response,
            @PathParam("attribute") String attribute, String value) throws Exception {
        // TODO (RCZ) - Get dem attribute validators and run 'em
        Set<AttributeValidator> validators = attributeValidatorRegistry.getValidators(attribute);
        Set<String> suggestedValues = new HashSet<>();
        Set<ValidationViolation> violations = new HashSet<>();
        for (AttributeValidator validator : validators) {
            Optional<AttributeValidationReport> validationReport =
                    validator.validate(new AttributeImpl(attribute, value));
            if (validationReport.isPresent()) {
                AttributeValidationReport report = validationReport.get();
                if (!report.getSuggestedValues()
                        .isEmpty()) {
                    suggestedValues = report.getSuggestedValues();
                }
                violations.addAll(report.getAttributeValidationViolations());
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("violations", violations);
        resultMap.put("suggested-values", suggestedValues);
        return Response.ok(JsonFactory.create().toJson(resultMap)).build();
    }

    @PUT
    @Path("/metacard/{type}")
    public Response validateMetacard(@Context HttpServletResponse response,
            @PathParam("type") String type) {
        // TODO (RCZ) - metacard level validators.. just validate everything?

        return null;
    }
}
