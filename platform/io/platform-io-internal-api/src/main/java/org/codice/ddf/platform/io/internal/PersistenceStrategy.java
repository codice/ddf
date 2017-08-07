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
 **/
package org.codice.ddf.platform.io.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;

/**
 * Interface implemented by classes that can read and write configuration properties. Implementations
 * should be mappable to the file extension, or some unique key, that is the format it supports
 * reading from and writing to. While this was built for writing to disk, that is not a hard
 * requirement on new implementations.
 * <p>
 * <b>This interface should not be used to obtain an arbitrary instance, as implementations may
 * greatly vary, and no data validation is performed to alert the consumer of the service what
 * is or is not supported.</b> Filter results using the {@link #STRATEGY_EXTENSION} service property.
 * <p>
 * This interface is for internal use only.
 * <p>
 * <i>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</i>
 */
public interface PersistenceStrategy {

    /**
     * Service property key to get the file extension a particular strategy can operate upon.
     */
    String STRATEGY_EXTENSION = "platform.io.strategy.extension";

    /**
     * Gets the extension handled by this strategy.
     *
     * @return the extension (without the prefix <code>.</code>) handled by this strategy
     */
    String getExtension();

    /**
     * Reads the configuration properties from an {@link InputStream}. The stream will <b>not</b>
     * be closed automatically.
     *
     * @param inputStream input stream where the properties will be read from
     * @return the properties read
     * @throws IOException thrown if the properties couldn't be read
     */
    Dictionary<String, Object> read(InputStream inputStream) throws IOException;

    /**
     * Writes the configuration properties to an {@link OutputStream}. The stream will <b>not</b>
     * be closed automatically.
     *
     * @param outputStream output stream where the properties will be written
     * @param properties   properties to write
     * @throws IOException thrown if the properties couldn't be written
     */
    void write(OutputStream outputStream, Dictionary<String, Object> properties) throws IOException;
}