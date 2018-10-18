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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the proxy filter chain class. */
public class ProxyFilterChainTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFilterChainTest.class);

  /**
   * Tests that all of the filters are properly called.
   *
   * @throws ServletException
   * @throws IOException
   */
  @Test
  public void testDoFilter() throws IOException, ServletException, AuthenticationException {
    ProxyFilterChain proxyChain = new ProxyFilterChain();
    SecurityFilter filter1 = createMockSecurityFilter("filter1");
    SecurityFilter filter2 = createMockSecurityFilter("filter2");
    SecurityFilter filter3 = createMockSecurityFilter("filter3");

    ServletRequest request = mock(ServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);

    proxyChain.addSecurityFilter(filter1);
    proxyChain.addSecurityFilter(filter2);
    proxyChain.addSecurityFilter(filter3);

    proxyChain.doFilter(request, response);

    // Verify that all of the filters were called once.
    verify(filter1).doFilter(request, response, proxyChain);
    verify(filter2).doFilter(request, response, proxyChain);
    verify(filter3).doFilter(request, response, proxyChain);
  }

  /**
   * Tests that an exception is thrown if a new filter is attempted to be added after the filter has
   * been run.
   *
   * @throws IOException
   * @throws ServletException
   */
  @Test(expected = IllegalStateException.class)
  public void testAddFilterAfterDo() throws IOException, AuthenticationException {
    ProxyFilterChain proxyChain = new ProxyFilterChain();
    SecurityFilter filter1 = mock(SecurityFilter.class);
    proxyChain.doFilter(mock(ServletRequest.class), mock(ServletResponse.class));
    proxyChain.addSecurityFilter(filter1);
  }

  /**
   * Tests that an exception is thrown if more filters are attempted to be added after the filter
   * has been run.
   *
   * @throws IOException
   * @throws ServletException
   */
  @Test(expected = IllegalStateException.class)
  public void testAddFiltersAfterDo() throws IOException, AuthenticationException {
    ProxyFilterChain proxyChain = new ProxyFilterChain();
    SecurityFilter filter2 = mock(SecurityFilter.class);
    SecurityFilter filter3 = mock(SecurityFilter.class);
    proxyChain.doFilter(mock(ServletRequest.class), mock(ServletResponse.class));
    proxyChain.addSecurityFilter(filter2);
    proxyChain.addSecurityFilter(filter3);
  }

  private SecurityFilter createMockSecurityFilter(final String name)
      throws IOException, AuthenticationException {
    SecurityFilter mockFilter = mock(SecurityFilter.class);
    Mockito.when(mockFilter.toString()).thenReturn(name);
    Mockito.doAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              LOGGER.debug("{} was called.", name);
              ((FilterChain) args[2])
                  .doFilter(((ServletRequest) args[0]), ((ServletResponse) args[1]));
              return null;
            })
        .when(mockFilter)
        .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

    return mockFilter;
  }
}
