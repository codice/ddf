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
package org.codice.ddf.platform.io;

import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;

/**
 * Class that persists configuration properties using the Felix .cfg file format.
 * <p>
 * All values are stored as strings. The returned {@link Dictionary} from {@link #read(InputStream)}
 * will always consist of string values that must be separately parsed or processed by the caller,
 * even if they weren't strings when handed to {@link #write(OutputStream, Dictionary)}.
 * <p>
 * Not recommended for use with complex objects such as arrays or maps. Where possible, use .config
 * and {@link ConfigStrategy} instead.
 */
public class CfgStrategy implements PersistenceStrategy {
    @Override
    public String getExtension() {
        return "cfg";
    }

    @Override
    public void write(OutputStream out, Dictionary<String, Object> inputDictionary)
            throws IOException {
        notNull(out, "Output stream cannot be null");
        notNull(inputDictionary, "Properties cannot be null");
        final Properties props = new Properties();
        final Enumeration<String> keys = inputDictionary.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            props.put(key,
                    inputDictionary.get(key)
                            .toString());
        }
        props.save(out);
    }

    @Override
    public Dictionary<String, Object> read(InputStream in) throws IOException {
        notNull(in, "Input stream cannot be null");
        final Properties props = new Properties();
        props.load(in);
        return new Hashtable<>(props);
    }
}
