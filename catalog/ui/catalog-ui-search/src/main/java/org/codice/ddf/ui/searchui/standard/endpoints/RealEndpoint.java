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

import static ddf.catalog.validation.violation.ValidationViolation.Severity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.PATCH;
import org.apache.shiro.SecurityUtils;
import org.boon.json.JsonFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import ddf.security.SubjectUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class RealEndpoint {
    private final CatalogFramework catalogFramework;

    private final ReportingMetacardValidator reportingMetacardValidator;

    private final FilterBuilder filterBuilder;

    private final EndpointUtil endpointUtil;

    private final static int DEFAULT_PAGE_SIZE = 10;

    public RealEndpoint(CatalogFramework catalogFramework,
            ReportingMetacardValidator reportingMetacardValidator, FilterBuilder filterBuilder,
            EndpointUtil endpointUtil) {
        this.catalogFramework = catalogFramework;
        this.reportingMetacardValidator = reportingMetacardValidator;
        this.filterBuilder = filterBuilder;
        this.endpointUtil = endpointUtil;
    }

    @GET
    @Path("/metacardtype")
    public Response getMetacardType() throws Exception {
        Map<String, Object> resultTypes = endpointUtil.getMetacardTypeMap();
        return Response.ok(JsonFactory.create()
                .toJson(resultTypes), MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path("/metacard/{id}")
    public Response getMetacard(@PathParam("id") String id) throws Exception {
        Metacard metacard = endpointUtil.getMetacard(id);
        if (metacard == null) {
            return Response.status(404)
                    .build();
        }

        Map<String, Object> response = endpointUtil.transformToJson(metacard);

        return Response.ok(endpointUtil.getJson(response), MediaType.APPLICATION_JSON)
                .build();
    }

    public class GoldPlatingException extends Exception {
    }

    // TODO (RCZ) - Eventually also make bulk validation endpoint
    @GET
    @Path("/metacard/{id}/validation")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validateMetacard(@PathParam("id") String id) throws Exception {
        Metacard newMetacard = endpointUtil.getMetacard(id);
        MetacardType metacardType = newMetacard.getMetacardType();

        // TODO (RCZ) - don't assume this only service, do for real
        Optional<MetacardValidationReport> metacardValidationReport =
                reportingMetacardValidator.validateMetacard(newMetacard);

        if (metacardValidationReport.isPresent()) {
            Set<ValidationViolation> attributeValidationViolations = metacardValidationReport.get()
                    .getAttributeValidationViolations();

            Map<String, ViolationResult> violationsResult = getViolationsResult(
                    attributeValidationViolations);
            List<ViolationResult> result = violationsResult.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            return Response.ok(endpointUtil.getJson(result), MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.ok("[]", MediaType.APPLICATION_JSON)
                .build();
    }

    @POST
    @Path("/metacards")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetacards(String body) throws Exception {
        List<String> ids = JsonFactory.create()
                .parser()
                .parseList(String.class, body);

        List<Metacard> metacards = endpointUtil.getMetacards(ids, "*")
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .map(Result::getMetacard)
                .collect(Collectors.toList());
        Map<String, Object> response = endpointUtil.transformToJson(metacards);

        return Response.ok(endpointUtil.getJson(response))
                .build();
    }

    @DELETE
    @Path("/metacards")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteMetacards(String body) throws Exception {
        List<String> ids = JsonFactory.create()
                .parser()
                .parseList(String.class, body);
        DeleteResponse deleteResponse = catalogFramework.delete(new DeleteRequestImpl(ids,
                Metacard.ID,
                null));
        if (deleteResponse.getProcessingErrors() != null && !deleteResponse.getProcessingErrors()
                .isEmpty()) {
            return Response.status(500)
                    .build();
        }
        return Response.ok()
                .build();
    }

    @SuppressFBWarnings
    @PATCH
    @Path("/metacards")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response patchMetacards(String body) throws Exception {
        List<MetacardChanges> metacardChanges = JsonFactory.create()
                .parser()
                .parseList(MetacardChanges.class, body);

        Set<String> changedIds = metacardChanges.stream()
                .flatMap(mc -> mc.ids.stream())
                .collect(Collectors.toSet());

        Map<String, Result> results = endpointUtil.getMetacards(changedIds, "*");

        for (MetacardChanges changeset : metacardChanges) {
            for (AttributeChange attributeChange : changeset.attributes) {
                for (String id : changeset.ids) {
                    Result result = results.get(id);
                    if (Optional.ofNullable(result)
                            .map(Result::getMetacard)
                            .map(Metacard::getMetacardType)
                            .map(mt -> mt.getAttributeDescriptor(attributeChange.attribute))
                            .map(AttributeDescriptor::isMultiValued)
                            .orElse(false)) {
                        result.getMetacard()
                                .setAttribute(new AttributeImpl(attributeChange.attribute,
                                        (List<Serializable>) new ArrayList<Serializable>(
                                                attributeChange.values)));
                    } else {
                        result.getMetacard()
                                .setAttribute(new AttributeImpl(attributeChange.attribute,
                                        Collections.singletonList(attributeChange.values.get(0))));
                    }
                }
            }
        }

        List<Metacard> changedMetacards = results.values()
                .stream()
                .map(Result::getMetacard)
                .collect(Collectors.toList());
        UpdateResponse updateResponse =
                catalogFramework.update(new UpdateRequestImpl(changedIds.toArray(new String[0]),
                        changedMetacards));
        // TODO (RCZ) - How do I want to do validation? since we can still update
        if (updateResponse.getProcessingErrors() != null && !updateResponse.getProcessingErrors()
                .isEmpty()) {
            return Response.ok("[{\"errors\":\"There were validation/processing errors\"}]")
                    .build();
        }

        return Response.ok(body)
                .build();
    }

    @GET
    @Path("/recent")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response recentMetacards(@QueryParam("pageSize") int pageSize,
            @QueryParam("pageNumber") int pageNumber) throws Exception {
        pageSize = pageSize == 0 ? DEFAULT_PAGE_SIZE : pageSize;
        pageNumber = pageNumber == 0 ? 1 : pageNumber;
        if (pageNumber <= 0) {
            return Response.status(400)
                    .build();
        }

        // TODO (RCZ) - fix when actually in
        Filter userFilter = filterBuilder.attribute("Metacard.OWNER")
                .is()
                .equalTo()
                .text(SubjectUtils.getEmailAddress(SecurityUtils.getSubject()));

        int startIndex = 1 + ((pageNumber - 1) * pageSize);
        catalogFramework.query(new QueryRequestImpl(new QueryImpl(userFilter,
                startIndex,
                pageSize,
                SortBy.NATURAL_ORDER,
                false,
                TimeUnit.SECONDS.toMillis(10))));

        // TODO (RCZ) - now need to sort and return results.

        return Response.status(999)
                .build();
    }

    private Map<String, ViolationResult> getViolationsResult(
            Set<ValidationViolation> attributeValidationViolations) {
        Map<String, ViolationResult> violationsResult = new HashMap<>();
        for (ValidationViolation violation : attributeValidationViolations) {
            for (String attribute : violation.getAttributes()) {
                if (!violationsResult.containsKey(attribute)) {
                    violationsResult.put(attribute, new ViolationResult());
                }
                ViolationResult violationResponse = violationsResult.get(attribute);
                violationResponse.attribute = attribute;

                if (Severity.ERROR.equals(violation.getSeverity())) {
                    violationResponse.errors.add(violation.getMessage());
                } else if (Severity.WARNING.equals(violation.getSeverity())) {
                    violationResponse.warnings.add(violation.getMessage());
                } else {
                    throw new RuntimeException("Unexpected Severity Level");
                }
            }
        }
        return violationsResult;
    }

    private List<Serializable> getSerializableList(List list) {
        return new ArrayList<>(list);
    }

    private static class ViolationResult {
        String attribute;

        List<String> errors = new ArrayList<>();

        List<String> warnings = new ArrayList<>();
    }

    private static class MetacardChanges {
        List<String> ids;

        List<AttributeChange> attributes;
    }

    private static class AttributeChange {
        String attribute;

        List<String> values;
    }
}
