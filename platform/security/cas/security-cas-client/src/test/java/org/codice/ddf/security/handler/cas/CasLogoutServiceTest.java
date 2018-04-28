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
package org.codice.ddf.security.handler.cas;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.common.SecurityTokenHolder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Test;

public class CasLogoutServiceTest {

  private CasLogoutService casLogoutService;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private HttpSession httpSession;

  @Before
  public void initialize() {
    CasLogoutAction casLogoutActionProvider = new CasLogoutAction();
    Action casLogoutAction = casLogoutActionProvider.getAction(null);
    casLogoutService = new CasLogoutService();
    casLogoutService.setCasServerLogoutUrl(casLogoutAction.getUrl().toString());

    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);

    httpSession = mock(HttpSession.class);
    SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
    when(httpSession.getAttribute(SecurityConstants.SAML_ASSERTION))
        .thenReturn(securityTokenHolder);
    when(httpSession.getId()).thenReturn("session-id");
    when(request.getSession(false)).thenReturn(httpSession);

    Subject subject = mock(Subject.class);
    when(subject.hasRole(anyString())).thenReturn(false);
    ThreadContext.bind(subject);

    System.setProperty("security.audit.roles", "none");
  }

  @Test
  public void testCasLogout() {
    casLogoutService.sendLogoutRequest(request, response);
    verify(httpSession).invalidate();
  }

  @Test
  public void testNullSubject() {
    ThreadContext.bind((Subject) null);
    casLogoutService.sendLogoutRequest(request, response);
    verify(httpSession).invalidate();
  }

  @Test
  public void testNullSystemProperty() {
    System.clearProperty("security.audit.roles");
    casLogoutService.sendLogoutRequest(request, response);
    verify(httpSession).invalidate();
  }
}
