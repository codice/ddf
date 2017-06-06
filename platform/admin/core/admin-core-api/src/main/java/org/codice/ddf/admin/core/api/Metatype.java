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
package org.codice.ddf.admin.core.api;

import java.util.List;
import java.util.Map;

/**
 * Top level object that represents a metatype
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface Metatype extends Map<String, Object> {
    String ID = "id";

    String NAME = "name";

    String ATTRIBUTE_DEFINITIONS = "metatype";

    default String getId() {
        return (String) get(ID);
    }

    default void setId(String id) {
        put(ID, id);
    }

    default String getName() {
        return (String) get(NAME);
    }

    default void setName(String name) {
        put(NAME, name);
    }

    default List<MetatypeAttribute> getAttributeDefinitions() {
        return (List<MetatypeAttribute>) get(ATTRIBUTE_DEFINITIONS);
    }

    default void setAttributeDefinitions(List<MetatypeAttribute> attributeDefinitions) {
        put(ATTRIBUTE_DEFINITIONS, attributeDefinitions);
    }
}
