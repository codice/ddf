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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.boon.json.JsonFactory;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.InputTransformer;

@Path("/")
public class WorkspacesEndpoint {

    private final CatalogFramework cf;

    private final FilterBuilder fb;

    private final InputTransformer it;

    public WorkspacesEndpoint(CatalogFramework cf, FilterBuilder fb, InputTransformer it) {
        this.cf = cf;
        this.fb = fb;
        this.it = it;
    }

    private static List<String> getValues(Metacard m, String attribute) {
        return m.getAttribute(attribute)
                .getValues()
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    private static Workspace toWorkspace(Metacard m) {
        return new Workspace(m.getId(), m.getTitle(), getValues(m,
                WorkspaceMetacardTypeImpl.WORKSPACE_METACARDS), getValues(m,
                WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES));
    }

    private static Response ok() {
        return Response.ok()
                .build();
    }

    private static Response ok(Object o) {
        return Response.ok(JsonFactory.create()
                .toJson(o))
                .build();
    }

    private static Response notFound() {
        return Response.status(404)
                .build();
    }

    private static Workspace getWorkspace(HttpServletRequest req) throws IOException {
        return JsonFactory.create()
                .readValue(req.getInputStream(), Workspace.class);
    }

    private static Metacard getMetacard(HttpServletRequest req) throws IOException {
        WorkspaceMetacardImpl m = new WorkspaceMetacardImpl();
        Workspace w = getWorkspace(req);

        m.setTitle(w.title);
        m.setQueries(w.queries);
        m.setMetacards(w.metacards);

        return m;
    }

    @GET
    @Path("/workspaces/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Workspace getDocument(@PathParam("id") String id) throws Exception {
        List<Workspace> workspaces = query(builder().allOf(byId(id), byWorkspaceTag()));

        if (workspaces.size() > 0) {
            return workspaces.get(0);
        }

        return null;
    }

    @GET
    @Path("/workspaces")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocuments(@Context HttpServletRequest req) throws Exception {
        return ok(query(byWorkspaceTag()));
    }

    @POST
    @Path("/workspaces")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postDocument(@Context HttpServletRequest req) throws Exception {
        Metacard m = getMetacard(req);
        Workspace w = saveMetacard(m);
        return ok(w);
    }

    @DELETE
    @Path("/workspaces/{id}")
    public Response deleteDocument(@PathParam("id") String id) throws Exception {
        cf.delete(new DeleteRequestImpl(id));
        return ok();
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

    private List<Workspace> query(Filter f)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        return cf.query(new QueryRequestImpl(new QueryImpl(f)))
                .getResults()
                .stream()
                .map(result -> result.getMetacard())
                .map(WorkspacesEndpoint::toWorkspace)
                .collect(Collectors.toList());
    }

    private Workspace saveMetacard(Metacard metacard)
            throws IngestException, SourceUnavailableException {
        String id = cf.create(new CreateRequestImpl(metacard))
                .getCreatedMetacards()
                .get(0)
                .getId();
        return new Workspace(id);
    }
}
