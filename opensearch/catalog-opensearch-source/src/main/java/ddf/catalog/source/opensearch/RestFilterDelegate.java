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

package ddf.catalog.source.opensearch;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterDelegate;

/**
 * Used to find Filter objects that can be fulfilled by a DDF REST request.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class RestFilterDelegate extends FilterDelegate<RestUrl> {

    private RestUrl restUrl;

    public RestUrl getRestUrl() {
        return restUrl;
    }

    /**
     * Constructs instance
     * 
     * @param restUrl
     */
    public RestFilterDelegate(RestUrl restUrl) {
        this.restUrl = restUrl;
    }

    @Override
    public RestUrl propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {

        if (!Metacard.ID.equals(propertyName)) {
            throw new UnsupportedOperationException();
        }

        restUrl.setId(literal);

        return restUrl;
    }
}
