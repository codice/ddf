/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.cache.impl;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.Validate;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;

/**
 * Class used to create keys for the {@link ResourceCache} class.
 */
public class CacheKey {

    private Metacard metacard;

    private Set<String> propertyNames = Collections.emptySet();

    private ResourceRequest resourceRequest;

    /**
     * Constructor used for keys generated from a {@link Metacard} and {@link ResourceRequest}.
     *
     * @param metacard        metacard to use to generate the key
     * @param resourceRequest resource request object to use to generate the key
     */
    public CacheKey(Metacard metacard, ResourceRequest resourceRequest) {
        this(metacard);

        Validate.notNull(resourceRequest, "ResourceRequest must not be null.");

        this.resourceRequest = resourceRequest;
        this.propertyNames = resourceRequest.getPropertyNames();
    }

    /**
     * Constructor used for keys generated from a {@link Metacard} only. Will create a key that
     * maps to the metacard's default resource. To generate a key for other specific metacard
     * resources, use {@link #CacheKey(Metacard, ResourceRequest)}.
     *
     * @param metacard metacard to use to generate the key
     */
    public CacheKey(Metacard metacard) {
        Validate.notNull(metacard, "Metacard must not be null.");
        this.metacard = metacard;
    }

    /**
     * Generates a cache key based on the metacard (and optionally resource request) object
     * provided to the constructor.
     *
     * @return key key to use when doing {@link ResourceCache} lookups
     */
    public String generateKey() {

        String properties = "";

        // The OPTION_ARGUMENT, e.g., Photograph, PDF, etc., is the only resource request option 
        // that alters the InputStream to be read for resource retrieval, so only look for that 
        // option when generating the unique cache key.
        if (propertyNames != null) {
            for (String propertyName : propertyNames) {
                if (ResourceRequest.OPTION_ARGUMENT.equals(propertyName)
                        || ContentItem.QUALIFIER.equals(propertyName)) {
                    properties = "_" + propertyName + "-"
                            + resourceRequest.getPropertyValue(propertyName);
                    break;
                }
            }
        }

        return metacard.getSourceId() + "-" + metacard.getId() + properties;
    }
}
