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
package ddf.catalog.operation;

import java.util.List;
import java.util.Set;

import ddf.catalog.data.Result;

public interface QueryResponse extends SourceResponse {

    public static final String TOTAL_HITS = "total-hits";

    public static final String SITE_LIST = "site-list";

    public static final String TOTAL_RESULTS_RETURNED = "total-results-returned";

    public static final String ELAPSED_TIME = "elapsed-time";

    @Override
    public Set<ProcessingDetails> getProcessingDetails();

    public boolean hasMoreResults();

    public Result poll();

    public Result poll(long timeout);

    // public Collection<Result> poll( long timeout, long size ) throws InterruptedException;

    public Result take();

    public List<Result> take(long resultSize);

}
