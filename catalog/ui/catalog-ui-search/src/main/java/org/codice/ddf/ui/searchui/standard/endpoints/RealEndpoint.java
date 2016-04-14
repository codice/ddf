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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.report.MetacardValidationReport;

public class RealEndpoint {
    private final CatalogFramework catalogFramework;

    private final FilterBuilder filterBuilder;

    private final AttributeValidatorRegistry attributeValidatorRegistry;

    private final Function<String, Metacard> getMetacard;

    private final List<MetacardType> metacardTypes;

    private final ReportingMetacardValidator reportingMetacardValidator;

    public RealEndpoint(CatalogFramework catalogFramework, FilterBuilder filterBuilder,
            AttributeValidatorRegistry attributeValidatorRegistry, List<MetacardType> metacardTypes,
            ReportingMetacardValidator reportingMetacardValidator) {
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
        this.attributeValidatorRegistry = attributeValidatorRegistry;
        this.reportingMetacardValidator = reportingMetacardValidator;
        this.getMetacard = EndpointUtil.getMetacardFunction(catalogFramework, filterBuilder);
        this.metacardTypes = metacardTypes;
    }

    @GET
    @Path("/metacardtype")
    public Response getMetacardType() throws Exception {
        Map<String, Object> resultTypes = getMetacardTypeMap();
        return Response.ok(JsonFactory.create()
                .toJson(resultTypes), MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path("/metacard/{id}")
    public Response getMetacard(@PathParam("id") String id) throws Exception {
        Metacard metacard = getMetacard.apply(id);
        Map<String, Object> response = transformToJson(metacard);

        return Response.ok(JsonFactory.create(new JsonParserFactory(),
                new JsonSerializerFactory().includeNulls()
                        .includeEmpty())
                .toJson(response), MediaType.APPLICATION_JSON)
                .build();
    }

    @POST
    @Path("/metacard")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createMetacard(String metacard) throws Exception {
        throw new GoldPlatingException();
    }

    public class GoldPlatingException extends Exception {
    }

    @PUT
    @Path("/metacard")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMetacard(String metacard) throws Exception {
        Map<String, Object> metacardMap = JsonFactory.create()
                .parser()
                .parseMap(metacard);
        Metacard newMetacard = getMetacard.apply((String) metacardMap.get(Metacard.ID));
        MetacardType metacardType = newMetacard.getMetacardType();

        for (Map.Entry<String, Object> entry : metacardMap.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            if (Metacard.THUMBNAIL.equals(entry.getKey())) {
                newMetacard.setAttribute(new AttributeImpl(Metacard.THUMBNAIL,
                        (String) entry.getValue()));
            }

            if (metacardType.getAttributeDescriptor(entry.getKey())
                    .isMultiValued()) {
                // TODO (RCZ) - this is bad and i should feel bad. Don't cast , do something better
                newMetacard.setAttribute(new AttributeImpl(entry.getKey(),
                        getSerializableList((List) entry.getValue())));
            } else {
                Serializable data = null;
                if (entry.getValue() instanceof List) {
                    data = getSerializableList((List) entry.getValue()).get(0);
                } else if (entry.getValue() instanceof Long) {
                    data = (Long) entry.getValue();
                } else if (entry.getValue() instanceof String) {
                    data = (String) entry.getValue();
                } else {
                    throw new GoldPlatingException();
                }
                newMetacard.setAttribute(new AttributeImpl(entry.getKey(), data));
            }
        }

        // TODO (RCZ) - don't assume this only service, do for real
        Optional<MetacardValidationReport> metacardValidationReport =
                reportingMetacardValidator.validateMetacard(newMetacard);

        if (metacardValidationReport.isPresent()) {
            return Response.status(400)
                    .entity(metacardValidationReport.get()
                            .getAttributeValidationViolations())
                    .build();
        }

        UpdateResponse updateResponse =
                catalogFramework.update(new UpdateRequestImpl(newMetacard.getId(), newMetacard));
        if (updateResponse.getProcessingErrors() != null && !updateResponse.getProcessingErrors()
                .isEmpty()) {
            // TODO (RCZ) - should probably pull actual processing errors?
            return Response.status(400)
                    .build();
        }

        return Response.ok(JsonFactory.create(new JsonParserFactory(),
                new JsonSerializerFactory().includeNulls()
                        .includeEmpty())
                .toJson(transformToJson(newMetacard)), MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path("/metacard/{id}/validation")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validateMetacard(@PathParam("id") String id) throws Exception {
        Metacard newMetacard = getMetacard.apply(id);
        MetacardType metacardType = newMetacard.getMetacardType();

        // TODO (RCZ) - don't assume this only service, do for real
        Optional<MetacardValidationReport> metacardValidationReport =
                reportingMetacardValidator.validateMetacard(newMetacard);

        if (metacardValidationReport.isPresent()) {
            return Response.status(400)
                    .entity(metacardValidationReport.get()
                            .getAttributeValidationViolations())
                    .build();
        }
        return Response.ok("[]",MediaType.APPLICATION_JSON).build();
    }

    private List<Serializable> getSerializableList(List list) {
        return new ArrayList<>(list);
    }

    private Optional<MetacardType> getMetacardType(String name) {
        return metacardTypes.stream()
                .filter(mt -> mt.getName()
                        .equals(name))
                .findFirst();
    }

    private Map<String, Object> transformToJson(Metacard metacard) {
        Set<AttributeDescriptor> attributeDescriptors = metacard.getMetacardType()
                .getAttributeDescriptors();
        Map<String, Object> result = new HashMap<>();
        for (AttributeDescriptor descriptor : attributeDescriptors) {
            if (metacard.getAttribute(descriptor.getName()) == null) {
                if (descriptor.isMultiValued()) {
                    result.put(descriptor.getName(), Collections.emptyList());
                } else {
                    result.put(descriptor.getName(), null);
                }
                continue;
            }
            if (Metacard.THUMBNAIL.equals(descriptor.getName())) {
                if (metacard.getThumbnail() != null) {
                    result.put(descriptor.getName(),
                            Base64.getEncoder()
                                    .encodeToString(metacard.getThumbnail()));
                } else {
                    result.put(descriptor.getName(), null);
                }
                continue;

            }
            if (descriptor.isMultiValued()) {
                result.put(descriptor.getName(),
                        metacard.getAttribute(descriptor.getName())
                                .getValues());
            } else {
                result.put(descriptor.getName(),
                        metacard.getAttribute(descriptor.getName())
                                .getValue());
            }
        }

        Map<String, Object> typeMap = new HashMap<>();
        typeMap.put("type",
                getMetacardTypeMap().get(metacard.getMetacardType()
                        .getName()));
        typeMap.put("type-name",
                metacard.getMetacardType()
                        .getName());
        typeMap.put("ids", Collections.singletonList(metacard.getId()));

        List<Object> typeList = new ArrayList<>();
        typeList.add(typeMap);

        Map<String, Object> outerMap = new HashMap<>();
        outerMap.put("metacards", Collections.singletonList(result));
        outerMap.put("metacard-types", typeList);

        return outerMap;
    }

    private Map<String, Object> getMetacardTypeMap() {
        Map<String, Object> resultTypes = new HashMap<>();
        for (MetacardType metacardType : metacardTypes) {
            List<Object> attributes = new ArrayList<>();
            for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
                Map<String, Object> attributeProperties = new HashMap<>();
                attributeProperties.put("type",
                        descriptor.getType()
                                .getAttributeFormat()
                                .name());
                attributeProperties.put("multivalued", descriptor.isMultiValued());
                attributeProperties.put("id", descriptor.getName());
                attributes.add(attributeProperties);
            }
            resultTypes.put(metacardType.getName(), attributes);
        }
        return resultTypes;
    }

}
