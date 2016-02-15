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
 **/
package ddf.catalog.security.filter.strategy;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.Response;
import ddf.catalog.security.FilterResult;
import ddf.catalog.security.FilterStrategy;
import ddf.catalog.security.impl.FilterResultImpl;

public class FilteringStrategy implements FilterStrategy {

    @Override
    public FilterResult process(Response response, Metacard metacard) {
        return new FilterResultImpl(true);
    }
}
