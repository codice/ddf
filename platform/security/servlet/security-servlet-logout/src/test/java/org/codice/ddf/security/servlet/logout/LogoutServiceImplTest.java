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
package org.codice.ddf.security.servlet.logout;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.action.Action;
import ddf.security.SecurityConstants;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import javax.servlet.http.HttpSession;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogoutServiceImplTest {

  private static SessionFactory sessionFactory;

  private static SecurityManager sm;

  @BeforeClass
  public static void initialize() {
    sessionFactory = mock(SessionFactory.class);
    HttpSession httpSession = mock(HttpSession.class);
    SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);
    sm = mock(SecurityManager.class);

    when(sessionFactory.getOrCreateSession(null)).thenReturn(httpSession);
    when(httpSession.getAttribute(SecurityConstants.SAML_ASSERTION))
        .thenReturn(securityTokenHolder);
    when(securityTokenHolder.getSecurityToken()).thenReturn(new SecurityToken());
  }

  @Test
  public void testLogout() throws ParseException, SecurityServiceException {
    DefaultLogoutAction defaultLogoutActionProvider = new DefaultLogoutAction();
    Action defaultLogoutAction = defaultLogoutActionProvider.getAction(null);

    LogoutServiceImpl logoutServiceImpl = new LogoutServiceImpl();
    logoutServiceImpl.setHttpSessionFactory(sessionFactory);
    logoutServiceImpl.setSecurityManager(sm);
    logoutServiceImpl.setLogoutActionProviders(ImmutableList.of(defaultLogoutActionProvider));

    String responseMessage = logoutServiceImpl.getActionProviders(null);

    JSONArray actionProperties = (JSONArray) new JSONParser().parse(responseMessage);
    assertEquals(1, actionProperties.size());
    JSONObject defaultActionProperty = ((JSONObject) actionProperties.get(0));

    assertEquals(defaultActionProperty.get("description"), defaultLogoutAction.getDescription());
    assertEquals(defaultActionProperty.get("title"), defaultLogoutAction.getTitle());
    assertEquals(defaultActionProperty.get("url"), defaultLogoutAction.getUrl().toString());
  }
}
