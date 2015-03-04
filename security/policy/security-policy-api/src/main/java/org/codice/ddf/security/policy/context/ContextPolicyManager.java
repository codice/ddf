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

import java.util.Collection;

/**
 * Manages and stores all web context policies.
 */
public interface ContextPolicyManager {

    /**
     * Returns the policy associated with the given context path. The argument is assumed to be
     * the type of path returned from calling httpRequest.getContextPath();
     * @param path - context path
     * @return policy associated with the given path
     */
    public ContextPolicy getContextPolicy(String path);

    /**
     * Returns a Collection of all {@link ContextPolicy} objects
     * @return collection of policies
     */
    public Collection<ContextPolicy> getAllContextPolicies();

    /**
     * Sets a policy for a particular path
     * @param path - context path
     * @param contextPolicy - context policy
     */
    public void setContextPolicy(String path, ContextPolicy contextPolicy);

    /**
     * Returns true if the policy is white listed.
     * @param path - - context path
     * @return true if the policy is white listed
     */
    public boolean isWhiteListed(String path);
}
