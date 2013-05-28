/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.cas.client;


import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;


public class TestPageTest
{

    @Test
    public void testDoGet() throws IOException, ServletException
    {
        StringWriter writer = new StringWriter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        TestPage page = new TestPage();
        page.doGet(request, response);
        assertTrue(writer.toString().contains("request.getRemoteUser() = null"));
    }

    @Test
    public void testErrorPage() throws IOException, ServletException
    {
        StringWriter writer = new StringWriter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteUser()).thenThrow(new IllegalArgumentException("Excepted exception."));
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        TestPage page = new TestPage();
        page.doGet(request, response);
        assertTrue(writer.toString().contains("Error Creating Test Page"));
    }
}
