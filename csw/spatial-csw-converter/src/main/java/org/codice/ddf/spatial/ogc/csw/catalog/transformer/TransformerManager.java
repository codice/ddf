/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a list of available Transformers and their properties.
 */
public class TransformerManager<T> {

    private final List<ServiceReference<T>> serviceRefs;

    private final BundleContext context;

    private static final String MIME_TYPE = "mime-type";

    private static final String SCHEMA = "schema";

    public TransformerManager(BundleContext context,
            List<ServiceReference<T>> serviceReferences) {
        this.serviceRefs = serviceReferences;
        this.context = context;
    }

    public List<String> getAvailableMimeTypes() {
        return getAvailableProperty(MIME_TYPE);
    }

    public List<String> getAvailableSchemas() {
        return getAvailableProperty(SCHEMA);
    }

    private List<String> getAvailableProperty(String propertyName) {
        List<String> properties = new ArrayList<String>();
        for (ServiceReference<T> serviceRef : serviceRefs) {
            Object mimeObject = serviceRef.getProperty(propertyName);
            if (mimeObject != null && mimeObject instanceof String) {
                properties.add((String) mimeObject);
            }
        }
        return properties;
    }

    public T getTransformerBySchema(String schema) {
        return getTransformerByProperty(SCHEMA, schema);
    }

    public T getTransformerByMimeType(String mimeType) {
        return getTransformerByProperty(MIME_TYPE, mimeType);
    }

    public T getCswQueryResponseTransformer() {
        for (ServiceReference<T> serviceRef : serviceRefs) {
            Object propertyObject = serviceRef.getProperty("id");
            if (propertyObject != null && propertyObject instanceof String) {
                if ("csw".equals((String) propertyObject)) {
                    return context.getService(serviceRef);
                }
            }
        }
        return null;
    }

    private T getTransformerByProperty(String property, String value) {
        if (value == null) {
            return null;
        }
        for (ServiceReference<T> serviceRef : serviceRefs) {
            Object propertyObject = serviceRef.getProperty(property);
            if (propertyObject != null && propertyObject instanceof String) {
                if (value.equals((String) propertyObject)) {
                    return context.getService(serviceRef);
                }
            }
        }
        return null;
    }
}
