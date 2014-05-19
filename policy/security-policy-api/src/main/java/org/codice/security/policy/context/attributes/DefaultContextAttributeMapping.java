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
package org.codice.security.policy.context.attributes;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.permission.MatchOneCollectionPermission;
import org.apache.shiro.authz.Permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Default implementation of ContextAttributeMapping
 */
public class DefaultContextAttributeMapping implements ContextAttributeMapping {

    private String attributeName;

    private String attributeValue;

    private CollectionPermission accessPermissionCollection;

    public DefaultContextAttributeMapping(String attributeName, String attributeValue) {
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public void setAttributeName(String name) {
        this.attributeName = name;
    }

    @Override
    public String getAttributeValue() {
        return attributeValue;
    }

    @Override
    public void setAttributeValue(String value) {
        this.attributeValue = value;
    }

    @Override
    public CollectionPermission getAttributePermission() {
        if(accessPermissionCollection == null) {
            accessPermissionCollection = new AccessPermissionCollection(new ArrayList<Permission>());
            accessPermissionCollection.addAll(getKeyValuePermissions());
        }
        return accessPermissionCollection;
    }

    private Collection<KeyValuePermission> getKeyValuePermissions() {
        List<KeyValuePermission> permissions = new ArrayList<KeyValuePermission>();
        if(attributeValue != null) {

            if(attributeValue.contains("|")) { //list
                String[] attrs = attributeValue.split("\\|");
                for(String attr : attrs) {
                    permissions.add(new KeyValuePermission(attributeName, Arrays.asList(attr)));
                }
            } else { //value
                List<String> attrs = new ArrayList<String>();
                attrs.add(attributeValue);
                permissions.add(new KeyValuePermission(attributeName, attrs));
            }
        }

        return permissions;
    }

    public static class AccessPermissionCollection extends MatchOneCollectionPermission {

        public AccessPermissionCollection(Collection<Permission> permissions) {
            super(permissions);
        }

        /**
         * Overrides the implies method to handle checking for the existence of one attribute - the
         * "match one" scenario rather than the "match all" behavior of the overridden classes.
         * Specifically, this permission will imply another permission if that permission matches at
         * least one of our permission attributes.
         *
         * @param p
         *            the permission to check for behavior/functionality comparison.
         * @return {@code true} if this current instance <em>implies</em> the specified
         *         {@code Permission} argument, {@code false} otherwise.
         */
        @Override
        public boolean implies(Permission p) {
            if (permissionList.isEmpty()) {
                SecurityLogger.logDebug(PERMISSION_START_MSG + toString() + PERMISSION_NOT_IMPLIES_MSG
                        + p.toString() + PERMISSION_END_MSG);
                return false;
            }

            if (p instanceof CollectionPermission) {
                for (Permission perm : ((CollectionPermission) p).getPermissionList()) {
                    for (Permission ourPerm : permissionList) {
                        // we only care about the key value permission here, because that one can have
                        // multiple values
                        // mapped to a single key. In the case of "match one" we only need one of those
                        // values to satisfy
                        // the permission.
                        if (ourPerm instanceof KeyValuePermission) {
                            for (String value : ((KeyValuePermission) ourPerm).getValues()) {
                                // Since this is "match one" we know that only one of these values needs
                                // to match in order
                                // for the entire permission at that key to be implied
                                // So here we loop through all of the values assigned to that key and
                                // create new
                                // single valued key value permissions
                                KeyValuePermission kvp = new KeyValuePermission(
                                        ((KeyValuePermission) ourPerm).getKey());
                                kvp.addValue(value);
                                if (perm.implies(kvp)) {
                                    return true;
                                }
                            }
                        }
                        // Currently we use key value permissions for everything. However, we still need
                        // to be able to handle
                        // permissions other than KV, so this else block will serve as the catch all for
                        // everything else.
                        else {
                            // Shiro permissions are always a "match all" condition so we need to flip
                            // the implies to make it match one
                            if (perm.implies(ourPerm)) {
                                return true;
                            }
                        }
                    }
                }
            }

            // default catch all permission check
            for (Permission permission : permissionList) {
                // Shiro permissions are always a "match all" condition so we need to flip the implies
                // to make it match one
                if (p.implies(permission)) {
                    SecurityLogger.logDebug(PERMISSION_START_MSG + toString() + PERMISSION_IMPLIES_MSG
                            + p.toString() + PERMISSION_END_MSG);
                    return true;
                }
            }
            SecurityLogger.logDebug(PERMISSION_START_MSG + toString() + PERMISSION_NOT_IMPLIES_MSG
                    + p.toString() + PERMISSION_END_MSG);
            return false;
        }
    }
}
