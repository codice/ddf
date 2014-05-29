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
package org.codice.ddf.security.policy.context;

import ddf.security.permission.CollectionPermission;
import org.codice.ddf.security.policy.context.attributes.ContextAttributeMapping;

import java.util.Collection;

/**
 * Represents the policy for a given web context.
 */
public interface ContextPolicy {

    /**
     * Returns the context path that this policy covers.
     * @return context path
     */
    public String getContextPath();

    /**
     * Returns a {@link java.util.Collection} of authentication methods
     *
     * @return authentication methods
     */
    public Collection<String> getAuthenticationMethods();

    /**
     * Returns a {@link java.util.Collection} of {@link ddf.security.permission.CollectionPermission}
     * objects built from the attribute mappings.
     *
     * @return permissions
     */
    public Collection<CollectionPermission> getAllowedAttributePermissions();

    /**
     * Returns a {@link java.util.Collection} of attribute names.
     *
     * @return all attribute names used in this policy
     */
    public Collection<String> getAllowedAttributeNames();

    /**
     * Sets the attribute mappings.
     *
     * @param attributes
     */
    public void setAllowedAttributes(Collection<ContextAttributeMapping> attributes);
}
