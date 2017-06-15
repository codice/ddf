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
package org.codice.ddf.catalog.ui.metacard.transform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.boon.json.annotations.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class CsvTransform {
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvTransform.class);

    private boolean applyGlobalHidden = false;

    private Set<String> hiddenFields;

    private List<String> columnOrder;

    private Map<String, String> columnAliasMap;

    @JsonIgnore
    private List<Map<String, Object>> metacards;

    public Set<String> getHiddenFields() {
        return hiddenFields;
    }

    public void setHiddenFields(Set<String> hiddenFields) {
        this.hiddenFields = hiddenFields;
    }

    public List<String> getColumnOrder() {
        return columnOrder;
    }

    public void setColumnOrder(List<String> columnOrder) {
        this.columnOrder = columnOrder;
    }

    public Map<String, String> getColumnAliasMap() {
        return columnAliasMap;
    }

    public void setColumnAliasMap(Map<String, String> columnAliasMap) {
        this.columnAliasMap = columnAliasMap;
    }

    public List<Map<String, Object>> getMetacards() {
        return metacards;
    }

    public void setMetacards(List<Map<String, Object>> metacards) {
        this.metacards = metacards;
    }

    public boolean isApplyGlobalHidden() {
        return applyGlobalHidden;
    }

    public void setApplyGlobalHidden(boolean applyGlobalHidden) {
        this.applyGlobalHidden = applyGlobalHidden;
    }

    public List<Metacard> getTransformedMetacards(List<MetacardType> metacardTypes,
            AttributeRegistry attributeRegistry) {
        return metacards.stream()
                .map((Map<String, Object> mapMetacard) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> properties =
                            (Map<String, Object>) ((Map<String, Object>) mapMetacard.getOrDefault(
                                    "metacard",
                                    new HashMap<>())).getOrDefault("properties", new HashMap<>());

                    MetacardType metacardType = getMetacardType(properties,
                            metacardTypes,
                            attributeRegistry);

                    Metacard metacard = new MetacardImpl(metacardType);

                    properties.entrySet()
                            .stream()
                            .map(entry -> {
                                String key = entry.getKey();
                                Object val = entry.getValue();
                                if (val == null) {
                                    return null;
                                }
                                if (val instanceof List) {
                                    List list = (List) val;
                                    if (list.size() > 0 && !(list.get(0) instanceof Serializable)) {
                                        return null;
                                    }
                                    return new AttributeImpl(key,
                                            (List<Serializable>) new ArrayList<Serializable>((List) val));
                                }
                                if (!(val instanceof Serializable)) {
                                    LOGGER.debug(
                                            "Attribute value for [{}] is not serializable. Value: {}",
                                            key,
                                            val);
                                    return null;
                                }
                                return new AttributeImpl(key, (Serializable) val);
                            })
                            .filter(Objects::nonNull)
                            .forEach(metacard::setAttribute);

                    return metacard;
                })
                .collect(Collectors.toList());
    }

    private MetacardType getMetacardType(Map<String, Object> mapMetacard,
            List<MetacardType> metacardTypes, AttributeRegistry attributeRegistry) {
        String metacardTypeName = (String) mapMetacard.getOrDefault("metacard-type",
                "ddf.metacard");

        MetacardType metacardType = metacardTypes.stream()
                .filter(type -> type.getName()
                        .equals(metacardTypeName))
                .findFirst()
                .orElse(new MetacardTypeImpl(metacardTypeName, ImmutableList.of()));

        // Don't assume metacard type will contain all necessary attributes (eg, injected attributes)
        Set<String> existingAttributeDescriptors = metacardType.getAttributeDescriptors()
                .stream()
                .map(AttributeDescriptor::getName)
                .collect(Collectors.toSet());

        Set<AttributeDescriptor> missingDescriptors = mapMetacard.keySet()
                .stream()
                .filter(key -> !existingAttributeDescriptors.contains(key))
                .map(attributeRegistry::lookup)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (!missingDescriptors.isEmpty()) {
            metacardType = new MetacardTypeImpl(metacardTypeName, metacardType, missingDescriptors);
        }
        return metacardType;
    }
}
