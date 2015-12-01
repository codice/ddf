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
 * Principal that designates a {@link ddf.security.Subject} as guest
 */
public class GuestPrincipal implements Principal {

    public static final String GUEST_NAME_PREFIX = "Guest";

    public static final String NAME_DELIMITER = "@";

    private String name;

    public GuestPrincipal(String address) {
        this.name = GUEST_NAME_PREFIX + NAME_DELIMITER + address;
    }

    /**
     * Returns the ip address associated with this guest principal
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
     * Parses the ip address out of a guest principal name that has the format
     * Guest@127.0.0.1
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
