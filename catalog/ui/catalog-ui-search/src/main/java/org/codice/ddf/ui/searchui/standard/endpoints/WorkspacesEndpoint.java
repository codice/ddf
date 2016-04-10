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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.opengis.filter.Filter;

import com.google.common.collect.Sets;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

@Path("/workspaces")
public class WorkspacesEndpoint {

    private final CatalogFramework cf;

    private final FilterBuilder fb;

    private final WorkspaceTransformer transformer;

    public WorkspacesEndpoint(CatalogFramework cf, FilterBuilder fb,
            WorkspaceTransformer transformer) {
        this.cf = cf;
        this.fb = fb;
        this.transformer = transformer;
    }

    private Set getRoles(Metacard metacard) {
        Attribute attr = metacard.getAttribute(WorkspaceMetacardTypeImpl.WORKSPACE_ROLES);

        if (attr != null) {
            return new HashSet<>(attr.getValues());
        }

        return new HashSet<>();
    }

    private Set getUpdatedRoles(String id, Metacard newMetacard) throws Exception {
        Set newRoles = getRoles(newMetacard);

        List<Metacard> metacards = queryWorkspaces(byId(id));
        if (!metacards.isEmpty()) {
            Set oldRoles = getRoles(metacards.get(0));
            return Sets.symmetricDifference(oldRoles, newRoles);
        }

        return newRoles;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map getDocument(@PathParam("id") String id, @Context HttpServletResponse res)
            throws Exception {
        List<Metacard> workspaces = queryWorkspaces(byId(id));

        if (workspaces.size() > 0) {
            return transformer.transform(workspaces.get(0));
        }

        res.setStatus(404);
        return null;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map> getDocuments(@Context HttpServletRequest req) throws Exception {
        return transformer.transform(queryWorkspaces());
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map postDocument(Map<String, Object> workspace, @Context HttpServletResponse res)
            throws Exception {
        Map<String, Object> response;

        try {
            Metacard saved = saveMetacard(transformer.transform(workspace));
            response = transformer.transform(saved);
            res.setStatus(201);
        } catch (Exception ex) {
            res.setStatus(400);
            response = new HashMap<>();
            response.put("message", "bad request");
        }

        return response;
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map putDocument(Map<String, Object> workspace, @PathParam("id") String id)
            throws Exception {
        Set updatedRoles = getUpdatedRoles(id, transformer.transform(workspace));
        Metacard updated = updateMetacard(id, transformer.transform(workspace));
        return transformer.transform(updated);
    }

    @DELETE
    @Path("/{id}")
    public void deleteDocument(@PathParam("id") String id, @Context HttpServletResponse res)
            throws Exception {
        cf.delete(new DeleteRequestImpl(id));
        res.setStatus(200);
    }

    private FilterBuilder builder() {
        return fb;
    }

    private Filter byId(String id) {
        return builder().attribute(Metacard.ID)
                .is()
                .equalTo()
                .text(id);
    }

    private Filter byWorkspaceTag() {
        return builder().attribute(Metacard.TAGS)
                .is()
                .like()
                .text(WorkspaceMetacardTypeImpl.WORKSPACE_TAG);
    }

    private List<Metacard> queryWorkspaces()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        return query(byWorkspaceTag());
    }

    private List<Metacard> queryWorkspaces(Filter f)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        return query(builder().allOf(f, byWorkspaceTag()));
    }

    private List<Metacard> query(Filter f)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        return cf.query(new QueryRequestImpl(new QueryImpl(f)))
                .getResults()
                .stream()
                .map(Result::getMetacard)
                .collect(Collectors.toList());
    }

    private Metacard updateMetacard(String id, Metacard metacard)
            throws SourceUnavailableException, IngestException {
        return cf.update(new UpdateRequestImpl(id, metacard))
                .getUpdatedMetacards()
                .get(0)
                .getNewMetacard();
    }

    private Metacard saveMetacard(Metacard metacard)
            throws IngestException, SourceUnavailableException {
        return cf.create(new CreateRequestImpl(metacard))
                .getCreatedMetacards()
                .get(0);

    }
}
