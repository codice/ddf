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
package ddf.security.permission;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Permission class comprised of a key and a list of corresponding values. Contains the logic to
 * determine if this key/value permission can imply another key/value permission.
 */
public class KeyValuePermission implements Permission {
    private String key;

    private List<String> values;

    /**
     * Creates a new KeyValuePermission with the specified key and an empty list of values.
     * 
     * @param key
     *            the key to be used for this permission
     */
    public KeyValuePermission(String key) {
        this(key, new ArrayList<String>());
    }

    /**
     * Creates a new KeyValuePermission with the specified key and corresponding list of values.
     * 
     * @param key
     *            the key to be used for this permission
     * @param values
     *            the list of values to be used for this permission
     * @throws IllegalArgumentException
     *             if the key is null - a valid key is required
     */
    public KeyValuePermission(String key, List<String> values) {
        if (key == null) {
            throw new IllegalArgumentException(
                    "Incoming key cannot be null, could not create permission.");
        }
        this.key = key;
        if (values == null) {
            this.values = new ArrayList<String>();
        } else {
            this.values = values;
        }
    }

    public String getKey() {
        return key;
    }

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Adds an additional value to the existing values.
     * 
     * @param value
     *            new value to be added to the existing values for this key/value pair
     */
    public void addValue(String value) {
        values.add(value);
    }

    /**
     * Returns {@code true} if this current instance <em>implies</em> all the functionality and/or
     * resource access described by the specified {@code Permission} argurment, {@code false}
     * otherwise.
     * <p/>
     * That is, this current instance must be exactly equal to or a <em>superset</em> of the
     * functionality and/or resource access described by the given {@code Permission} argument. Yet
     * another way of saying this would be:
     * <p/>
     * If &quot;permission1 implies permission2&quot;, i.e.
     * <code>permission1.implies(permission2)</code> , then any Subject granted {@code permission1}
     * would have ability greater than or equal to that defined by {@code permission2}.
     * <p/>
     * For KeyValuePermission objects this is determined as follows:
     * <p/>
     * If the keys of each permission are equal and if the values from this object implies the
     * values from the passed in permission, then this permission will imply the passed in
     * permission.
     * 
     * @param p
     *            permission to checked to see if this permission implies p
     * @return {@code true} if this current instance <em>implies</em> all the functionality and/or
     *         resource access described by the specified {@code Permission} argument, {@code false}
     *         otherwise.
     */
    @Override
    public boolean implies(Permission p) {
        if (p instanceof KeyValuePermission) {
            if (getKey().equals(((KeyValuePermission) p).getKey())) {
                WildcardPermission thisWildCard = buildWildcardFromKeyValue(this);
                WildcardPermission implied = buildWildcardFromKeyValue((KeyValuePermission) p);
                return thisWildCard.implies(implied);
            }
        } else if (p instanceof KeyValueCollectionPermission) {
            WildcardPermission thisWildCard = buildWildcardFromKeyValue(this);
            List<KeyValuePermission> permissionList = ((KeyValueCollectionPermission) p)
                    .getKeyValuePermissionList();
            for (KeyValuePermission keyValuePermission : permissionList) {
                if (getKey().equals(keyValuePermission.getKey())) {
                    WildcardPermission implied = buildWildcardFromKeyValue(keyValuePermission);
                    return thisWildCard.implies(implied);
                }
            }
        } else if (p instanceof WildcardPermission) {
            WildcardPermission thisWildCard = buildWildcardFromKeyValue(this);
            return thisWildCard.implies(p);
        }
        return false;
    }

    /**
     * Returns a {@link org.apache.shiro.authz.permission.WildcardPermission} representing a
     * {@link KeyValuePermission}
     * 
     * @param perm
     *            the permission to convert.
     * @return new equivalent permission
     */
    private WildcardPermission buildWildcardFromKeyValue(KeyValuePermission perm) {
        StringBuilder wildcardString = new StringBuilder();
        for (String value : perm.getValues()) {
            wildcardString.append(value);
            wildcardString.append(",");
        }
        WildcardPermission wildcardPermission = new WildcardPermission(wildcardString.toString()
                .substring(0, wildcardString.length() - 1));
        return wildcardPermission;
    }

    /**
     * Creates a string representation of this key/value permission object.
     * 
     * @return a string representation of this key/value permission object
     */
    @Override
    public String toString() {
        return key + " : " + StringUtils.join(values, ",");
    }

}
