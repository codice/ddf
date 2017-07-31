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
 * Provides error information related to an exported path that was not included in the exported file.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class ExportPathMigrationException extends ExportMigrationException {
    private static final String ABSOLUTE_PATH_WARNING = "Path [%s] %s";

    private static final String SYSTEM_PROPERTY_ABSOLUTE_PATH_WARNING =
            "The value for system property [%s] is set to a path [%s] that %s";

    private static final String JAVA_FILE_PROPERTY_ABSOLUTE_PATH_WARNING =
            "The value for property [%s] in file [%s] is set to a path [%s] that %s";

    /**
     * Constructor used to report a migration error on a specific path.
     *
     * @param path  path that cannot be exported
     */
    public ExportPathMigrationException(Path path, String reason) {
        super(String.format(ABSOLUTE_PATH_WARNING, path.toString(), reason));
    }

    /**
     * Constructor used to report a migration error on a specific path.
     *
     * @param path  path that cannot be exported
     * @param cause the cause for this error
     */
    public ExportPathMigrationException(Path path, String reason, Throwable cause) {
        this(path.toString(), reason, cause);
    }

    /**
     * Constructor used to report a migration error on a specific path.
     *
     * @param path  path that cannot be exported
     * @param cause the cause for this error
     */
    public ExportPathMigrationException(String path, String reason, Throwable cause) {
        super(String.format(ABSOLUTE_PATH_WARNING, path.toString(), reason), cause);
    }

    /**
     * Constructor used to report a migration error on a path coming from a system property.
     *
     * @param systemProperty name of the system property that contains the path
     * @param path           path that cannot be exported
     * @param cause          the cause for this error
     */
    public ExportPathMigrationException(String systemProperty, Path path, String reason,
            Throwable cause) {
        super(String.format(SYSTEM_PROPERTY_ABSOLUTE_PATH_WARNING,
                systemProperty,
                path.toString(),
                reason), cause);
    }

    /**
     * Constructor used to report a migration error on a path coming from a Java properties file.
     *
     * @param propertiesFilePath path to the Java properties file that contains the property
     * @param property     name of the property that contains the path
     * @param path               path that cannot be exported
     * @param cause              the cause for this error
     */
    public ExportPathMigrationException(Path propertiesFilePath, String property, Path path,
            String reason, Throwable cause) {
        super(String.format(JAVA_FILE_PROPERTY_ABSOLUTE_PATH_WARNING,
                propertiesFilePath.toString(),
                property,
                path.toString(),
                reason), cause);
    }
}
