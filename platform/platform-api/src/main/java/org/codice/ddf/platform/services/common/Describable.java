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
package org.codice.ddf.platform.services.common;

import javax.validation.constraints.NotNull;

/**
 * Describable is used to capture a basic description of a service. This provides valuable runtime
 * information to the user regarding the application bundles and OSGi.
 *
 * This interface is not meant to be a concrete store of data and does not have a default implementation.
 * It is expected that the children are services that perform specific tasks.
 */
public interface Describable {
    /**
     * Returns the version.
     *
     * @return the version of the item being described (example: 1.0)
     */
    @NotNull String getVersion();

    /**
     * Returns the name, aka ID, of the describable item. The name should be unique for each
     * instance with the scope of a service or a component. For example, this is unique for any
     * Migratable in a set of Migratables. It is not necessarily unique between Migratables and
     * Metacards.
     *
     * @return ID of the item
     */
    @NotNull String getId();

    /**
     * Returns the title of the describable item. It is generally more verbose than the name (aka
     * ID).
     *
     * @return title of the item (example: File System Provider)
     */
    @NotNull String getTitle();

    /**
     * Returns a description of the describable item.
     *
     * @return description of the item (example: Provider that returns back static results)
     */
    @NotNull String getDescription();

    /**
     * Returns the organization associated with the describable item.
     *
     * @return organizational name or acronym (example: USAF)
     */
    @NotNull String getOrganization();
}
