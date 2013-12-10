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
package ddf.security;

public final class SecurityConstants {
    /**
     * String used to retrieve the security logger in each class that wishes to
     * log to the security log. Guarantees that all classes use the same
     * security logger.
     */
    public static final String SECURITY_LOGGER = "securityLogger";

    /**
     * Property name for the security subject. The subject is added to incoming
     * requests under this property name. The security framework retrieves the
     * subject using this property name in order to perform any security
     * operations involving the subject of the request.
     */
    public static final String SECURITY_SUBJECT = "ddf.security.subject";

    /**
     * Property key to obtain the saml assertion from a query request /
     * response.
     */
    public static final String SAML_ASSERTION = "saml.assertion";

    private SecurityConstants() {

    }
}
