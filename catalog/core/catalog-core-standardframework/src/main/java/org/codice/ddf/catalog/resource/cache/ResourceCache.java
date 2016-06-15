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
package org.codice.ddf.catalog.resource.cache;

import java.util.Optional;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.resource.Resource;

/**
 * Interface that defines the operations related to the resource cache.
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in the future.</b>
 */
public interface ResourceCache {

    /**
     * Gets the default resource associated with the {@link Metacard} provided.
     *
     * @param metacard metacard that corresponds to the resource that was requested
     * @return resource associated with the {@link Metacard} provided, if any
     */
    Optional<Resource> get(Metacard metacard);

    /**
     * Gets a specific resource associated with the {@link Metacard} provided based on the
     * attributes of the {@link ResourceRequest}.
     *
     * @param metacard        metacard that corresponds to the resource that was requested
     * @param resourceRequest request object that was used to retrieve the resource. Will be used to
     *                        determine the type of resource that will be returned.
     * @return resource associated with the {@link Metacard} provided, if any
     */
    Optional<Resource> get(Metacard metacard, ResourceRequest resourceRequest);

    /**
     * Determines if the resource associated with the {@link Metacard} is present in the cache
     *
     * @param metacard metacard to use to find a matching resource
     * @return {@code true} only if the default resource associated currently exists in the cache
     */
    boolean contains(Metacard metacard);

    /**
     * Determines if the resource associated with the {@link Metacard} is present in the cache
     *
     * @param metacard        metacard to use to find a matching resource
     * @param resourceRequest request object that was used to retrieve the resource. Will be used to
     *                        determine the type of resource to look up.
     * @return {@code true} only if the resource of the given type associated currently exists in
     * the cache
     */
    boolean contains(Metacard metacard, ResourceRequest resourceRequest);
}
