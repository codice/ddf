package org.codice.ddf.ui.searchui.standard.endpoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class EndpointUtil {

    public static Function<String, Metacard> getMetacardFunction(
            final CatalogFramework catalogFramework, final FilterBuilder filterBuilder) {
        return (String id) -> {
            try {
                return getMetacard(catalogFramework, filterBuilder, id);
            } catch (UnsupportedQueryException | SourceUnavailableException | StandardSearchException | FederationException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static Metacard getMetacard(CatalogFramework catalogFramework,
            FilterBuilder filterBuilder, String id)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            StandardSearchException {
        Filter idFilter = filterBuilder.attribute(Metacard.ID)
                .is()
                .equalTo()
                .text(id);
        Filter tagsFilter = filterBuilder.attribute(Metacard.TAGS)
                .is()
                .like()
                .text("*");
        Filter filter = filterBuilder.allOf(idFilter, tagsFilter);

        QueryResponse queryResponse = catalogFramework.query(new QueryRequestImpl(new QueryImpl(
                filter), true));

        if (queryResponse.getHits() == 0) {
            throw new StandardSearchException("Could not find metacard for that metacard id");
        }

        Result result = queryResponse.getResults().get(0);
        return result.getMetacard();
    }

    public static List<String> getStringList(List<Serializable> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

}
