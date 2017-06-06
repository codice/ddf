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

package org.codice.ddf.configuration.persistence;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Dictionary;

import javax.validation.constraints.NotNull;

/**
 * Interface implemented by classes that can read and write configuration properties.
 */
public interface PersistenceStrategy {

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
