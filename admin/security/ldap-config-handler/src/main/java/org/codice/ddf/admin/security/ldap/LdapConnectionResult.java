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
package org.codice.ddf.admin.security.ldap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum LdapConnectionResult {
    CANNOT_CONNECT("Unable to reach the specified host."),
    CANNOT_CONFIGURE("Unable to setup test environment."),
    CANNOT_BIND("Unable to bind the user to the LDAP connection. Try a different username or password. Make sure the username is in the format of a distinguished name."),
    BASE_USER_DN_NOT_FOUND("The specified base user DN does not appear to exist."),
    BASE_GROUP_DN_NOT_FOUND("The specified base group DN does not appear to exist."),
    USER_NAME_ATTRIBUTE_NOT_FOUND("No users found with the described attribute in the base user DN"),
    NO_USERS_IN_BASE_USER_DN("The base user DN was found, but there are no users in it."),
    NO_GROUPS_IN_BASE_GROUP_DN("The base group DN was found, but there are no groups in it."),

    SUCCESSFUL_CONNECTION("A connection with the LDAP was successfully established."),
    SUCCESSFUL_BIND("Successfully binded the user to the LDAP connection."),
    FOUND_BASE_USER_DN("Found users in base user dn"),
    FOUND_BASE_GROUP_DN("Found groups in base group DN"),
    FOUND_USER_NAME_ATTRIBUTE("Users with given user attribute found in base user dn");

    private String description;

    LdapConnectionResult(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public static Map<String, String> toDescriptionMap(List<LdapConnectionResult> resultTypes) {
        return resultTypes.stream()
                .collect(Collectors.toMap(Enum::name, LdapConnectionResult::description));
    }
}
