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
package ddf.catalog.security.policy;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import ddf.catalog.data.Metacard;
import ddf.security.SubjectUtils;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.policy.extension.PolicyExtension;

/**
 * <p>
 * Policy extension that checks for the presence of point of contact information
 * (set by UploaderPolicyPlugin on PostQuery). If there is a point of contact
 * and it matches the user's email, this will imply all permissions. If there is
 * a point of contact and it does NOT match the user's email, this will only imply
 * the point of contact permission.
 * </p>
 */
public class UploaderPolicyExtension implements PolicyExtension {
    public static final String READ_ACTION = "read";

    public static final String UPDATE_ACTION = "update";

    @Override
    public KeyValueCollectionPermission isPermittedMatchAll(
            CollectionPermission subjectAllCollection,
            KeyValueCollectionPermission matchAllCollection) {
        //Get the "point-of-contact-all"
        Optional<KeyValuePermission> pocPermission = matchAllCollection.getKeyValuePermissionList()
                .stream()
                .map(permission -> (KeyValuePermission) permission)
                .filter(permission -> permission.getKey()
                        .equals(Metacard.POINT_OF_CONTACT + "-all") || permission.getKey()
                        .equals(Metacard.POINT_OF_CONTACT + "-xacml"))
                .findFirst();

        if (pocPermission.isPresent()) {
            return checkEmailAndImplyPermissions(subjectAllCollection,
                    matchAllCollection,
                    pocPermission);
        } else {
            return matchAllCollection;
        }
    }

    @Override
    public KeyValueCollectionPermission isPermittedMatchOne(
            CollectionPermission subjectAllCollection,
            KeyValueCollectionPermission matchOneCollection) {
        //Get the "point-of-contact-one"
        Optional<KeyValuePermission> pocPermission = matchOneCollection.getKeyValuePermissionList()
                .stream()
                .map(permission -> (KeyValuePermission) permission)
                .filter(permission -> permission.getKey()
                        .equals(Metacard.POINT_OF_CONTACT + "-one"))
                .findFirst();

        if (pocPermission.isPresent()) {
            return checkEmailAndImplyPermissions(subjectAllCollection,
                    matchOneCollection,
                    pocPermission);
        } else {
            return matchOneCollection;
        }
    }

    private KeyValueCollectionPermission checkEmailAndImplyPermissions(
            CollectionPermission subjectAllCollection, KeyValueCollectionPermission matchCollection,
            Optional<KeyValuePermission> pocPermission) {
        //Get the email from the subjectPermissions
        Optional<KeyValuePermission> emailPermission = subjectAllCollection.getPermissionList()
                .stream()
                .map(permission -> (KeyValuePermission) permission)
                .filter(permission -> permission.getKey()
                        .equals(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI))
                .findFirst();

        //If this is a read or update action and subject's email matches the "point-of-contact"
        if (isReadOrUpdate(matchCollection.getAction()) && emailPermission.isPresent()
                && permissionsHaveCommonValue(pocPermission.get(), emailPermission.get())) {

            //Imply all permissions
            return new KeyValueCollectionPermission(matchCollection.getAction());
        } else {
            //Only imply the "point-of-contact" permissions
            return new KeyValueCollectionPermission(matchCollection.getAction(),
                    matchCollection.getKeyValuePermissionList()
                            .stream()
                            .map(permission -> (KeyValuePermission) permission)
                            .filter(permission -> !permission.getKey()
                                    .contains(Metacard.POINT_OF_CONTACT))
                            .collect(Collectors.toList()));
        }
    }

    private boolean isReadOrUpdate(String actionString) {
        return actionString != null && (actionString.equals(READ_ACTION) || actionString.equals(
                UPDATE_ACTION));
    }

    private boolean permissionsHaveCommonValue(KeyValuePermission permission1,
            KeyValuePermission permission2) {
        return !Collections.disjoint(permission1.getValues(), permission2.getValues());
    }
}
