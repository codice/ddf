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
package ddf.catalog.impl.operations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Support class for dealing with security for the {@code CatalogFrameworkImpl}.
 *
 * Specifically, this class exists to isolate the logic required to build a security policy map.
 */
public class OperationsSecuritySupport {
    void buildPolicyMap(HashMap<String, Set<String>> policyMap,
            Set<Map.Entry<String, Set<String>>> policy) {
        if (policy != null) {
            for (Map.Entry<String, Set<String>> entry : policy) {
                if (policyMap.containsKey(entry.getKey())) {
                    policyMap.get(entry.getKey())
                            .addAll(entry.getValue());
                } else {
                    policyMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
            }
        }
    }
}
