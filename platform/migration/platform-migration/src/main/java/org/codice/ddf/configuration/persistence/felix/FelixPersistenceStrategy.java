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
package org.codice.ddf.configuration.persistence.felix;

import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Dictionary;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;

/**
 * Class that persists configuration properties using the Felix' file format.
 */
public class FelixPersistenceStrategy implements PersistenceStrategy {

    @Override
    public void write(OutputStream outputStream, Dictionary<String, Object> properties)
            throws IOException {
        notNull(outputStream, "OutputStream cannot be null");
        notNull(properties, "Properties cannot be null");

        ConfigurationHandler.write(outputStream, properties);
    }

}
