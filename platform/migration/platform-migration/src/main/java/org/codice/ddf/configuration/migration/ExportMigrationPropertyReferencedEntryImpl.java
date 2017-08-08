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
package org.codice.ddf.configuration.migration;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property which value references another migration entry.
 */
public abstract class ExportMigrationPropertyReferencedEntryImpl extends ExportMigrationEntryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ExportMigrationPropertyReferencedEntryImpl.class);

    private static final String METADATA_PROPERTY = "property";

    private static final String METADATA_REFERENCE = "reference";

    private final String property;

    /**
     * Instantiated a new property referenced migration entry given a migratable context, property name
     * and pathname.
     *
     * @param context  the migration context associated with this entry
     * @param property the property name for this entry
     * @param pathname the pathname for this entry
     * @throws IllegalArgumentException if <code>context</code>, <code>property</code>, or
     *                                  <code>pathname</code> is <code>null</code>
     */
    ExportMigrationPropertyReferencedEntryImpl(ExportMigrationContextImpl context, String property,
            String pathname) {
        super(context, context.resolveAgainstUserDirectory(pathname));
        Validate.notNull(property, "invalid null property");
        this.property = property;
    }

    protected String getProperty() {
        return property;
    }
}
