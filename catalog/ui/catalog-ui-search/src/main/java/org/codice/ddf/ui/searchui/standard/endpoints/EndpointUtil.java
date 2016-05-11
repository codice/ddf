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
package org.codice.ddf.ui.searchui.standard.endpoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class EndpointUtil {
    private final List<MetacardType> metacardTypes;

    private final CatalogFramework catalogFramework;

    private final FilterBuilder filterBuilder;

    public EndpointUtil(List<MetacardType> metacardTypes, CatalogFramework catalogFramework,
            FilterBuilder filterBuilder) {
        this.metacardTypes = metacardTypes;
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
    }

    public Metacard getMetacard(String id)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            StandardSearchException {
        Filter idFilter = filterBuilder.attribute(Metacard.ID)
                .is()
                .equalTo()
                .text(id);
        Filter tagsFilter = filterBuilder.attribute(Metacard.TAGS)
                .is()
                .like()
                .text("*");
        Filter filter = filterBuilder.allOf(idFilter, tagsFilter);

        QueryResponse queryResponse = catalogFramework.query(new QueryRequestImpl(new QueryImpl(
                filter), true));

        if (queryResponse.getHits() == 0) {
            throw new StandardSearchException("Could not find metacard for that metacard id");
        }

        Result result = queryResponse.getResults()
                .get(0);
        return result.getMetacard();
    }

    public List<String> getStringList(List<Serializable> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    public Map<String, Result> getMetacards(Collection<String> ids, String tagFilter)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        if (ids.isEmpty()) {
            return new HashMap<>();
        }

        List<Filter> filters = new ArrayList<>(ids.size());
        for (String id : ids) {
            Filter historyFilter = filterBuilder.attribute(Metacard.ID)
                    .is()
                    .equalTo()
                    .text(id);
            Filter idFilter = filterBuilder.attribute(Metacard.TAGS)
                    .is()
                    .like()
                    .text(tagFilter);
            Filter filter = filterBuilder.allOf(historyFilter, idFilter);
            filters.add(filter);
        }

        Filter queryFilter = filterBuilder.anyOf(filters);
        QueryResponse response = catalogFramework.query(new QueryRequestImpl(new QueryImpl(
                queryFilter,
                1,
                -1,
                SortBy.NATURAL_ORDER,
                false,
                TimeUnit.SECONDS.toMillis(10)), false));
        Map<String, Result> results = new HashMap<>();
        for (Result result : response.getResults()) {
            results.put(result.getMetacard()
                    .getId(), result);
        }
        return results;
    }

    public Map<String, Object> getMetacardTypeMap() {
        Map<String, Object> resultTypes = new HashMap<>();
        for (MetacardType metacardType : metacardTypes) {
            Map<String, Object> attributes = new HashMap<>();
            for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
                Map<String, Object> attributeProperties = new HashMap<>();
                attributeProperties.put("type",
                        descriptor.getType()
                                .getAttributeFormat()
                                .name());
                attributeProperties.put("multivalued", descriptor.isMultiValued());
                attributeProperties.put("id", descriptor.getName());
                attributes.put(descriptor.getName(), attributeProperties);
            }
            resultTypes.put(metacardType.getName(), attributes);
        }
        return resultTypes;
    }

    public Map<String, Object> transformToJson(Metacard metacard) {
        return transformToJson(Collections.singletonList(metacard));
    }

    public Map<String, Object> transformToJson(List<Metacard> metacards) {
        List<Map<String, Object>> metacardJsons = metacards.stream()
                .map(this::getMetacardJson)
                .collect(Collectors.toList());

        Set<String> types = metacards.stream()
                .map(Metacard::getMetacardType)
                .map(MetacardType::getName)
                .collect(Collectors.toSet());

        List<Map<String, Object>> typesList = new ArrayList<>();
        for (String type : types) {
            Map<String, Object> typeMap = new HashMap<>();
            typeMap.put("type-name", type);
            typeMap.put("type", getMetacardTypeMap().get(type));

            // TODO (RCZ) - optimize this since might be hit a lot
            typeMap.put("ids",
                    metacards.stream()
                            .filter(mc -> type.equals(mc.getMetacardType()
                                    .getName()))
                            .map(Metacard::getId)
                            .collect(Collectors.toList()));
            typesList.add(typeMap);
        }

        Map<String, Object> outerMap = new HashMap<>();
        outerMap.put("metacards", metacardJsons);
        outerMap.put("metacard-types", typesList);

        return outerMap;
    }

    private Map<String, Object> getMetacardJson(Metacard metacard) {
        Set<AttributeDescriptor> attributeDescriptors = metacard.getMetacardType()
                .getAttributeDescriptors();
        Map<String, Object> result = new HashMap<>();
        for (AttributeDescriptor descriptor : attributeDescriptors) {
            if (metacard.getAttribute(descriptor.getName()) == null) {
                if (descriptor.isMultiValued()) {
                    result.put(descriptor.getName(), Collections.emptyList());
                } else {
                    result.put(descriptor.getName(), null);
                }
                continue;
            }
            if (Metacard.THUMBNAIL.equals(descriptor.getName())) {
                if (metacard.getThumbnail() != null) {
                    result.put(descriptor.getName(),
                            Base64.getEncoder()
                                    .encodeToString(metacard.getThumbnail()));
                } else {
                    result.put(descriptor.getName(), null);
                }
                continue;

            }
            if (descriptor.isMultiValued()) {
                result.put(descriptor.getName(),
                        metacard.getAttribute(descriptor.getName())
                                .getValues());
            } else {
                result.put(descriptor.getName(),
                        metacard.getAttribute(descriptor.getName())
                                .getValue());
            }
        }
        return result;
    }

    public String getJson(Object result) {
        return JsonFactory.create(new JsonParserFactory(),
                new JsonSerializerFactory().includeNulls()
                        .includeEmpty())
                .toJson(result);
    }

    public Optional<MetacardType> getMetacardType(String name) {
        return metacardTypes.stream()
                .filter(mt -> mt.getName()
                        .equals(name))
                .findFirst();
    }
}
