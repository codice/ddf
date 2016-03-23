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
package org.codice.ddf.ui.searchui.standard.endpoints;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.InputTransformer;

public class WorkspaceTransformer {

    private final CatalogFramework cf;

    private final InputTransformer it;

    public WorkspaceTransformer(CatalogFramework cf, InputTransformer it) {
        this.cf = cf;
        this.it = it;
    }

    private static QueryMetacardImpl transformQuery(Map<String, Object> m) {
        QueryMetacardImpl q = new QueryMetacardImpl();

        q.setTitle((String) m.get("title"));
        q.setCql((String) m.get("cql"));
        q.setEnterprise((Boolean) m.get("enterprise"));

        return q;
    }

    private String toMetacardXml(Metacard m) {
        try {
            return IOUtils.toString(cf.transform(m, "xml", null)
                    .getInputStream());
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public Metacard transform(Map w) {
        WorkspaceMetacardImpl m = new WorkspaceMetacardImpl();

        m.setTitle((String) w.get("title"));

        if (w.get("queries") != null && List.class.isAssignableFrom(w.get("queries")
                .getClass())) {
            List<Map<String, Object>> queries = (List<Map<String, Object>>) w.get("queries");

            List<String> xmlQueries = queries.stream()
                    .map(WorkspaceTransformer::transformQuery)
                    .map(this::toMetacardXml)
                    .collect(Collectors.toList());

            m.setQueries(xmlQueries);
        }

        if (w.get("metacards") != null && List.class.isAssignableFrom(w.get("metacards")
                .getClass())) {
            m.setMetacards((List) w.get("metacards"));
        }

        return m;
    }

    private Metacard toMetacardFromXml(Serializable xml) {
        try {
            if (xml instanceof String) {
                try (InputStream is = IOUtils.toInputStream((String) xml)) {
                    Metacard m = it.transform(is);
                    return m;
                }
            }
        } catch (Exception ex) {
        }

        return null;
    }

    public Map transform(Metacard m) {
        Map<String, Object> h = new HashMap<>();

        if (m != null) {
            for (AttributeDescriptor ad : m.getMetacardType()
                    .getAttributeDescriptors()) {
                Attribute attr = m.getAttribute(ad.getName());
                if (attr != null) {
                    // ignore metacard tags
                    if (Metacard.TAGS.equals(ad.getName())) continue;

                    if (WorkspaceMetacardTypeImpl.WORKSPACE_QUERIES.equals(ad.getName())) {
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
}
