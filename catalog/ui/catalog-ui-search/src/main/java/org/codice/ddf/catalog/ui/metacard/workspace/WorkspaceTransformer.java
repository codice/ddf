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
import java.util.HashSet;
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

    @SuppressWarnings("unchecked")
    private static QueryMetacardImpl transformQuery(Map<String, Object> m) {
        QueryMetacardImpl q = new QueryMetacardImpl();

        if (check(m.get(Metacard.TITLE), String.class)) {
            q.setTitle((String) m.get(Metacard.TITLE));
        }

        if (check(m.get(Metacard.ID), String.class)) {
            q.setId((String) m.get(Metacard.ID));
        }

        if (check(m.get(QueryMetacardTypeImpl.QUERY_CQL), String.class)) {
            q.setCql((String) m.get(QueryMetacardTypeImpl.QUERY_CQL));
        }

        if (check(m.get(QueryMetacardTypeImpl.QUERY_ENTERPRISE), Boolean.class)) {
            q.setEnterprise((Boolean) m.get(QueryMetacardTypeImpl.QUERY_ENTERPRISE));
        } else if (check(m.get(QueryMetacardTypeImpl.QUERY_SOURCES), List.class)) {
            q.setSources((List) m.get(QueryMetacardTypeImpl.QUERY_SOURCES));
        }

        return q;
    }


    @SuppressWarnings("unchecked")
    public WorkspaceMetacardImpl transform(Map w) {
        WorkspaceMetacardImpl m = new WorkspaceMetacardImpl();

        if (check(w.get(Metacard.ID), String.class)) {
            m.setId((String) w.get(Metacard.ID));
        }

        if (check(w.get(Metacard.TITLE), String.class)) {
            m.setTitle((String) w.get(Metacard.TITLE));
        }

        if (check(w.get(WorkspaceMetacardTypeImpl.WORKSPACE_METACARDS), List.class)) {
            m.setMetacards((List) w.get(WorkspaceMetacardTypeImpl.WORKSPACE_METACARDS));
        }

        if (check(w.get(WorkspaceMetacardTypeImpl.WORKSPACE_ROLES), List.class)) {
            m.setRoles(new HashSet<>((List) w.get(WorkspaceMetacardTypeImpl.WORKSPACE_ROLES)));
        }

        if (check(w.get(WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES), List.class)) {
            List<Map<String, Object>> queries = (List<Map<String, Object>>) w.get(
                    WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES);

            List<String> xmlQueries = queries.stream()
                    .map(WorkspaceTransformer::transformQuery)
                    .map(this::toMetacardXml)
                    .collect(Collectors.toList());

            m.setQueries(xmlQueries);
        }

        return m;
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

    private static boolean check(Object o, Class<?> clazz) {
        return o != null && clazz.isAssignableFrom(o.getClass());
    }

    private String toMetacardXml(Metacard m) {
        try {
            return IOUtils.toString(catalogFramework.transform(m, "xml", null)
                    .getInputStream());
        } catch (Exception e) {
            return "";
        }
    }

    private Metacard toMetacardFromXml(Serializable xml) {
        try {
            if (xml instanceof String) {
                try (InputStream is = IOUtils.toInputStream((String) xml)) {
                    Metacard m = inputTransformer.transform(is);
                    return m;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);// TODO (RCZ) - wat do here
        }

        return null;
    }
}
