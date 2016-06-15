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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        }

        return query;
    }

    public SharingMetacardImpl toSharing(Map<String, Object> map) {
        SharingMetacardImpl sharing = new SharingMetacardImpl();

        Object type = map.get(SharingMetacardTypeImpl.SHARING_ATTRIBUTE);
        if (check(type, String.class)) {
            sharing.setSharingAttribute((String) type);
        }

        Object permission = map.get(SharingMetacardTypeImpl.SHARING_ACTION);
        if (check(permission, String.class)) {
            sharing.setAction((String) permission);
        }

        Object value = map.get(SharingMetacardTypeImpl.SHARING_VALUE);
        if (check(value, String.class)) {
            sharing.setValue((String) value);
        }

        return sharing;
    }

    @SuppressWarnings("unchecked")
    public WorkspaceMetacardImpl toWorkspace(Map<String, Object> w) {
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();

        if (check(w.get(Metacard.ID), String.class)) {
            workspace.setId((String) w.get(Metacard.ID));
        }

        if (check(w.get(Metacard.TITLE), String.class)) {
            workspace.setTitle((String) w.get(Metacard.TITLE));
        }

        if (check(w.get(WorkspaceMetacardTypeImpl.WORKSPACE_OWNER), String.class)) {
            workspace.setOwner((String) w.get(WorkspaceMetacardTypeImpl.WORKSPACE_OWNER));
        }

        if (check(w.get(WorkspaceMetacardTypeImpl.WORKSPACE_METACARDS), List.class)) {
            workspace.setMetacards((List) w.get(WorkspaceMetacardTypeImpl.WORKSPACE_METACARDS));
        }

        if (check(w.get(WorkspaceMetacardTypeImpl.WORKSPACE_SHARING), List.class)) {
            List<Map<String, Object>> sharing = (List<Map<String, Object>>) w.get(
                    WorkspaceMetacardTypeImpl.WORKSPACE_SHARING);

            Set<String> xmlSharing = sharing.stream()
                    .map(this::toSharing)
                    .map(this::toMetacardXml)
                    .collect(Collectors.toSet());

            workspace.setSharing(xmlSharing);
        }

        if (check(w.get(WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES), List.class)) {
            List<Map<String, Object>> queries = (List<Map<String, Object>>) w.get(
                    WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES);

            List<String> xmlQueries = queries.stream()
                    .map(this::toQuery)
                    .map(this::toMetacardXml)
                    .collect(Collectors.toList());

            workspace.setQueries(xmlQueries);
        }

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

                    if (Metacard.RELATED.equals(ad.getName())) {
                        h.put(WorkspaceMetacardTypeImpl.WORKSPACE_METACARDS, attr.getValues());
                    } else if (WorkspaceMetacardTypeImpl.WORKSPACE_SHARING.equals(ad.getName())) {
                        h.put(ad.getName(),
                                attr.getValues()
                                        .stream()
                                        .map(this::toMetacardFromXml)
                                        .map(this::transform)
                                        .collect(Collectors.toList()));
                    } else if (WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES.equals(ad.getName())) {
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
                    Metacard m = inputTransformer.transform(is);
                    return m;
                }
            }
        } catch (Exception ex) {
            // TODO (RCZ) - wat do here
            throw new RuntimeException(ex);
        }

        return null;
    }

}
