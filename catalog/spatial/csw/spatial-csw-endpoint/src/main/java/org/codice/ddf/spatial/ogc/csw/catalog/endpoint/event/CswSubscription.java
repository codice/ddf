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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event;

import java.util.Set;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.opengis.filter.Filter;

import ddf.catalog.event.impl.SubscriptionImpl;
import ddf.catalog.operation.QueryRequest;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;

public class CswSubscription extends SubscriptionImpl {

    private GetRecordsType originalRequest;

    private QueryRequest queryRequest;

    private CswSubscription(GetRecordsType originalRequest, QueryRequest queryRequest,
            Filter filter, SendEvent sendEvent, Set<String> sourceIds, boolean enterprise) {
        super(filter, sendEvent, sourceIds, enterprise);
        this.originalRequest = originalRequest;
        this.queryRequest = queryRequest;
    }

    public CswSubscription(GetRecordsType originalRequest, QueryRequest queryRequest,
            TransformerManager mimeTypeTransformerManager) throws CswException {
        this(originalRequest,
                queryRequest,
                queryRequest.getQuery(),
                new SendEvent(mimeTypeTransformerManager, originalRequest, queryRequest),
                queryRequest.getSourceIds(),
                queryRequest.isEnterprise());
    }

    public static CswSubscription getFilterlessSubscription(GetRecordsType originalRequest,
            QueryRequest queryRequest, TransformerManager mimeTypeTransformerManager) throws CswException {
        return new CswSubscription(originalRequest,
                queryRequest,
                Filter.INCLUDE,
                new SendEvent(mimeTypeTransformerManager, originalRequest, queryRequest),
                null,
                false);
    }

    public GetRecordsType getOriginalRequest() {
        return originalRequest;
    }

    public QueryRequest getQueryRequest() {
        return queryRequest;
    }
}
