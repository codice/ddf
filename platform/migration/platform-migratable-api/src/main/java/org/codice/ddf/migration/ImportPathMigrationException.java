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

import java.nio.file.Path;

/**
 * Provides error information related to an imported path that was referenced from a property that
 * no longer reference it on the local system.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class ImportPathMigrationException extends ImportMigrationException {
    private static final String ABSOLUTE_PATH_WARNING = "Path [%s] %s";

    private static final String SYSTEM_PROPERTY_ABSOLUTE_PATH_WARNING =
            "The value for system property [%s] is no longer set to path [%s]; %s";

    private static final String JAVA_FILE_PROPERTY_ABSOLUTE_PATH_WARNING =
            "The value for property [%s] in file [%s] is no longer set to path [%s]; %s";

    /**
     * Constructor used to report a migration error on a specific path.
     *
     * @param path path that cannot be exported
     */
    public ImportPathMigrationException(Path path, String reason) {
        super(String.format(ABSOLUTE_PATH_WARNING, path.toString(), reason));
    }

    /**
     * Constructor used to report a migration error on a specific path.
     *
     * @param path  path that cannot be exported
     * @param cause the cause for this error
     */
    public ImportPathMigrationException(Path path, String reason, Throwable cause) {
        super(String.format(ABSOLUTE_PATH_WARNING, path.toString(), reason), cause);
    }

    /**
     * Constructor used to report a migration error on a referenced path reference coming from a
     * system property.
     *
     * @param name   name of the system property that referenced the path
     * @param path   path that is not properly referenced
     * @param reason reason for the error
     */
    public ImportPathMigrationException(String name, Path path, String reason) {
        super(String.format(ImportPathMigrationException.SYSTEM_PROPERTY_ABSOLUTE_PATH_WARNING,
                name,
                path,
                reason));
    }

    /**
     * Constructor used to report a migration error on a referenced path reference coming from a
     * system property.
     *
     * @param name   name of the system property that referenced the path
     * @param path   path that is not properly referenced
     * @param reason reason for the error
     * @param cause  the cause for this error
     */
    public ImportPathMigrationException(String name, Path path, String reason, Throwable cause) {
        super(String.format(ImportPathMigrationException.SYSTEM_PROPERTY_ABSOLUTE_PATH_WARNING,
                name,
                path,
                reason), cause);
    }

    /**
     * Constructor used to report a migration error on a referenced path coming from a Java properties
     * file.
     *
     * @param file   path to the Java properties file that contains the property
     * @param name   name of the property that referenced the path
     * @param path   path that is not properly referenced
     * @param reason reason for the error
     */
    public ImportPathMigrationException(Path file, String name, Path path, String reason) {
        super(String.format(ImportPathMigrationException.JAVA_FILE_PROPERTY_ABSOLUTE_PATH_WARNING,
                file,
                name,
                path,
                reason));
    }

    /**
     * Constructor used to report a migration error on a referenced path coming from a Java properties
     * file.
     *
     * @param file   path to the Java properties file that contains the property
     * @param name   name of the property that referenced the path
     * @param path   path that is not properly referenced
     * @param reason reason for the error
     * @param cause  the cause for this error
     */
    public ImportPathMigrationException(Path file, String name, Path path, String reason,
            Throwable cause) {
        super(String.format(ImportPathMigrationException.JAVA_FILE_PROPERTY_ABSOLUTE_PATH_WARNING,
                file,
                name,
                path,
                reason), cause);
    }
}
