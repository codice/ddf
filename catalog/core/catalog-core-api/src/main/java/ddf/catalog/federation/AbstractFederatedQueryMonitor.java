/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.federation;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.QueryResponseImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.Source;

/**
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.federation.base.AbstractFederatedQueryMonitor
 */
@SuppressWarnings("unused")
@Deprecated
public abstract class AbstractFederatedQueryMonitor implements Runnable {

    private QueryResponse returnResults;

    private Map<Source, Future<SourceResponse>> futures;

    private Query query;

    private ExecutorService pool;

    public AbstractFederatedQueryMonitor(ExecutorService pool,
            Map<Source, Future<SourceResponse>> futures2, QueryResponseImpl returnResults,
            Query query) {
        this.pool = pool;
        this.returnResults = returnResults;
        this.query = query;
        this.futures = futures2;
    }

    public abstract void run();

}
