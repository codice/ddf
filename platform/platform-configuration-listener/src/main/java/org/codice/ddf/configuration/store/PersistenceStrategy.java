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

package org.codice.ddf.configuration.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;

import javax.validation.constraints.NotNull;

/**
 * Interface implemented by classes that can read and write configuration properties.
 */
public interface PersistenceStrategy {
    /**
     * Reads the configuration properties from an {@link InputStream}.
     *
     * @param inputStream input stream to read the properties from
     * @return {@link Dictionary} of properties
     * @throws ConfigurationFileException thrown if there was a problem with the property format
     * @throws IOException                thrown if the properties couldn't be read
     */
    @NotNull
    Dictionary<String, Object> read(@NotNull InputStream inputStream)
            throws ConfigurationFileException, IOException;

    /**
     * Writes the configuration properties to an {@link OutputStream}.
     *
     * @param outputStream output stream where the properties will be written
     * @param properties   properties to write
     * @throws IOException thrown if the properties couldn't be written
     */
    void write(@NotNull OutputStream outputStream, @NotNull Dictionary<String, Object> properties)
            throws IOException;
}
