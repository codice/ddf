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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DelegateServletFilterTest {

  private DelegateServletFilter delegateServletFilter;

  @Before
  public void setup() {
    delegateServletFilter = new DelegateServletFilter(new ArrayList<>());
  }

  @Test
  public void testInit() {
    delegateServletFilter.init(mock(FilterConfig.class));
  }

  @Test
  public void testDestroy() {
    delegateServletFilter.destroy();
  }

  @Test
  public void testDoFilterWithFilter() throws IOException, ServletException {
    List<Filter> filterList = new ArrayList<>();
    Filter filter = makeFilter();
    filterList.add(filter);
    delegateServletFilter = new DelegateServletFilter(filterList);

    final ServletRequest servletRequest = mock(ServletRequest.class);
    final ServletResponse servletResponse = mock(ServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    // when
    delegateServletFilter.doFilter(servletRequest, servletResponse, filterChain);

    // then
    final InOrder inOrder = Mockito.inOrder(filter, filterChain);

    inOrder
        .verify(filter)
        .doFilter(eq(servletRequest), eq(servletResponse), any(FilterChain.class));
    inOrder.verify(filterChain).doFilter(servletRequest, servletResponse);
  }

  @Test
  public void testDoFilterWithNoFilters() throws IOException, ServletException {

    final ServletRequest servletRequest = mock(ServletRequest.class);
    final ServletResponse servletResponse = mock(ServletResponse.class);
    final FilterChain filterChain = mock(FilterChain.class);

    // when
    delegateServletFilter.doFilter(servletRequest, servletResponse, filterChain);

    // then
    verify(filterChain).doFilter(servletRequest, servletResponse);
  }

  private Filter makeFilter() throws IOException, ServletException {
    final Filter filter = mock(Filter.class);
    Mockito.doAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              ((FilterChain) args[2])
                  .doFilter(((ServletRequest) args[0]), ((ServletResponse) args[1]));
              return null;
            })
        .when(filter)
        .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

    return filter;
  }
}
