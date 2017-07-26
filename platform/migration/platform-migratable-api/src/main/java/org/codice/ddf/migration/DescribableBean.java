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

package org.codice.ddf.migration;

import static org.apache.commons.lang.Validate.notNull;

import org.codice.ddf.platform.services.common.Describable;

/**
 * This class may serve as a base for any service that provides the {@link Describable} methods
 * from injectable data. It is not abstract; otherwise it could not neatly be created in the
 * blueprint files or reduce the count of constructor args on its children.
 * <p>
 * The class is referred to as a 'Bean' since it only wraps data and provides no significant
 * business logic.
 * <p>
 * {@inheritDoc}
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class DescribableBean implements Describable {

    private String version;

    private String id;

    private String title;

    private String description;

    private String organization;

    /**
     * Basic constructor with minimum required components.
     *
     * @param version      version of this describable
     * @param id           id of this describable
     * @param title        title of this describable
     * @param description  description of this describable
     * @param organization organization where this describable belongs
     * @throws IllegalArgumentException if <code>version</code>, <code>id</code>, <code>title</code>,
     *                                  <code>description</code>, or <code>organization</code> is
     *                                  <code>null</code>
     */
    public DescribableBean(String version, String id, String title, String description,
            String organization) {

        notNull(description, "description cannot be null");
        notNull(organization, "organization cannot be null");
        notNull(title, "title cannot be null");
        notNull(id, "id cannot be null");
        notNull(version, "version cannot be null");

        this.version = version;
        this.id = id;
        this.title = title;
        this.description = description;
        this.organization = organization;

    }

    /**
     * Copy constructor to support simplicity of child object constructors.
     *
     * @param describableInfo object containing required info to be copied into
     *                        this describable.
     * @throws IllegalArgumentException if <code>describableInfo</code> is <code>null</code>
     */
    public DescribableBean(DescribableBean describableInfo) {
        notNull(describableInfo, "describable info cannot be null");

        this.version = describableInfo.getVersion();
        this.id = describableInfo.getId();
        this.title = describableInfo.getTitle();
        this.description = describableInfo.getDescription();
        this.organization = describableInfo.getOrganization();
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getOrganization() {
        return organization;
    }

}

