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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.DefaultAttributeValue;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.plugin.DefaultMetacardAttributePlugin;

public class DefaultAttributeValueRegistry implements DefaultMetacardAttributePlugin {
    private final Map<String, Serializable> globalDefaults = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Serializable>> metacardDefaults =
            new ConcurrentHashMap<>();

    public void addDefaultValue(DefaultAttributeValue defaultAttributeValue, Map props) {
        if (defaultAttributeValue == null) {
            return;
        }
        if (defaultAttributeValue.getMetacardTypeNames() == null
                || defaultAttributeValue.getMetacardTypeNames()
                .isEmpty()) {
            globalDefaults.put(defaultAttributeValue.getAttributeName(),
                    defaultAttributeValue.getDefaultValue());
        } else {
            for (String metacardTypeName : defaultAttributeValue.getMetacardTypeNames()) {
                metacardDefaults.compute(metacardTypeName, (name, defaultAttributeValues) -> {
                    if (defaultAttributeValues == null) {
                        defaultAttributeValues = new HashMap<String, Serializable>();
                    }
                    defaultAttributeValues.put(defaultAttributeValue.getAttributeName(),
                            defaultAttributeValue.getDefaultValue());
                    return defaultAttributeValues;
                });
            }
        }
    }

    public Optional<Serializable> getDefaultValue(String attributeName) {
        notNull(attributeName, "The attribute name cannot be null.");
        return this.getDefaultValue(null, attributeName);
    }

    public Optional<Serializable> getDefaultValue(String metacardTypeName, String attributeName) {
        notNull(attributeName, "The attribute name cannot be null.");

        final Serializable globalDefault = globalDefaults.get(attributeName);
        Serializable metacardDefault = null;
        if (metacardTypeName != null) {
            metacardDefault = metacardDefaults.getOrDefault(metacardTypeName,
                    Collections.emptyMap())
                    .get(attributeName);
        }
        return Optional.ofNullable(metacardDefault != null ? metacardDefault : globalDefault);
    }

    public void removeDefaultValue(DefaultAttributeValue defaultAttributeValue, Map props) {
        if (defaultAttributeValue == null) {
            return;
        }
        if (defaultAttributeValue.getMetacardTypeNames() == null
                || defaultAttributeValue.getMetacardTypeNames()
                .isEmpty()) {
            globalDefaults.remove(defaultAttributeValue.getAttributeName());
        } else {
            for (String metacardTypeName : defaultAttributeValue.getMetacardTypeNames()) {
                metacardDefaults.computeIfPresent(metacardTypeName,
                        (name, defaultAttributeValues) -> {
                            defaultAttributeValues.remove(defaultAttributeValue.getAttributeName());
                            return defaultAttributeValues;
                        });
            }
        }
    }

    private boolean hasNoValue(Attribute attribute) {
        return attribute == null || attribute.getValue() == null;
    }

    @Override
    public Metacard addDefaults(Metacard metacard) {
        MetacardType metacardType = metacard.getMetacardType();
        metacardType.getAttributeDescriptors()
                .stream()
                .map(AttributeDescriptor::getName)
                .filter(attributeName -> hasNoValue(metacard.getAttribute(attributeName)))
                .forEach(attributeName -> {
                    getDefaultValue(metacardType.getName(),
                            attributeName).ifPresent(defaultValue -> metacard.setAttribute(new AttributeImpl(
                            attributeName,
                            defaultValue)));
                });
        return metacard;
    }
}
