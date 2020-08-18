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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.filter.http.HttpFilterChain;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DelegatingHttpFilterHandlerTest {

  private HttpFilter mockHttpFilter() throws Exception {
    HttpFilter mockFilter = mock(HttpFilter.class);
    doAnswer(
            invocationOnMock -> {
              HttpServletRequest request = invocationOnMock.getArgument(0);
              HttpServletResponse response = invocationOnMock.getArgument(1);
              HttpFilterChain filterChain = invocationOnMock.getArgument(2);
              filterChain.doFilter(request, response);
              return null;
            })
        .when(mockFilter)
        .doFilter(any(), any(), any());
    return mockFilter;
  }

  /**
   * Rank services in the order they are passed in: First reference = highest rank Last reference =
   * lowest rank
   *
   * @param references
   */
  private void rankServiceReferences(ServiceReference<?>... references) {
    for (int i = 0; i < references.length; i++) {
      for (int j = i + 1; j < references.length; j++) {
        when(references[i].compareTo(references[j])).thenReturn(1);
        when(references[j].compareTo(references[i])).thenReturn(-1);
      }
    }
  }

  @Test
  public void testDelegatingHttpFilterHandler() throws Exception {
    Request mockBaseRequest = mock(Request.class);
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);

    HttpFilter mockFilter = mockHttpFilter();
    ServiceReference<HttpFilter> mockServiceReference = mock(ServiceReference.class);

    BundleContext mockBundleContext = mock(BundleContext.class);
    when(mockBundleContext.getServiceReferences(any(Class.class), anyString()))
        .thenReturn(Collections.singletonList(mockServiceReference));
    when(mockBundleContext.getService(mockServiceReference)).thenReturn(mockFilter);

    Handler handler = mock(Handler.class);

    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(mockBundleContext);
    underTest.setHandler(handler);

    underTest.handle("/", mockBaseRequest, mockRequest, mockResponse);

    verify(mockFilter).doFilter(any(), any(), any());
    verify(handler).handle("/", mockBaseRequest, mockRequest, mockResponse);
  }

  @Test
  public void testDelegatingHttpFilterHandlerWithServiceRanking() throws Exception {
    Request mockBaseRequest = mock(Request.class);
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);

    HttpFilter filter1 = mockHttpFilter();
    HttpFilter filter2 = mockHttpFilter();
    HttpFilter filter3 = mockHttpFilter();

    ServiceReference<HttpFilter> reference1 = mock(ServiceReference.class);
    ServiceReference<HttpFilter> reference2 = mock(ServiceReference.class);
    ServiceReference<HttpFilter> reference3 = mock(ServiceReference.class);
    rankServiceReferences(reference1, reference2, reference3);

    BundleContext mockBundleContext = mock(BundleContext.class);
    when(mockBundleContext.getService(reference1)).thenReturn(filter1);
    when(mockBundleContext.getService(reference2)).thenReturn(filter2);
    when(mockBundleContext.getService(reference3)).thenReturn(filter3);
    when(mockBundleContext.getServiceReferences(any(Class.class), anyString()))
        .thenReturn(Arrays.asList(reference2, reference3, reference1));

    Handler handler = mock(Handler.class);

    DelegatingHttpFilterHandler underTest = new DelegatingHttpFilterHandler(mockBundleContext);
    underTest.setHandler(handler);

    underTest.handle("/", mockBaseRequest, mockRequest, mockResponse);

    InOrder inOrder = Mockito.inOrder(filter1, filter2, filter3);
    inOrder.verify(filter1).doFilter(any(), any(), any());
    inOrder.verify(filter2).doFilter(any(), any(), any());
    inOrder.verify(filter3).doFilter(any(), any(), any());
    inOrder.verifyNoMoreInteractions();
  }
}
