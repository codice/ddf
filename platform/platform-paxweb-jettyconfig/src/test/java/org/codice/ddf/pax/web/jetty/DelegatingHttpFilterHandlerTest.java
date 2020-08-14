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
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.HttpFilter;
import org.codice.ddf.platform.filter.HttpFilterChain;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.junit.Test;
import org.osgi.util.tracker.ServiceTracker;

public class DelegatingHttpFilterHandlerTest {

  @Test
  public void testDelegatingHttpFilterHandler() throws Exception {
    Request mockBaseRequest = mock(Request.class);
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);

    HttpFilter mockFilter = mock(HttpFilter.class);
    doAnswer(
            invocationOnMock -> {
              ((HttpFilterChain) invocationOnMock.getArgument(2))
                  .doFilter(mockRequest, mockResponse);
              return null;
            })
        .when(mockFilter)
        .doFilter(any(), any(), any());

    ServiceTracker<HttpFilter, HttpFilter> serviceTracker = mock(ServiceTracker.class);
    when(serviceTracker.getServices(any())).thenReturn(new HttpFilter[] {mockFilter});
    when(serviceTracker.size()).thenReturn(1);

    Handler handler = mock(Handler.class);

    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(serviceTracker);
    underTest.setHandler(handler);

    underTest.handle("/", mockBaseRequest, mockRequest, mockResponse);

    verify(mockFilter).doFilter(any(), any(), any());
    verify(handler).handle("/", mockBaseRequest, mockRequest, mockResponse);
  }
}
