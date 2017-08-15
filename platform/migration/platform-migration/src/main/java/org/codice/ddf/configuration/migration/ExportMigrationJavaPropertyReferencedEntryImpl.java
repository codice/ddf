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

import java.nio.file.Path;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.ExportPathMigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property defined in a Java properties file which value
 * references another migration entry.
 */
public class ExportMigrationJavaPropertyReferencedEntryImpl
        extends ExportMigrationPropertyReferencedEntryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ExportMigrationJavaPropertyReferencedEntryImpl.class);

    private static final String METADATA_NAME = "name";

    /**
     * Holds the path for the properties file where the reference is defined.
     */
    private final Path propertiesPath;

    /**
     * Instantiated a new java property referenced migration entry given a migratable context, properties
     * path, name, and pathname.
     *
     * @param context        the migration context associated with this entry
     * @param propertiesPath the path to the Java property file
     * @param property       the property name for this entry
     * @param pathname       the pathname for this entry
     * @throws IllegalArgumentException if <code>context</code>, <code>propertiesPath</code>,
     *                                  <code>property</code>, or <code>pathname</code> is <code>null</code>
     */
    ExportMigrationJavaPropertyReferencedEntryImpl(ExportMigrationContextImpl context,
            Path propertiesPath, String property, String pathname) {
        super(context, property, pathname);
        Validate.notNull(propertiesPath, "invalid null properties path");
        this.propertiesPath = propertiesPath;
    }

    public Path getPropertiesPath() {
        return propertiesPath;
    }

    @Override
    protected void recordEntry() {
        getReport().recordJavaProperty(this);
    }

    @Override
    protected String toDebugString() {
        return String.format("Java property reference [%s] from [%s] as file [%s] to [%s]",
                getProperty(),
                propertiesPath,
                getAbsolutePath(),
                getPath());
    }

    @Override
    protected void recordWarning(String reason) {
        getReport().record(new ExportPathMigrationWarning(propertiesPath,
                getProperty(),
                getPath(),
                reason));
    }

    @Override
    protected ExportPathMigrationException newError(String reason, Throwable cause) {
        return new ExportPathMigrationException(propertiesPath,
                getProperty(),
                getPath(),
                reason,
                cause);
    }
}
