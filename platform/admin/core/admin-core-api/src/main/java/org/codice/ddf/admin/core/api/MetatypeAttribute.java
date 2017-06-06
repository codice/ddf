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

import java.util.Map;

/**
 * The individual attribute definition within a metatype's object class definition
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface MetatypeAttribute extends Map<String, Object> {

    String ID = "id";

    String NAME = "name";

    String CARDINALITY = "cardinality";

    String DEFAULT_VALUE = "defaultValue";

    String DESCRIPTION = "description";

    String TYPE = "type";

    String OPTION_LABELS = "optionLabels";

    String OPTION_VALUES = "optionValues";

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

    default int getCardinality() {
        return (int) get(CARDINALITY);
    }

    default void setCardinality(int cardinality) {
        put(CARDINALITY, cardinality);
    }

    default String[] getDefaultValue() {
        return (String[]) get(DEFAULT_VALUE);
    }

    default void setDefaultValue(String[] defaultValue) {
        put(DEFAULT_VALUE, defaultValue);
    }

    default String getDescription() {
        return (String) get(DESCRIPTION);
    }

    default void setDescription(String description) {
        put(DESCRIPTION, description);
    }

    default int getType() {
        return (int) get(TYPE);
    }

    default void setType(int type) {
        put(TYPE, type);
    }

    default String[] getOptionLabels() {
        return (String[]) get(OPTION_LABELS);
    }

    default void setOptionLabels(String[] optionLabels) {
        put(OPTION_LABELS, optionLabels);
    }

    default String[] getOptionValues() {
        return (String[]) get(OPTION_VALUES);
    }

    default void setOptionValues(String[] optionValues) {
        put(OPTION_VALUES, optionValues);
    }
}
