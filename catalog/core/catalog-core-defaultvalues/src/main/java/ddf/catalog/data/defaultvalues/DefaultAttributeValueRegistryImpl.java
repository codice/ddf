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
package ddf.catalog.data.defaultvalues;

import static org.apache.commons.lang.Validate.notNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ddf.catalog.data.DefaultAttributeValueRegistry;

public class DefaultAttributeValueRegistryImpl implements DefaultAttributeValueRegistry {
    private final Map<String, Serializable> globalDefaults = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Serializable>> metacardDefaults =
            new ConcurrentHashMap<>();

    @Override
    public void setDefaultValue(String attributeName, Serializable defaultValue) {
        notNull(attributeName, "The attribute name cannot be null");
        notNull(defaultValue, "The default value cannot be null");

        globalDefaults.put(attributeName, defaultValue);
    }

    @Override
    public void setDefaultValue(String metacardTypeName, String attributeName,
            Serializable defaultValue) {
        notNull(metacardTypeName, "The metacard type name cannot be null.");
        notNull(attributeName, "The attribute name cannot be null.");
        notNull(defaultValue, "The default value cannot be null.");

        metacardDefaults.compute(metacardTypeName, (name, defaultAttributeValues) -> {
            if (defaultAttributeValues == null) {
                final Map<String, Serializable> newDefaults = new HashMap<>();
                newDefaults.put(attributeName, defaultValue);
                return newDefaults;
            } else {
                defaultAttributeValues.put(attributeName, defaultValue);
                return defaultAttributeValues;
            }
        });
    }

    @Override
    public Optional<Serializable> getDefaultValue(String metacardTypeName, String attributeName) {
        notNull(metacardTypeName, "The metacard type name cannot be null.");
        notNull(attributeName, "The attribute name cannot be null.");

        final Serializable globalDefault = globalDefaults.get(attributeName);
        final Serializable metacardDefault = metacardDefaults.getOrDefault(metacardTypeName,
                Collections.emptyMap())
                .get(attributeName);
        return Optional.ofNullable(metacardDefault != null ? metacardDefault : globalDefault);
    }

    @Override
    public void removeDefaultValue(String attributeName) {
        notNull(attributeName, "The attribute name cannot be null");

        globalDefaults.remove(attributeName);
    }

    @Override
    public void removeDefaultValue(String metacardTypeName, String attributeName) {
        notNull(metacardTypeName, "The metacard type name cannot be null.");
        notNull(attributeName, "The attribute name cannot be null.");

        metacardDefaults.computeIfPresent(metacardTypeName, (name, defaultAttributeValues) -> {
            defaultAttributeValues.remove(attributeName);
            return defaultAttributeValues;
        });
    }

    @Override
    public void removeDefaultValues() {
        globalDefaults.clear();
    }

    @Override
    public void removeDefaultValues(String metacardTypeName) {
        notNull(metacardTypeName, "The metacard type name cannot be null.");

        metacardDefaults.remove(metacardTypeName);
    }
}
