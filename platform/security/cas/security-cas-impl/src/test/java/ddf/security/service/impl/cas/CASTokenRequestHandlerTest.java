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
package ddf.security.service.impl.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.service.SecurityServiceException;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.authc.AuthenticationToken;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.junit.Test;

public class CASTokenRequestHandlerTest {

  private static final String SAMPLE_TICKET = "ST-956-Lyg0BdLkgdrBO9W17bXS";

  /**
   * Tests that with no setting changes the ticket is returned.
   *
   * @throws SecurityServiceException
   */
  @Test
  public void testDefaultAddress() throws SecurityServiceException {
    // setup mock classes
    AttributePrincipal principal = mock(AttributePrincipal.class);
    when(principal.getProxyTicketFor(nullable(String.class))).thenReturn(SAMPLE_TICKET);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getUserPrincipal()).thenReturn(principal);

    CASTokenRequestHandler handler = new CASTokenRequestHandler();
    handler.setStsClientConfiguration(mock(STSClientConfiguration.class));
    Object token = handler.createToken(request);
    assertTrue(token instanceof AuthenticationToken);
    assertEquals(SAMPLE_TICKET, ((AuthenticationToken) token).getCredentials());
  }

  /**
   * Tests that an exception is thrown if there was no principal in the request.
   *
   * @throws SecurityServiceException
   */
  @Test(expected = SecurityServiceException.class)
  public void testNoAttribute() throws SecurityServiceException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    CASTokenRequestHandler handler = new CASTokenRequestHandler();
    handler.setStsClientConfiguration(mock(STSClientConfiguration.class));
    handler.createToken(request);
    fail("No Principal was added to request, code should throw an exception.");
  }
}
