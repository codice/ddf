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
package org.codice.proxy.http;

import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpConsumer;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpProxyCamelHttpTransportServletTest {

    private CamelContext mockContext = mock(CamelContext.class);

    @Test
    public void testResolve() throws Exception {
        HttpProxyCamelHttpTransportServlet servlet = new HttpProxyCamelHttpTransportServlet(mockContext);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getPathInfo()).thenReturn("/example0/something/something");
        HttpConsumer consumer = servlet.resolve(request);
        try {
            when(request.getPathInfo()).thenReturn("/example0");
            consumer = servlet.resolve(request);
        } catch (Exception e) {
            fail("Failed to resolve request. " + e.getMessage());
        }
    }

    @Test
    public void testService() throws ServletException, IOException {
        HttpProxyCamelHttpTransportServlet servlet = new HttpProxyCamelHttpTransportServlet(mockContext);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getPathInfo()).thenReturn("/example0/something/something");
        servlet.service(request, response);
        try {
            when(request.getPathInfo()).thenReturn("/example0");
            servlet.service(request, response);
        } catch (Exception e) {
            fail("Failed to resolve request. " + e.getMessage());
        }
    }
}