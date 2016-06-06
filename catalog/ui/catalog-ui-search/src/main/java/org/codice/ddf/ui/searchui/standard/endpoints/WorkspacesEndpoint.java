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

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardTypeImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceTransformer;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspacesEndpoint.class);

    private static final String UPDATE_ERROR_MESSAGE =
            "Workspace is either restricted or not found.";

    private static final String NOT_PERMITTED_ERROR_MESSAGE =
            "User not permitted to create workpsace.";

    private final CatalogFramework catalogFramework;

    private final FilterBuilder filterBuilder;

    private final WorkspaceTransformer transformer;

    public WorkspacesEndpoint(CatalogFramework catalogFramework, FilterBuilder filterBuilder,
            WorkspaceTransformer transformer) {
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
        this.transformer = transformer;
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
        LOGGER.warn("Could not find workspace {}.", id);
        return Collections.singletonMap("message", UPDATE_ERROR_MESSAGE);
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getDocuments(@Context HttpServletRequest req) throws Exception {
        return transformer.transform(queryWorkspaces());
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map postDocument(Map<String, Object> workspace, @Context HttpServletResponse res)
            throws Exception {
        try {
            Metacard saved = saveMetacard(transformer.transform(workspace));
            res.setStatus(201);
            return transformer.transform(saved);
        } catch (IngestException ex) {
            LOGGER.warn("Could not create workspace {}.", workspace, ex);
            res.setStatus(401);
            return Collections.singletonMap("message", NOT_PERMITTED_ERROR_MESSAGE);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map putDocument(Map<String, Object> workspace, @PathParam("id") String id,
            @Context HttpServletResponse res) throws Exception {
        WorkspaceMetacardImpl update = transformer.transform(workspace);
        update.setId(id);

        try {
            return transformer.transform(updateMetacard(id, update));
        } catch (IngestException ex) {
            res.setStatus(404);
            LOGGER.warn("Could not update workspace {} {}.", id, workspace, ex);
            return Collections.singletonMap("message", UPDATE_ERROR_MESSAGE);
        }
    }

    @DELETE
    @Path("/{id}")
    public Map deleteDocument(@PathParam("id") String id, @Context HttpServletResponse res)
            throws Exception {
        try {
            catalogFramework.delete(new DeleteRequestImpl(id));
            return Collections.singletonMap("message", "success");
        } catch (IngestException ex) {
            res.setStatus(404);
            LOGGER.warn("Could not delete workspace {}.", id, ex);
            return Collections.singletonMap("message", UPDATE_ERROR_MESSAGE);
        }
    }

    private Set<String> getRoles(Metacard metacard) {
        Attribute attr = metacard.getAttribute(WorkspaceMetacardTypeImpl.WORKSPACE_ROLES);

        if (attr != null) {
            return new HashSet<>(getStringList(attr.getValues()));
        }

        return new HashSet<>();
    }

    private Set<String> getUpdatedRoles(String id, Metacard newMetacard) throws Exception {
        Set<String> newRoles = getRoles(newMetacard);

        List<Metacard> metacards = queryWorkspaces(byId(id));
        if (!metacards.isEmpty()) {
            Set<String> oldRoles = getRoles(metacards.get(0));
            return Sets.symmetricDifference(oldRoles, newRoles);
        }

        return newRoles;
    }

    private FilterBuilder builder() {
        return filterBuilder;
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
        return catalogFramework.query(new QueryRequestImpl(new QueryImpl(f)))
                .getResults()
                .stream()
                .map(Result::getMetacard)
                .collect(Collectors.toList());
    }

    private Metacard updateMetacard(String id, Metacard metacard)
            throws SourceUnavailableException, IngestException {
        return catalogFramework.update(new UpdateRequestImpl(id, metacard))
                .getUpdatedMetacards()
                .get(0)
                .getNewMetacard();
    }

    private Metacard saveMetacard(Metacard metacard)
            throws IngestException, SourceUnavailableException {
        return catalogFramework.create(new CreateRequestImpl(metacard))
                .getCreatedMetacards()
                .get(0);

    }

    private List<String> getStringList(List<Serializable> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }
}
