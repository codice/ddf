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

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.core.versioning.HistoryMetacardImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Path("/history")
public class HistoryEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryEndpoint.class);

    private final CatalogFramework catalogFramework;

    private final FilterBuilder filterBuilder;

    private final EndpointUtil endpointUtil;

    public HistoryEndpoint(CatalogFramework catalogFramework, FilterBuilder filterBuilder,
            EndpointUtil endpointUtil) {
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
        this.endpointUtil = endpointUtil;
    }

    @GET
    @Path("/{id}")
    public Response getHistory(@PathParam("id") String id) throws Exception {
        List<Result> queryResponse = getMetacardHistory(id);
        if (queryResponse == null || queryResponse.isEmpty()) {
            return Response.status(404)
                    .build();
        }
        long start = System.nanoTime();
        List<HistoryResponse> response = queryResponse.stream()
                .map(Result::getMetacard)
                .map(mc -> new HistoryResponse(mc.getId(),
                        (String) mc.getAttribute(HistoryMetacardImpl.EDITED_BY)
                                .getValue(),
                        (Date) mc.getAttribute(HistoryMetacardImpl.VERSIONED)
                                .getValue()))
                //                .sorted(compareBy(mc -> mc.versioned))
                .collect(Collectors.toList());
        LOGGER.error("Time taken to do the history stuff: {} ns", System.nanoTime() - start);
        return Response.ok(endpointUtil.getJson(response), MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path("/{id}/revert/{revertId}")
    public Response revertHistory(@PathParam("id") String id,
            @PathParam("revertId") String revertId) throws Exception {
        Metacard versionMetacard = endpointUtil.getMetacard(revertId);
        if (versionMetacard == null) {
            return Response.status(404)
                    .build();
        }

        if (((String) versionMetacard.getAttribute(HistoryMetacardImpl.ACTION)
                .getValue()).equals(HistoryMetacardImpl.Action.DELETED.getKey())) {
            /* can't revert to a deleted.. right now */
            return Response.status(400)
                    .build();
        }

        Metacard revertMetacard = HistoryMetacardImpl.toBasicMetacard(versionMetacard);
        catalogFramework.update(new UpdateRequestImpl(id, revertMetacard));

        Map<String, Object> response = endpointUtil.transformToJson(revertMetacard);

        return Response.ok(JsonFactory.create(new JsonParserFactory(),
                new JsonSerializerFactory().includeNulls()
                        .includeEmpty())
                .toJson(response), MediaType.APPLICATION_JSON)
                .build();
    }

    // TODO (RCZ) - move this into a util class
    @SuppressWarnings("unchecked")
    private <T, R extends Comparable> Comparator<T> compareBy(Function<T, R> getField) {
        return (T o1, T o2) -> getField.apply(o1)
                .compareTo(getField.apply(o2));
    }

    private List<Result> getMetacardHistory(String id) throws Exception {
        Filter historyFilter = filterBuilder.attribute(Metacard.TAGS)
                .is()
                .equalTo()
                .text(HistoryMetacardImpl.HISTORY_TAG);
        Filter idFilter = filterBuilder.attribute(HistoryMetacardImpl.ID_HISTORY)
                .is()
                .equalTo()
                .text(id);

        Filter filter = filterBuilder.allOf(historyFilter, idFilter);
        QueryResponse response = catalogFramework.query(new QueryRequestImpl(new QueryImpl(filter,
                1,
                -1,
                SortBy.NATURAL_ORDER,
                false,
                TimeUnit.SECONDS.toMillis(10)), false));
        return response.getResults();
    }

    private static class HistoryResponse {
        Instant versioned;

        @SuppressFBWarnings
        String id;

        @SuppressFBWarnings
        String editedBy;

        private HistoryResponse(String historyId, String editedBy, Instant versioned) {
            this.id = historyId;
            this.editedBy = editedBy;
            this.versioned = versioned;
        }

        private HistoryResponse(String historyId, String editedBy, Date versioned) {
            this.id = historyId;
            this.editedBy = editedBy;
            this.versioned = versioned.toInstant();
        }

        Instant getVersioned() {
            return this.versioned;
        }
    }
}
