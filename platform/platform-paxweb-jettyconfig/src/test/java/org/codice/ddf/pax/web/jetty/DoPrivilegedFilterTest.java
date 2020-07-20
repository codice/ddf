/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.pax.web.jetty;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DoPrivilegedFilterTest {

  private final DoPrivilegedFilter underTest = new DoPrivilegedFilter();

  @Mock private ServletRequest request;

  @Mock private ServletResponse response;

  @Mock private FilterChain chain;

  @Test
  public void testDefaultDoFilter() throws Exception {
    underTest.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
  }

  @Test(expected = IOException.class)
  public void testIoException() throws Exception {
    doThrow(IOException.class).when(chain).doFilter(request, response);
    underTest.doFilter(request, response, chain);
  }

  @Test(expected = ServletException.class)
  public void testServletException() throws Exception {
    doThrow(ServletException.class).when(chain).doFilter(request, response);
    underTest.doFilter(request, response, chain);
  }

  @Test(expected = ServletException.class)
  public void testUnknownException() throws Exception {
    doThrow(Exception.class).when(chain).doFilter(request, response);
    underTest.doFilter(request, response, chain);
  }
}
