/*
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

package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import ddf.catalog.transform.QueryFilterTransformer;

public class QueryFilterTransformerHelper {
    private Map<QName, QueryFilterTransformer> queryFilterTransformerMap =
            new ConcurrentHashMap<>();

    public void bind(ServiceReference<QueryFilterTransformer> reference) {
        if (reference == null) {
            return;
        }

        QName namespace = getNamespace(reference);
        QueryFilterTransformer transformer = getTransformer(reference);
        queryFilterTransformerMap.put(namespace, transformer);
    }

    public void unbind(ServiceReference<QueryFilterTransformer> reference) {
        if (reference == null) {
            return;
        }

        QName namespace = getNamespace(reference);
        queryFilterTransformerMap.remove(namespace);

        getBundleContext().ungetService(reference);
    }

    public QueryFilterTransformer getTransformer(QName qName) {
        return queryFilterTransformerMap.get(qName);
    }

    private QName getNamespace(ServiceReference<QueryFilterTransformer> reference) {
        String namespace = (String) reference.getProperty("id");
        return QName.valueOf(namespace);
    }

    private QueryFilterTransformer getTransformer(
            ServiceReference<QueryFilterTransformer> reference) {
        BundleContext bundleContext = getBundleContext();

        QueryFilterTransformer transformer = bundleContext.getService(reference);

        if (transformer == null) {
            throw new IllegalStateException(
                    "Attempted to retrieve an unregistered service: " + reference);
        }

        return bundleContext.getService(reference);
    }

    private BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
    }
}
