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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Functional interface used to facilitate exporting an entry when a more specific processing other
 * than simply copying the file is required.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
@FunctionalInterface
public interface MigrationExporter {
    /**
     * Called to export information for a given entry to the provided output stream.
     *
     * @param report the migration report where to record errors or warnings
     * @param out    the output stream where to export information
     * @throws IOException        if an I/O error occurs (the error will be recorded automatically with the
     *                            report)
     * @throws MigrationException if any other errors occurs which prevents the export from
     *                            continuing (the error will also be automatically recorded)
     */
    public void apply(MigrationReport report, OutputStream out) throws IOException;
}