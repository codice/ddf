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
package org.codice.ddf.catalog.ui.filter.json;

public class FilterJson {

  public static class Keys {

    // Required on every predicate
    public static final String TYPE = "type";

    // Contains the children of a logic node
    public static final String FILTERS = "filters";

    // Terminal predicate properties
    public static final String PROPERTY = "property";
    public static final String VALUE = "value";

    // Function properties
    public static final String NAME = "name";
    public static final String PARAMS = "params";

    @Deprecated public static final String TEMPLATE_PROPS = "templateProperties";

    private Keys() {}
  }

  public static class Ops {

    public static final String FUNC = "FILTER_FUNCTION";

    // Comparison Operators
    public static final String EQ = "=";
    public static final String NOT_EQ = "!=";
    public static final String GT = ">";
    public static final String GT_OR_ET = ">=";
    public static final String LT = "<";
    public static final String LT_OR_ET = "<=";
    public static final String ILIKE = "ILIKE";
    public static final String LIKE = "LIKE";

    // Temporal Operators
    public static final String BEFORE = "BEFORE";
    public static final String AFTER = "AFTER";

    // Spatial Operators
    public static final String INTERSECTS = "INTERSECTS";
    public static final String DWITHIN = "DWITHIN";

    // Logic Operators
    public static final String AND = "AND";
    public static final String OR = "OR";
    public static final String NOT = "NOT";

    private Ops() {}
  }

  private FilterJson() {}
}
