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
package org.codice.ddf.security.handler.oidc;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OidcCallbackEndpointTest {
  private static final int HTTP_REDIRECT = Status.TEMPORARY_REDIRECT.getStatusCode();

  private static OidcCallbackEndpoint callbackEndpoint;

  @Mock private HttpServletRequest mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private HttpSession mockSession;

  private Response response;

  @BeforeClass
  public static void setupClass() {
    callbackEndpoint = new OidcCallbackEndpoint();
  }

  @Test(expected = IllegalArgumentException.class)
  public void logoutWithNullRequest() {
    response = callbackEndpoint.logout(null, mockResponse);
  }

  @Test(expected = IllegalArgumentException.class)
  public void logoutWithNullResponse() {
    response = callbackEndpoint.logout(mockRequest, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void logoutWitNullSession() {
    when(mockRequest.getSession(any(Boolean.class))).thenReturn(null);
    response = callbackEndpoint.logout(mockRequest, mockResponse);
  }

  @Test
  public void logout() throws Exception {
    when(mockRequest.getSession()).thenReturn(mockSession);
    when(mockRequest.getSession(any(Boolean.class))).thenReturn(mockSession);
    response = callbackEndpoint.logout(mockRequest, mockResponse);

    verify(mockSession, times(1)).invalidate();
    assertThat(response.getStatus(), is(HTTP_REDIRECT));
    assertThat(
        response.getHeaderString("Location"), is(SystemBaseUrl.EXTERNAL.constructUrl("/logout")));
  }
}
