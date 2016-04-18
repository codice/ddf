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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.impl.UpdateRequestImpl;

@Path("/edit")
public class MetacardEditEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardEditEndpoint.class);

    private final CatalogFramework catalogFramework;

    private final AttributeRegistry attributeRegistry;

    private final EndpointUtil endpointUtil;

    public MetacardEditEndpoint(CatalogFramework catalogFramework,
            AttributeRegistry attributeRegistry, EndpointUtil endpointUtil) {
        this.catalogFramework = catalogFramework;
        this.attributeRegistry = attributeRegistry;
        this.endpointUtil = endpointUtil;
    }

    @GET
    @Path("/{id}/{attribute}")
    public Response getAttribute(@Context HttpServletResponse response, @PathParam("id") String id,
            @PathParam("attribute") String attribute) throws Exception {
        Metacard metacard = endpointUtil.getMetacard(id);
        Attribute metacardAttribute = metacard.getAttribute(attribute);
        if (metacardAttribute == null) {
            return Response.status(404).build();
        }
        Optional<AttributeDescriptor> attributeDescriptor =
                attributeRegistry.getAttributeDescriptor(attribute);
        if (!attributeDescriptor.isPresent()) {
            /* Could not find attribute descriptor for requested attribute */
            return Response.status(404).build();
        }

        AttributeDescriptor descriptor = attributeDescriptor.get();
        /* Yes i'm using a raw map. get off my back yo */
        Map<String, Object> result = getResponseMap(attribute, metacardAttribute, descriptor);
        return Response.ok(JsonFactory.create()
                .toJson(result), MediaType.APPLICATION_JSON)
                .build();
    }

    @PUT
    @Path("/{id}/{attribute}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response setAttribute(@Context HttpServletResponse response, @PathParam("id") String id,
            @PathParam("attribute") String attribute, String value) throws Exception {
        Metacard metacard = endpointUtil.getMetacard(id);
        if (metacard == null) {
            return Response.status(404).build();
        }

        Attribute metacardAttribute = metacard.getAttribute(attribute);
        Optional<AttributeDescriptor> attributeDescriptor =
                attributeRegistry.getAttributeDescriptor(attribute);
        if (!attributeDescriptor.isPresent()) {
            /* Could not find attribute descriptor for requested attribute */
            return Response.status(404)
                    .build();
        }

        AttributeDescriptor descriptor = attributeDescriptor.get();
        if (descriptor.isMultiValued()) {
            if (metacardAttribute == null || metacardAttribute.getValues() == null) {
                metacard.setAttribute(new AttributeImpl(attribute,
                        Collections.singletonList(value)));
            } else {
                List<Serializable> values = new ArrayList<>(metacardAttribute.getValues());
                if (!values.contains(value)) {
                    values.add(value);
                }
                metacard.setAttribute(new AttributeImpl(attribute, values));
            }
        } else { // not multivalued
            metacard.setAttribute(new AttributeImpl(attribute, value));
        }

        catalogFramework.update(new UpdateRequestImpl(id, metacard));
        Map<String, Object> responseMap = getResponseMap(attribute,
                metacard.getAttribute(attribute),
                descriptor);
        return Response.ok(JsonFactory.create()
                .toJson(responseMap), MediaType.APPLICATION_JSON)
                .build();
    }

    @PUT
    @Path("/{id}/{attribute}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response setBinaryAttribute(@Context HttpServletResponse response,
            @PathParam("id") String id, @PathParam("attribute") String attribute, byte[] value)
            throws Exception {
        Metacard metacard = endpointUtil.getMetacard(id);
        if (metacard == null) {
            return Response.status(404).build();
        }

        Attribute metacardAttribute = metacard.getAttribute(attribute);
        Optional<AttributeDescriptor> attributeDescriptor =
                attributeRegistry.getAttributeDescriptor(attribute);
        if (!attributeDescriptor.isPresent()) {
            /* Could not find attribute descriptor for requested attribute */
            response.setStatus(404);
            return Response.status(404)
                    .build();
        }
        AttributeDescriptor descriptor = attributeDescriptor.get();
        if (!descriptor.getType()
                .getAttributeFormat()
                .equals(AttributeType.AttributeFormat.BINARY)) {
            return Response.status(400)
                    .build();
        }

        if (descriptor.isMultiValued()) {
            List<Serializable> values;
            if (metacardAttribute == null) {
                values = new ArrayList<>();
            } else {
                values = metacardAttribute.getValues();
            }
            if (!values.contains(value)) {
                values.add(value);
            }
            metacard.setAttribute(new AttributeImpl(attribute, values));
        } else {
            metacard.setAttribute(new AttributeImpl(attribute, value));
        }

        catalogFramework.update(new UpdateRequestImpl(id, metacard));
        Map<String, Object> responseMap = getResponseMap(attribute,
                metacard.getAttribute(attribute),
                descriptor);
        return Response.ok(JsonFactory.create()
                .toJson(response))
                .build();

    }

    @DELETE
    @Path("/{id}/{attribute}")
    public Response deleteAttribute(@Context HttpServletResponse response,
            @PathParam("id") String id, @PathParam("attribute") String attribute, String value)
            throws Exception {
        Metacard metacard = endpointUtil.getMetacard(id);
        Attribute metacardAttribute = metacard.getAttribute(attribute);

        if (metacardAttribute == null) {
            return Response.ok()
                    .build(); // TODO (RCZ) - if it wasn't there is that an okay or bad?
        }

        metacard.setAttribute(new AttributeImpl(attribute, (Serializable) null));
        catalogFramework.update(new UpdateRequestImpl(id, metacard));
        return Response.ok()
                .build();
    }

    private Map<String, Object> getResponseMap(String attribute, Attribute metacardAttribute,
            AttributeDescriptor descriptor) {
        Map<String, Object> result = new HashMap<>();
        result.put("multivalued", descriptor.isMultiValued());
        result.put("type",
                descriptor.getType()
                        .getAttributeFormat()
                        .name());
        if (descriptor.isMultiValued()) {
            result.put(attribute, metacardAttribute.getValues());

        } else {
            result.put(attribute, metacardAttribute.getValue());
        }
        return result;
    }
}


