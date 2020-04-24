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
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.security.SecurityConstants;
import ddf.security.common.PrincipalHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.service.impl.SubjectUtils;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpSession;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogoutServiceImplTest {

  private static SessionFactory sessionFactory;

  private static SecurityManager sm;

  @BeforeClass
  public static void initialize() {
    sessionFactory = mock(SessionFactory.class);
    HttpSession httpSession = mock(HttpSession.class);
    PrincipalHolder principalHolder = mock(PrincipalHolder.class);
    sm = mock(SecurityManager.class);

    when(sessionFactory.getOrCreateSession(null)).thenReturn(httpSession);
    when(httpSession.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY))
        .thenReturn(principalHolder);
    when(principalHolder.getPrincipals()).thenReturn(new SimplePrincipalCollection());
  }

  @Test
  public void testLogout() throws ParseException, SecurityServiceException {
    MockLogoutAction mockLogoutActionProvider = new MockLogoutAction();
    Action defaultLogoutAction = mockLogoutActionProvider.getAction(null);

    LogoutServiceImpl logoutServiceImpl = new LogoutServiceImpl();
    logoutServiceImpl.setSubjectOperations(new SubjectUtils());
    logoutServiceImpl.setHttpSessionFactory(sessionFactory);
    logoutServiceImpl.setSecurityManager(sm);
    logoutServiceImpl.setLogoutActionProviders(ImmutableList.of(mockLogoutActionProvider));

    String responseMessage = logoutServiceImpl.getActionProviders(null, null);

    JSONObject defaultActionProperty = (JSONObject) new JSONParser().parse(responseMessage);

    assertEquals(defaultActionProperty.get("description"), defaultLogoutAction.getDescription());
    assertEquals(defaultActionProperty.get("title"), defaultLogoutAction.getTitle());
    assertEquals(defaultActionProperty.get("url"), defaultLogoutAction.getUrl().toString());
  }

  public class MockLogoutAction implements ActionProvider {

    @Override
    public <T> Action getAction(T subjectMap) {
      try {
        return new ActionImpl(
            "security.logout.test",
            "Test Logout",
            "Test",
            new URL("https://localhost:8993/logout/test"));
      } catch (MalformedURLException e) {
        return null;
      }
    }

    @Override
    public String getId() {
      return "security.logout.test";
    }
  }
}
