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
package org.codice.ddf.migration;

import static org.apache.commons.lang.Validate.notNull;

import javax.validation.constraints.NotNull;

/**
 * This class handles the boilerplate code that all {@link org.codice.ddf.migration.Migratable}'s
 * need to implement.
 */
public abstract class AbstractMigratable implements Migratable {

    protected final String description;
    
    protected final boolean isOptional;
    
    /**
     * Constructor
     * 
     * @param description a short description of what this {@link org.codice.ddf.migration.Migratable} does.
     * @param isOptional is the exported data from this {@link org.codice.ddf.migration.Migratable} optional or required
     */
    public AbstractMigratable(@NotNull String description, boolean isOptional) {
        notNull(description, "description cannot be null");
        this.description = description;
        this.isOptional = isOptional;
    }
    
    /**
     * @return a short description describing the purpose of this {@link org.codice.ddf.migration.Migratable}
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * @return is the exported data from this {@link org.codice.ddf.migration.Migratable} optional or required
     */
    public boolean isOptional() {
        return isOptional;
    }
}
