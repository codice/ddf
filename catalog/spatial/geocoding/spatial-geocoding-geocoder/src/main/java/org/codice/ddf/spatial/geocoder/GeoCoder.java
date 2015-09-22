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
package org.codice.ddf.spatial.geocoder;

public interface GeoCoder {
    /**
     * Takes a query for a place and returns the most relevant result.
     *
     * @param location  a string representing a simple placename query, such as "Washington, D.C."
     *                  or "France" (i.e. the string just contains search terms, not query logic)
     * @return the {@link GeoResult} most relevant to the query, null if no results were found
     */
    GeoResult getLocation(String location);
}
