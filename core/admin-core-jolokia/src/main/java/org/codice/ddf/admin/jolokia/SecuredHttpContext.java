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
package org.codice.ddf.admin.jolokia;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.WebConsoleSecurityProvider2;
import org.osgi.service.http.HttpContext;

/**
 * HttpContext that authenticates using the Felix webconsole security provider.
 * 
 * 
 */
public class SecuredHttpContext implements HttpContext {

    // used to authenticate against the Karaf JAAS realm
    private WebConsoleSecurityProvider2 securityProvider;

    public SecuredHttpContext(WebConsoleSecurityProvider2 securityProvider) {
        this.securityProvider = securityProvider;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return securityProvider.authenticate(request, response);
    }

    @Override
    public URL getResource(String name) {
        return null;
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

}