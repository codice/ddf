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
package ddf.security.principal;

import java.security.Principal;

import org.apache.commons.lang.StringUtils;

/**
 * Principal that designates a {@link ddf.security.Subject} as anonymous
 */
public class AnonymousPrincipal implements Principal {

    public static final String ANONYMOUS_NAME_PREFIX = "Anonymous";

    public static final String NAME_DELIMITER = "@";

    private String name;

    public AnonymousPrincipal(String address) {
        this.name = ANONYMOUS_NAME_PREFIX + NAME_DELIMITER + address;
    }

    /**
     * Returns the ip address associated with this anonymous principal
     * @return
     */
    public String getAddress() {
        return parseAddressFromName(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Parses the ip address out of an anonymous principal name that has the format
     * Anonymous@127.0.0.1
     * @param fullName
     * @return
     */
    public static String parseAddressFromName(String fullName) {
        if (!StringUtils.isEmpty(fullName)) {
            String[] parts = fullName.split(NAME_DELIMITER);
            if (parts.length == 2) {
                return parts[1];
            }
        }
        return null;
    }
}
