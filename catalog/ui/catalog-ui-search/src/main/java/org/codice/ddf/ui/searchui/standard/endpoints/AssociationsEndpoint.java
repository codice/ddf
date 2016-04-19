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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.boon.json.JsonFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
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
import ddf.catalog.operation.UpdateResponse;
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

    private final FilterBuilder filterBuilder;

    private final EndpointUtil endpointUtil;

    public AssociationsEndpoint(CatalogFramework catalogFramework, FilterBuilder filterBuilder,
            EndpointUtil endpointUtil) {
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
        this.endpointUtil = endpointUtil;
    }

    @GET
    @Path("/{id}")
    public Response getAssociations(@PathParam("id") String id) throws Exception {
        Associated associated = getAssociatedMetacardsIds(id);

        return getAssociationsResponse(associated);
    }

    private Response getAssociationsResponse(Associated associated)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        List<String> ids = new ArrayList<>();
        ids.addAll(associated.derived);
        ids.addAll(associated.related);

        Map<String, Result> results = getAssociatedMetacards(ids);

        AndrewAssociation aaRelated = new AndrewAssociation();
        aaRelated.type = "related";
        for (String relatedId : associated.related) {
            aaRelated.metacards.add(new AssociationItem(relatedId,
                    results.get(relatedId)
                            .getMetacard()
                            .getTitle()));
        }

        AndrewAssociation aaDerived = new AndrewAssociation();
        aaDerived.type = "derived";
        for (String derivedId : associated.derived) {
            aaDerived.metacards.add(new AssociationItem(derivedId,
                    results.get(derivedId)
                            .getMetacard()
                            .getTitle()));
        }

        List<AndrewAssociation> associations = new ArrayList<>();
        associations.add(aaDerived);
        associations.add(aaRelated);
        return Response.ok(endpointUtil.getJson(associations), MediaType.APPLICATION_JSON)
                .build();
    }

    private static final String ASSOCIATION_PREFIX = "metacard.associations.";

    @PUT
    @Path("/{id}")
    public Response putAssociations(@PathParam("id") String id, String body) throws Exception {
        List<AndrewAssociation> associations = JsonFactory.create()
                .parser()
                .parseList(AndrewAssociation.class, body);
        Metacard metacard = endpointUtil.getMetacard(id);
        List<Attribute> updatedAttributes = new ArrayList<>();
        for (AndrewAssociation association : associations) {
            ArrayList<String> newIds = new ArrayList<>();
            for (AssociationItem ai : association.metacards) {
                newIds.add(ai.id);
            }
            updatedAttributes.add(new AttributeImpl(ASSOCIATION_PREFIX + association.type, newIds));
        }
        updatedAttributes.forEach(metacard::setAttribute);
        UpdateResponse update = catalogFramework.update(new UpdateRequestImpl(id, metacard));

        return getAssociationsResponse(getAssociatedMetacardIdsFromMetacard(update.getUpdatedMetacards().get(0).getNewMetacard()));
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
        return getAssociatedMetacardIdsFromMetacard(endpointUtil.getMetacard(id));
    }

    private Associated getAssociatedMetacardIdsFromMetacard(Metacard result) {
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

    private Map<String, Result> getAssociatedMetacards(List<String> ids)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        if (ids.isEmpty()) {
            return new HashMap<>();
        }

        List<Filter> filters = new ArrayList<>(ids.size());
        for (String id : ids) {
            Filter historyFilter = filterBuilder.attribute(Metacard.ID)
                    .is()
                    .equalTo()
                    .text(id);
            Filter idFilter = filterBuilder.attribute(Metacard.TAGS)
                    .is()
                    .like()
                    .text("*");
            Filter filter = filterBuilder.allOf(historyFilter, idFilter);
            filters.add(filter);
        }

        Filter queryFilter = filterBuilder.anyOf(filters);
        QueryResponse response = catalogFramework.query(new QueryRequestImpl(new QueryImpl(
                queryFilter,
                1,
                -1,
                SortBy.NATURAL_ORDER,
                false,
                TimeUnit.SECONDS.toMillis(10)), false));
        Map<String, Result> results = new HashMap<>();
        for (Result result : response.getResults()) {
            results.put(result.getMetacard()
                    .getId(), result);
        }
        return results;
    }

    private class Associated {
        List<String> related;

        List<String> derived;
    }

    private class AssociationItem {
        String id;

        String title;

        private AssociationItem(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    private class AndrewAssociation {
        String type;

        List<AssociationItem> metacards = new ArrayList<>();
    }
}


























