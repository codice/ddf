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
package ddf.common.test.matchers;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;

/**
 * Compares two {@code Dictionary} objects returned by OSGi's
 * {@link org.osgi.service.cm.Configuration} objects for equality.
 */
public class ConfigurationPropertiesComparator {
    /**
     * Compares two {link org.osgi.service.cm.Configuration} {@link Dictionary} for equality.
     *
     * @param configProperties1 first dictionary to compare
     * @param configProperties2 second dictionary to compare
     * @return {@code true} if both dictionaries contain the exact same key-value pairs
     */
    public boolean equal(Dictionary<String, Object> configProperties1,
            Dictionary<String, Object> configProperties2) {

        if (configProperties1 == null && configProperties2 == null) {
            return true;
        }

        if (configProperties1 == null || configProperties2 == null) {
            return false;
        }

        if (configProperties1.size() != configProperties2.size()) {
            return false;
        }

        for (Enumeration<String> keys = configProperties1.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            Object value1 = configProperties1.get(key);
            Object value2 = configProperties2.get(key);

            if (value1 instanceof Object[] && value2 instanceof Object[]) {
                if (!Arrays.equals((Object[]) value1, (Object[]) value2)) {
                    return false;
                }
            } else if (!value1.equals(value2)) {
                return false;
            }
        }

        return true;
    }
}
