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
 * System properties
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface SystemPropertyDetails extends Map<String, Object> {
    String TITLE = "title";

    String DESCRIPTION = "description";

    String OPTIONS = "options";

    String KEY = "key";

    String VALUE = "value";

    String DEFAULT_VALUE = "defaultValue";

    default String getTitle() {
        return (String) get(TITLE);
    }

    default void setTitle(String title) {
        put(TITLE, title);
    }

    default String getDescription() {
        return (String) get(DESCRIPTION);
    }

    default void setDescription(String description) {
        put(DESCRIPTION, description);
    }

    default List<String> getOptions() {
        return (List<String>) get(OPTIONS);
    }

    default void setOptions(List<String> values) {
        put(OPTIONS, values);
    }

    default String getKey() {
        return (String) get(KEY);
    }

    default void setKey(String key) {
        put(KEY, key);
    }

    default String getValue() {
        return (String) get(VALUE);
    }

    default void setValue(String value) {
        put(VALUE, value);
    }

    default String getDefaultValue() {
        return (String) get(DEFAULT_VALUE);
    }

    default void setDefaultValue(String defaultValue) {
        put(DEFAULT_VALUE, defaultValue);
    }
}
