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

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.WebConsoleSecurityProvider2;
import org.junit.Test;

public class SecuredHttpContextTest {

    @Test
    public void testHttpContext() throws IOException {
        WebConsoleSecurityProvider2 securityProvider = mock(WebConsoleSecurityProvider2.class);
        SecuredHttpContext httpContext = new SecuredHttpContext(securityProvider);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        httpContext.handleSecurity(request, response);

        // did the same request and response get sent to the securityprovider
        verify(securityProvider).authenticate(request, response);

        // are not used by jolokia, should always return null
        assertNull(httpContext.getMimeType(null));
        assertNull(httpContext.getResource(null));
    }

}
