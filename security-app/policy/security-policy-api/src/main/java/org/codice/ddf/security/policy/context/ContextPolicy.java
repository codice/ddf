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
     * Setting that can be used in a header or attribute to signify that the
     * policy on that current message does not require authentication.
     */
    public static final String NO_AUTH_POLICY = "org.codice.ddf.security.policy.no_authentication";

    /**
     * Attribute name to identify the realm for which this policy applies. Set on the request
     * so that all classes processing the request have easy access to the realm name.
     */
    public static final String ACTIVE_REALM = "org.codice.ddf.security.policy.realm";

    /**
     * Returns the context path that this policy covers.
     *
     * @return context path
     */
    public String getContextPath();

    /**
     * Returns the realm that this policy utilizes.
     *
     * @return realm name
     */
    public String getRealm();

    /**
     * Returns a {@link java.util.Collection} of authentication methods
     *
     * @return authentication methods
     */
    public Collection<String> getAuthenticationMethods();

    /**
     * Returns a {@link java.util.Collection} of
     * {@link ddf.security.permission.CollectionPermission} objects built from
     * the attribute mappings.
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
