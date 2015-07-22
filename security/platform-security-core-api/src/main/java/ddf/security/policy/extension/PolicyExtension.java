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
package ddf.security.policy.extension;

import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;

/**
 * Extends the policy of the Java PDP realm. These objects can provide additional policy information that cannot be captured through the standard
 * match-all or match-one scenarios.
 */
public interface PolicyExtension {

    /**
     * Returns true if the subject collection can imply the two sets of KeyValueCollectionPermission objects.
     * This method should return any permissions that it was unable to imply. That should include any permissions
     * that the method does not understand. For example: if 10 match all permissions are passed into the method and
     * 2 of those permissions can be implied, then the method should return the remaining 8 match all permissions.
     *
     * Warning: not returning any permissions from this method will immediately grant access to every request and
     * bypass the rest of the PDP.
     *
     * @param subjectAllCollection
     * @param matchAllCollection
     * @return KeyValueCollectionPermission - set of permissions that can not be implied by this extension
     */
    KeyValueCollectionPermission isPermittedMatchAll(CollectionPermission subjectAllCollection,
            KeyValueCollectionPermission matchAllCollection);

    /**
     * Returns true if the subject collection can imply the two sets of KeyValueCollectionPermission objects.
     * This method should return any permissions that it was unable to imply. That should include any permissions
     * that the method does not understand. For example: if 10 match one permissions are passed into the method and
     * 2 of those permissions can be implied, then the method should return the remaining 8 match one permissions.
     *
     * Warning: not returning any permissions from this method will immediately grant access to every request and
     * bypass the rest of the PDP.
     *
     * @param subjectAllCollection
     * @param matchOneCollection
     * @return KeyValueCollectionPermission - set of permissions that can not be implied by this extension
     */
    KeyValueCollectionPermission isPermittedMatchOne(CollectionPermission subjectAllCollection,
            KeyValueCollectionPermission matchOneCollection);

}
