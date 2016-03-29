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

import javax.validation.constraints.NotNull;

import org.codice.ddf.platform.services.common.Describable;

public abstract class AbstractDescribable implements Describable {

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
     */
    public AbstractDescribable(@NotNull String version, @NotNull String id, @NotNull String title, @NotNull String description,
            @NotNull String organization) {

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

    @Override
    @NotNull public String getVersion() {
        return version;
    }

    @Override
    @NotNull public String getId() {
        return id;
    }

    @Override
    @NotNull public String getTitle() {
        return title;
    }

    @Override
    @NotNull public String getDescription() {
        return description;
    }

    @Override
    @NotNull public String getOrganization() {
        return organization;
    }

}

