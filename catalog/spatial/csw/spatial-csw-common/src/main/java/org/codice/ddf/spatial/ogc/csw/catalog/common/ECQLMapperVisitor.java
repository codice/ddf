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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.util.HashMap;
import java.util.Map;

/**
 * The ECQL.toCql method doesn't escape (double quote) all reserved words which causes
 * problems when then using ECQL.toFilter on cql that has one of those key words as a property name.
 * This visitor creates a new filter with those keywords quoted so ECQL.toFilter will work.
 */
public class ECQLMapperVisitor extends PropertyMapperVisitor {
    private static Map<String, String> ecqlMappings;

    static {
        ecqlMappings = new HashMap<>();
        ecqlMappings.put("id", "\"id\"");
        ecqlMappings.put("text", "\"text\"");
        ecqlMappings.put("temporal", "\"temporal\"");
        ecqlMappings.put("between", "\"between\"");
        ecqlMappings.put("spatial", "\"spatial\"");
    }

    public ECQLMapperVisitor() {
        super(ecqlMappings);
    }
}
