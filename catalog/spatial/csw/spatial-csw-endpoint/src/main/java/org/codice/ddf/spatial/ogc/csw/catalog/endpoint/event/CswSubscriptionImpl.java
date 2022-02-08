/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event;

import ddf.catalog.event.impl.SubscriptionImpl;
import ddf.catalog.operation.QueryRequest;
import java.util.Set;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import org.codice.ddf.cxf.client.ClientBuilderFactory;
import org.codice.ddf.security.Security;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.opengis.filter.Filter;

public class CswSubscriptionImpl extends SubscriptionImpl
    implements ddf.catalog.event.Subscription {

  private GetRecordsType originalRequest;

  private CswSubscriptionImpl(
      GetRecordsType request,
      Filter filter,
      SendEvent sendEvent,
      Set<String> sourceIds,
      boolean enterprise) {
    super(filter, sendEvent, sourceIds, enterprise);
    this.originalRequest = request;
  }

  public CswSubscriptionImpl(
      TransformerManager mimeTypeTransformerManager,
      GetRecordsType request,
      QueryRequest query,
      ClientBuilderFactory clientBuilderFactory,
      Security security)
      throws CswException {
    this(
        request,
        query.getQuery(),
        new SendEvent(mimeTypeTransformerManager, request, query, clientBuilderFactory, security),
        query.getSourceIds(),
        query.isEnterprise());
  }

  public static CswSubscriptionImpl getFilterlessSubscription(
      TransformerManager mimeTypeTransformerManager,
      GetRecordsType request,
      QueryRequest query,
      ClientBuilderFactory clientBuilderFactory,
      Security security)
      throws CswException {
    return new CswSubscriptionImpl(
        request,
        Filter.INCLUDE,
        new SendEvent(mimeTypeTransformerManager, request, query, clientBuilderFactory, security),
        null,
        false);
  }

  public GetRecordsType getOriginalRequest() {
    return originalRequest;
  }
}
