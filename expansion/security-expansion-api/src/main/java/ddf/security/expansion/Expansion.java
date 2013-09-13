/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.expansion;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Expansion {
    /**
     * Expands the values for an entire map of security entries.
     * 
     * @param map
     *            The map corresponding to the set of security attributes and their values to be
     *            expanded
     * @return The updated map with expanded set of values for each key
     */
    Map<String, Set<String>> expand(Map<String, Set<String>> map);

    /**
     * Expands the values for a given key and its values.
     * 
     * @param key
     *            The key corresponding to the set of security values to be expanded
     * @return The updated set of expanded values for the given key
     */
    Set<String> expand(String key, Set<String> values);

    /**
     * Returns the list of configured expansions.
     * 
     * @return map of the security attributes and their expansion settings
     */
    Map<String, List<String[]>> getExpansionMap();

}
