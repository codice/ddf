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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

@Path("/associations")
public class AssociationsEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssociationsEndpoint.class);

    private final CatalogFramework catalogFramework;

    private final EndpointUtil endpointUtil;

    public AssociationsEndpoint(CatalogFramework catalogFramework, EndpointUtil endpointUtil) {
        this.catalogFramework = catalogFramework;
        this.endpointUtil = endpointUtil;
    }

    @GET
    @Path("/{id}")
    public Response getAssociations(@PathParam("id") String id) throws Exception {
        Associated associated = getAssociatedMetacardsIds(id);

        AndrewAssociations aaRelated = new AndrewAssociations();
        aaRelated.type = "related";
        aaRelated.ids = associated.related;

        AndrewAssociations aaDerived = new AndrewAssociations();
        aaDerived.type = "derived";
        aaDerived.ids = associated.derived;

        List<AndrewAssociations> associations = new ArrayList<>();
        associations.add(aaDerived);
        associations.add(aaRelated);
        return Response.ok(JsonFactory.create(new JsonParserFactory(),
                new JsonSerializerFactory().includeNulls()
                        .includeEmpty())
                .toJson(associations), MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path("/{id}/related")
    public Response getRelatedAssociations(@PathParam("id") String id) throws Exception {
        List<String> related = getAssociatedMetacardsIds(id).related;
        AndrewAssociations aaRelated = new AndrewAssociations();
        aaRelated.type = "related";
        aaRelated.ids = related;
        return Response.ok(JsonFactory.create(new JsonParserFactory(),
                new JsonSerializerFactory().includeNulls()
                        .includeEmpty())
                .toJson(aaRelated), MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path("/{id}/derived")
    public Response getDerivedAssociations(@PathParam("id") String id) throws Exception {
        List<String> derived = getAssociatedMetacardsIds(id).derived;
        AndrewAssociations aaDerived = new AndrewAssociations();
        aaDerived.type = "derived";
        aaDerived.ids = derived;
        return Response.ok(JsonFactory.create(new JsonParserFactory(),
                new JsonSerializerFactory().includeNulls()
                        .includeEmpty())
                .toJson(aaDerived), MediaType.APPLICATION_JSON)
                .build();
    }

    @PUT
    @Path("/{id}/related")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> putRelatedAssociation(@PathParam("id") String id, String associatedId)
            throws Exception {
        return new ArrayList<>(putAssociation(id, associatedId, Metacard.RELATED));
    }

    @PUT
    @Path("/{id}/derived")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> putDerivedAssociations(@PathParam("id") String id, String associatedId)
            throws Exception {
        return new ArrayList<>(putAssociation(id, associatedId, Metacard.DERIVED));
    }

    @DELETE
    @Path("/{id}/related")
    public List<String> deleteRelatedAssociation(@PathParam("id") String id, String associatedId)
            throws Exception {
        return new ArrayList<>(deleteAssociation(id, associatedId, Metacard.RELATED));
    }

    @DELETE
    @Path("/{id}/derived")
    public List<String> deleteDerivedAssociation(@PathParam("id") String id, String associatedId)
            throws Exception {
        return new ArrayList<>(deleteAssociation(id, associatedId, Metacard.DERIVED));
    }

    private Set<String> deleteAssociation(String id, String associatedId, String attributeId)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            IngestException, StandardSearchException {
        Metacard target = endpointUtil.getMetacard(id);
        Set<String> values = new HashSet<>(endpointUtil.getStringList(target.getAttribute(
                attributeId)
                .getValues()));
        if (!values.contains(associatedId)) {
            LOGGER.info("Metacard [{}] does not contain association [{}]", id, associatedId);
            return values;
        }

        values.remove(associatedId);
        List<Serializable> updated = new ArrayList<>(values);
        target.setAttribute(new AttributeImpl(attributeId, updated));
        catalogFramework.update(new UpdateRequestImpl(id, target));
        return values;
    }

    private Set<String> putAssociation(String id, String associatedId, String attributeId)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            StandardSearchException, IngestException {

        Metacard target = endpointUtil.getMetacard(id);
        Attribute attribute = target.getAttribute(attributeId);
        Set<String> values = new HashSet<>(endpointUtil.getStringList(
                attribute != null ? attribute.getValues() : null));

        if (values.contains(associatedId)) {
            LOGGER.info("Metacard [{}] already contained association [{}]", id, associatedId);
            return values;
        }

        values.add(associatedId);
        List<Serializable> results = new ArrayList<>(values);
        target.setAttribute(new AttributeImpl(attributeId, results));
        catalogFramework.update(new UpdateRequestImpl(id, target));
        return values;
    }

    private Associated getAssociatedMetacardsIds(String id)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            StandardSearchException {
        Metacard result = endpointUtil.getMetacard(id);
        List<Serializable> related = new ArrayList<>();
        List<Serializable> derived = new ArrayList<>();

        Attribute relatedAttribute = result.getAttribute(Metacard.RELATED);
        related = relatedAttribute != null ? relatedAttribute.getValues() : related;

        Attribute derivedAttribute = result.getAttribute(Metacard.DERIVED);
        derived = derivedAttribute != null ? derivedAttribute.getValues() : derived;

        Associated associated = new Associated();
        associated.related = endpointUtil.getStringList(related);
        associated.derived = endpointUtil.getStringList(derived);

        return associated;
    }

    private class Associated {
        List<String> related;

        List<String> derived;
    }

    private class AndrewAssociations {
        String type;

        List<String> ids;
    }
}


























