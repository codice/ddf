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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Test;

public class DelegateServletFilterTest {

  @Test
  public void testDelegateFilter() throws Exception {
    ServletRequest request = mock(ServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    Filter filter = mock(Filter.class);
    FilterChain chain = mock(FilterChain.class);

    doAnswer(
            invocationOnMock -> {
              ((FilterChain) invocationOnMock.getArgument(2)).doFilter(request, response);
              return null;
            })
        .when(filter)
        .doFilter(any(), any(), any());

    Filter underTest = new DelegateServletFilter(Collections.singletonList(filter));

    underTest.doFilter(request, response, chain);

    verify(filter).doFilter(any(), any(), any());
    verify(chain).doFilter(request, response);
  }
}
