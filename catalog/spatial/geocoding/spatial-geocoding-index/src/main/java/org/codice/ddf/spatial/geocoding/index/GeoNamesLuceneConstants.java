/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.geocoding.index;

/**
 * Contains the names of the fields in the GeoNames Lucene index.
 */
public class GeoNamesLuceneConstants {
    public static final String NAME_FIELD = "name";
    public static final String LATITUDE_FIELD = "latitude";
    public static final String LONGITUDE_FIELD = "longitude";
    public static final String FEATURE_CODE_FIELD = "feature_code";
    public static final String POPULATION_FIELD = "population";
    public static final String ALTERNATE_NAMES_FIELD = "alternate_names";
    public static final String BOOST_FIELD = "boost";
}
