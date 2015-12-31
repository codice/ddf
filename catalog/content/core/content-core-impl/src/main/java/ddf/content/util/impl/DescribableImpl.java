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
package ddf.content.util.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.content.util.Describable;

/**
 * Default implementation of the Describable interface, providing basic setter/getter methods for a
 * describable item's ID, title, version, organization, and description.
 */
public abstract class DescribableImpl implements Describable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DescribableImpl.class);

    private String version = null;

    private String id = null;

    private String title = null;

    private String description = null;

    private String organization = null;

    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the describable item.
     *
     * @param version - version of this item
     */
    public void setVersion(String version) {
        LOGGER.debug("Setting version = {}", version);
        this.version = version;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the describable item.
     *
     * @param id - id of this item
     */
    public void setId(String id) {
        LOGGER.debug("ENTERING: setId - id = {}", id);
        this.id = id;
        LOGGER.debug("EXITING: setId");
    }

    @Override
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the describable item.
     *
     * @param title
     */
    public void setTitle(String title) {
        LOGGER.debug("ENTERING: setTitle");
        this.title = title;
        LOGGER.debug("EXITING: setTitle");
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the describable item.
     *
     * @param description - description of this item
     */
    public void setDescription(String description) {
        LOGGER.debug("ENTERING: setDescription");
        this.description = description;
        LOGGER.debug("EXITING: setDescription");
    }

    @Override
    public String getOrganization() {
        return organization;
    }

    /**
     * Sets the organization of the describable item.
     *
     * @param organization - organization of this item
     */
    public void setOrganization(String organization) {
        LOGGER.debug("ENTERING: setOrganization");
        LOGGER.debug("Setting organization = {}", organization);
        this.organization = organization;
        LOGGER.debug("EXITING: setOrganization");
    }

}
