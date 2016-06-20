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

    private CswSubscription(GetRecordsType request, Filter filter, SendEvent sendEvent,
            Set<String> sourceIds, boolean enterprise) {
        super(filter, sendEvent, sourceIds, enterprise);
        this.originalRequest = request;
    }

    public CswSubscription(TransformerManager mimeTypeTransformerManager, GetRecordsType request,
            QueryRequest query) throws CswException {
        this(request,
                query.getQuery(),
                new SendEvent(mimeTypeTransformerManager, request, query),
                query.getSourceIds(),
                query.isEnterprise());
    }

    public static CswSubscription getFilterlessSubscription(
            TransformerManager mimeTypeTransformerManager, GetRecordsType request,
            QueryRequest query) throws CswException {
        return new CswSubscription(request,
                Filter.INCLUDE,
                new SendEvent(mimeTypeTransformerManager, request, query),
                null,
                false);
    }

    public GetRecordsType getOriginalRequest() {
        return originalRequest;
    }
}
