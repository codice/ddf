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
package org.codice.security.policy.context.impl;

import ddf.security.permission.CollectionPermission;
import org.codice.security.policy.context.ContextPolicy;
import org.codice.security.policy.context.attributes.ContextAttributeMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ContextPolicy for the Policy Manager in this package.
 */
public class Policy implements ContextPolicy {

    private String contextPath;

    private List<String> authenticationMethods;

    private List<ContextAttributeMapping> attributeMappings;

    public Policy(String contextPath, List<String> authenticationMethods, List<ContextAttributeMapping> attributeMappings) {
        this.contextPath = contextPath;
        this.authenticationMethods = authenticationMethods;
        this.attributeMappings = attributeMappings;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public List<String> getAuthenticationMethods() {
        return authenticationMethods;
    }

    @Override
    public List<CollectionPermission> getAllowedAttributePermissions() {
        List<CollectionPermission> permissions = new ArrayList<CollectionPermission>();
        for(ContextAttributeMapping mapping : attributeMappings) {
            permissions.add(mapping.getAttributePermission());
        }
        return permissions;
    }

    @Override
    public void setAllowedAttributes(List<ContextAttributeMapping> attributes) {
        this.attributeMappings = attributes;
    }
}
