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
package org.codice.ddf.security.policy.context.impl;

import ddf.security.permission.CollectionPermission;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of ContextPolicy for the Policy Manager in this package.
 */
public class Policy implements ContextPolicy {

    private String contextPath;

    private Collection<String> authenticationMethods;

    private Collection<ContextAttributeMapping> attributeMappings;

    public Policy(String contextPath, Collection<String> authenticationMethods, Collection<ContextAttributeMapping> attributeMappings) {
        this.contextPath = contextPath;
        this.authenticationMethods = authenticationMethods;
        this.attributeMappings = attributeMappings;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public Collection<String> getAuthenticationMethods() {
        return authenticationMethods;
    }

    @Override
    public Collection<CollectionPermission> getAllowedAttributePermissions() {
        List<CollectionPermission> permissions = new ArrayList<CollectionPermission>();
        for(ContextAttributeMapping mapping : attributeMappings) {
            permissions.add(mapping.getAttributePermission());
        }
        return permissions;
    }

    @Override
    public Collection<String> getAllowedAttributeNames() {
        List<String> names = new ArrayList<String>();
        if(attributeMappings != null && attributeMappings.size() > 0) {
            for(ContextAttributeMapping mapping : attributeMappings) {
                names.add(mapping.getAttributeName());
            }
        }
        return names;
    }

    @Override
    public void setAllowedAttributes(Collection<ContextAttributeMapping> attributes) {
        this.attributeMappings = attributes;
    }
}
