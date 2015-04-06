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
package ddf.catalog;

import ddf.catalog.operation.QueryResponse;

/**
 * Interface implemented by classes that perform post-processing on {@link QueryResponse}. Instances
 * of this interface will be called by the catalog framework after all the results have been
 * received from the different federated sources, before any of the post-query plug-ins are called.
 */
public interface QueryResponsePostProcessor {

    /**
     * Performs any required post-processing on the {@link QueryResponse} object provided.
     * 
     * @param response
     *            {@link QueryResponse} to process. Cannot be <code>null</code>.
     */
    public void processResponse(QueryResponse response);
}
