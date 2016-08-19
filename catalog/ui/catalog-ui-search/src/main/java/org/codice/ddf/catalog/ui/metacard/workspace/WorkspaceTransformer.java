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
 **/
package org.codice.ddf.catalog.ui.metacard.workspace;

import java.io.InputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.InputTransformer;

public class WorkspaceTransformer {

    private final CatalogFramework catalogFramework;

    private final InputTransformer inputTransformer;

    public WorkspaceTransformer(CatalogFramework catalogFramework,
            InputTransformer inputTransformer) {
        this.catalogFramework = catalogFramework;
        this.inputTransformer = inputTransformer;
    }

    private static boolean check(Object o, Class<?> clazz) {
        return o != null && clazz.isAssignableFrom(o.getClass());
    }

    @SuppressWarnings("unchecked")
    public QueryMetacardImpl toQuery(Map<String, Object> map) {
        QueryMetacardImpl query = new QueryMetacardImpl();

        if (check(map.get(Metacard.TITLE), String.class)) {
            query.setTitle((String) map.get(Metacard.TITLE));
        }

        if (check(map.get(Metacard.ID), String.class)) {
            query.setId((String) map.get(Metacard.ID));
        }

        if (check(map.get(QueryMetacardTypeImpl.QUERY_CQL), String.class)) {
            query.setCql((String) map.get(QueryMetacardTypeImpl.QUERY_CQL));
        }

        if (check(map.get(QueryMetacardTypeImpl.QUERY_ENTERPRISE), Boolean.class)) {
            query.setEnterprise((Boolean) map.get(QueryMetacardTypeImpl.QUERY_ENTERPRISE));
        } else if (check(map.get(QueryMetacardTypeImpl.QUERY_SOURCES), List.class)) {
            query.setSources((List) map.get(QueryMetacardTypeImpl.QUERY_SOURCES));
            // the front-end uses src everywhere instead of sources, this should provide a quick and simple fix
        } else if (check(map.get("src"), List.class)) {
            query.setSources((List) map.get("src"));
        }

        return query;
    }

    @SuppressWarnings("unchecked")
    private WorkspaceMetacardImpl toWorkspace(Map<String, Object> w) {
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();

        w.entrySet()
                .stream()
                .map(entry -> {
                    if (WorkspaceAttributes.WORKSPACE_QUERIES.equals(entry.getKey())) {
                        List<Map<String, Object>> queries = (List) entry.getValue();

                        List<String> xmlQueries = queries.stream()
                                .map(this::toQuery)
                                .map(this::toMetacardXml)
                                .collect(Collectors.toList());

                        return new AbstractMap.SimpleEntry<>(entry.getKey(), xmlQueries);
                    }

                    return entry;
                })
                .forEach(entry -> {
                    Object value = entry.getValue();
                    if (value instanceof Serializable) {
                        workspace.setAttribute(entry.getKey(), (Serializable) value);
                    } else if (value instanceof List) {
                        workspace.setAttribute(entry.getKey(), new ArrayList<>((List) value));
                    }
                });

        return workspace;
    }

    public WorkspaceMetacardImpl transform(Map<String, Object> w) {
        return toWorkspace(w);
    }

    public Map<String, Object> transform(Metacard m) {
        Map<String, Object> h = new HashMap<>();

        if (m != null) {
            for (AttributeDescriptor ad : m.getMetacardType()
                    .getAttributeDescriptors()) {
                Attribute attr = m.getAttribute(ad.getName());
                if (attr != null) {
                    // ignore metacard tags
                    if (Metacard.TAGS.equals(ad.getName())) {
                        continue;
                    }

                    if (QueryMetacardTypeImpl.QUERY_SOURCES.equals(ad.getName())) {
                        h.put("src", attr.getValues());
                    } else if (WorkspaceAttributes.WORKSPACE_SHARING.equals(ad.getName())) {
                        h.put(ad.getName(),
                                attr.getValues()
                                        .stream()
                                        .map(this::toMetacardFromXml)
                                        .map(this::transform)
                                        .collect(Collectors.toList()));
                    } else if (WorkspaceAttributes.WORKSPACE_QUERIES.equals(ad.getName())) {
                        h.put(ad.getName(),
                                attr.getValues()
                                        .stream()
                                        .map(this::toMetacardFromXml)
                                        .map(this::transform)
                                        .collect(Collectors.toList()));
                    } else if (ad.isMultiValued()) {
                        h.put(ad.getName(), attr.getValues());
                    } else {
                        h.put(ad.getName(), attr.getValue());
                    }
                }
            }
        }

        return h;
    }

    public List<Map<String, Object>> transform(List<Metacard> metacards) {
        return metacards.stream()
                .map(this::transform)
                .collect(Collectors.toList());
    }

    public String toMetacardXml(Metacard m) {
        try {
            return IOUtils.toString(catalogFramework.transform(m, "xml", null)
                    .getInputStream());
        } catch (Exception e) {
            return "";
        }
    }

    public Metacard toMetacardFromXml(Serializable xml) {
        try {
            if (xml instanceof String) {
                try (InputStream is = IOUtils.toInputStream((String) xml)) {
                    return inputTransformer.transform(is);
                }
            }
        } catch (Exception ex) {
            // TODO (RCZ) - wat do here
            throw new RuntimeException(ex);
        }

        return null;
    }

}
