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
package ddf.catalog.transform;

import java.io.Serializable;
import java.util.Map;

import ddf.catalog.operation.QueryRequest;

/**
 * Transforms a {@link QueryRequest} containing external attribute names & values into a
 * normalized taxonomy {@link QueryRequest}.
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface QueryFilterTransformer {

    /**
     * Transforms a {@link QueryRequest}. {@link QueryRequest} is used in order to allow
     * the transformer to directly modify other properties (e.g. sourceId) based on criteria
     * without requiring the endpoint to modify the request after conversion
     *
     * @param queryRequest - the {@link QueryRequest} to transform
     * @param properties - a map of additional properties to be used by the transformer
     * @return the transformed {@link QueryRequest}
     */
    QueryRequest transform(QueryRequest queryRequest,  Map<String, Serializable> properties);
}
