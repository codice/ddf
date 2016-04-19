package org.codice.ddf.ui.searchui.standard.endpoints;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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

import ddf.catalog.CatalogFramework;
import ddf.catalog.core.versioning.HistoryMetacardImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;

@Path("/history")
public class HistoryEndpoint {
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

        List<HistoryResponse> response = queryResponse.stream()
                .map(Result::getMetacard)
                .map(mc -> new HistoryResponse(mc.getId(),
                        (String) mc.getAttribute(HistoryMetacardImpl.EDITED_BY)
                                .getValue(),
                        (Date) mc.getAttribute(HistoryMetacardImpl.VERSIONED)
                                .getValue()))
                .sorted(compareBy(mc -> mc.versioned))
                .collect(Collectors.toList());

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

        return Response.ok()
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
                0,
                -1,
                SortBy.NATURAL_ORDER,
                false,
                TimeUnit.SECONDS.toMillis(10)), false));
        return response.getResults();
    }

    private class HistoryResponse {
        Instant versioned;

        String id;

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
