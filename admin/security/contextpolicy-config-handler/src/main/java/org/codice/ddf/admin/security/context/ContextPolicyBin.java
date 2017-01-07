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

package org.codice.ddf.admin.security.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContextPolicyBin {

    private Set<String> contextPaths;

    private String realm;

    // TODO: tbatie - 12/10/16 - Should really be an ordered set
    private List<String> authenticationTypes;

    private Map<String, String> requiredAttributes;

    public ContextPolicyBin() {
        authenticationTypes = new ArrayList<>();
        requiredAttributes = new HashMap<>();
        contextPaths = new HashSet<>();
    }

    public ContextPolicyBin realm(String realm) {
        this.realm = realm;
        return this;
    }

    public List<String> authenticationTypes() {
        return authenticationTypes;
    }

    public Map<String, String> requiredAttributes() {
        return requiredAttributes;
    }

    public Set<String> contextPaths() {
        return contextPaths;
    }

    public String realm() {
        return realm;
    }

    public ContextPolicyBin authenticationTypes(List<String> authenticationTypes) {
        this.authenticationTypes = authenticationTypes;
        return this;
    }

    public ContextPolicyBin requiredAttributes(Map<String, String> requiredAttributes) {
        this.requiredAttributes = requiredAttributes;
        return this;
    }

    public ContextPolicyBin contextPaths(Set<String> contextPaths) {
        this.contextPaths = contextPaths;
        return this;
    }

    public ContextPolicyBin addContextPath(String context) {
        contextPaths.add(context);
        return this;
    }

    public boolean hasSameRequiredAttributes(Map<String, String> mappingsToCheck) {
        if (!(requiredAttributes.keySet()
                .containsAll(mappingsToCheck.keySet()) && mappingsToCheck.keySet()
                .containsAll(requiredAttributes.keySet()))) {
            return false;
        }

        return !requiredAttributes.entrySet()
                .stream()
                .filter(binMapping -> !mappingsToCheck.get(binMapping.getKey())
                        .equals(binMapping.getValue()))
                .findFirst()
                .isPresent();
    }
}
