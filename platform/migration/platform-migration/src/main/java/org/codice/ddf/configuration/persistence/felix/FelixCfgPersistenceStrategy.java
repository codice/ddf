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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.commons.lang.Validate;
import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;

/**
 * Class that persists configuration properties using the Felix cfg file format.
 */
public class FelixCfgPersistenceStrategy implements PersistenceStrategy {
    @Override
    public String getExtension() {
        return "cfg";
    }

    @Override
    public void write(OutputStream out, Dictionary<String, Object> properties)
            throws IOException {
        notNull(out, "OutputStream cannot be null");
        notNull(properties, "Properties cannot be null");
        final Properties props = new Properties();
        final Enumeration<String> keys = properties.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            props.put(key, (String) properties.get(key));
        }
        props.save(out);
    }

    @Override
    public Dictionary<String, Object> read(InputStream in) throws IOException {
        Validate.notNull(in, "invalid null input stream");
        final Properties props = new Properties();

        props.load(in);
        return new Hashtable<>(props);
    }
}
