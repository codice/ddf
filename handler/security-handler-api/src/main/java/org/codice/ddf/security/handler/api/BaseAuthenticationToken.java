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
package org.codice.ddf.security.handler.api;

import org.apache.shiro.authc.AuthenticationToken;

public class BaseAuthenticationToken implements AuthenticationToken {
    public static final String DEFAULT_REALM = "DDF";

    /**
     * Represents the account identity submitted during the authentication process.
     * <p/>
     * <p>Most application authentications are username/password based and have this
     * object represent a username. However, this can also represent the DN from an
     * X509 certificate, or any other unique identifier.
     * <p/>
     * <p>Ultimately, the object is application specific and can represent
     * any account identity (user id, X.509 certificate, etc).
     */
    protected Object principal;

    /**
     * Represents the credentials submitted by the user during the authentication process that verifies
     * the submitted Principal account identity.
     * <p/>
     * <p>Most application authentications are username/password based and have this object
     * represent a submitted password.
     * <p/>
     * <p>Ultimately, the credentials Object is application specific and can represent
     * any credential mechanism.
     */
    protected Object credentials;

    /**
     * Represents the realm within which the principal and the credentials have meaning. This information
     * is encoded into the BST and is available for use on the processing side of the STS services.
     */
    protected String realm;

    public BaseAuthenticationToken(Object principal, String realm, Object credentials) {
        this.principal = principal;
        this.realm = realm;
        this.credentials = credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    public String getRealm() {
        return realm;
    }
}
