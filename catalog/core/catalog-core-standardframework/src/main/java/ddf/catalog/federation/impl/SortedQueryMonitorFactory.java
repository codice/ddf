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
package ddf.catalog.federation.impl;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import org.apache.shiro.util.ThreadContext;

class SortedQueryMonitorFactory {

  public Runnable createMonitor(
      final CompletionService<SourceResponse> completionService,
      final Map<Future<SourceResponse>, QueryRequest> futures,
      final QueryResponseImpl returnResults,
      final QueryRequest request,
      List<PostFederatedQueryPlugin> postQuery) {

    return new SortedQueryMonitor(
        ThreadContext.getResources(),
        completionService,
        futures,
        returnResults,
        request,
        postQuery);
  }
}
