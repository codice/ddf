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

package ddf.catalog.source.opensearch.impl;

import java.util.List;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SimpleFilterDelegate;

/**
 * Used to find Filter objects that can be fulfilled by a DDF REST request.
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public class RestFilterDelegate extends SimpleFilterDelegate<RestUrl> {

    private RestUrl restUrl;

    /**
     * Constructs instance
     *
     * @param restUrl
     */
    public RestFilterDelegate(RestUrl restUrl) {
        this.restUrl = restUrl;
    }

    public RestUrl getRestUrl() {
        return restUrl;
    }

    @Override
    public <S> RestUrl defaultOperation(Object property, S literal, Class<S> literalClass,
            Enum operation) {
        return null;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        if (Metacard.ID.equals(propertyName)) {
            restUrl.setId(literal);
            return restUrl;
        }
        return null;
    }

    @Override
    public RestUrl and(List<RestUrl> operands) {
        return findFirstOperand(operands);
    }

    @Override
    public RestUrl or(List<RestUrl> operands) {
        return findFirstOperand(operands);
    }

    private RestUrl findFirstOperand(List<RestUrl> restUrls) {
        for (RestUrl restUrl : restUrls) {
            if (restUrl != null) {
                return restUrl;
            }
        }
        return null;
    }
}
