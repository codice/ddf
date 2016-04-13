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

import static org.codice.ddf.ui.searchui.standard.endpoints.EndpointUtil.getStringList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.xml.ws.Endpoint;

import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

@Path("/associations")
public class AssociationsEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssociationsEndpoint.class);

    private final CatalogFramework catalogFramework;

    private Function<String, Metacard> getMetacard;

    public AssociationsEndpoint(CatalogFramework catalogFramework, FilterBuilder filterBuilder) {
        this.catalogFramework = catalogFramework;
        getMetacard = EndpointUtil.getMetacardFunction(catalogFramework, filterBuilder);
    }

    @GET
    @Path("/{id}")
    public Associated getAssociations(@PathParam("id") String id) throws Exception {
        return getAssociatedMetacardsIds(id);
    }

    @GET
    @Path("/{id}/related")
    public List<String> getRelatedAssociations(@PathParam("id") String id) throws Exception {
        return getAssociatedMetacardsIds(id).related;
    }

    @GET
    @Path("/{id}/derived")
    public List<String> getDerivedAssociations(@PathParam("id") String id) throws Exception {
        return getAssociatedMetacardsIds(id).derived;
    }

    @PUT
    @Path("/{id}/related")
    @Consumes(MediaType.TEXT_PLAIN)
    public List<String> putRelatedAssociation(@PathParam("id") String id, String associatedId)
            throws Exception {
        return new ArrayList<>(putAssociation(id, associatedId, Metacard.RELATED));
    }

    @PUT
    @Path("/{id}/derived")
    @Consumes(MediaType.TEXT_PLAIN)
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
        Metacard target = getMetacard.apply(id);
        Set<String> values = new HashSet<>(getStringList(target.getAttribute(attributeId)
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

        Metacard target = getMetacard.apply(id);
        Attribute attribute = target.getAttribute(attributeId);
        Set<String> values = new HashSet<>(getStringList(
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
        Metacard result = getMetacard.apply(id);
        List<Serializable> related = new ArrayList<>();
        List<Serializable> derived = new ArrayList<>();

        Attribute relatedAttribute = result.getAttribute(Metacard.RELATED);
        related = relatedAttribute != null ? relatedAttribute.getValues() : related;

        Attribute derivedAttribute = result.getAttribute(Metacard.DERIVED);
        derived = derivedAttribute != null ? derivedAttribute.getValues() : derived;

        Associated associated = new Associated();
        associated.related = getStringList(related);
        associated.derived = getStringList(derived);
        return associated;
    }

    private class Associated {
        List<String> related;

        List<String> derived;
    }
}


























