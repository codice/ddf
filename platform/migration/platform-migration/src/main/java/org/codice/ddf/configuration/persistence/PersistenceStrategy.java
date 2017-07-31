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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;

/**
 * Interface implemented by classes that can read and write configuration properties.
 */
public interface PersistenceStrategy {
    /**
     * Gets the extension handled by this strategy.
     *
     * @return the extension (without the prefix <code>.</code>) handled by this strategy
     */
    public String getExtension();

    /**
     * Reads the configuration properties from an {@link InputStream}.
     *
     * @param inputStream output stream where the properties will be written
     * @return the properties read
     * @throws IOException thrown if the properties couldn't be read
     */
    public Dictionary<String, Object> read(InputStream inputStream) throws IOException;

    /**
     * Writes the configuration properties to an {@link OutputStream}.
     *
     * @param outputStream output stream where the properties will be written
     * @param properties   properties to write
     * @throws IOException thrown if the properties couldn't be written
     */
    public void write(OutputStream outputStream, Dictionary<String, Object> properties)
            throws IOException;
}
