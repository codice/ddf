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
package org.codice.ddf.security.filter.authorization;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValuePermission;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.junit.Before;
import org.junit.Test;

public class AuthorizationFilterTest {
  private static final String PATH = "/path";

  private boolean sucess = false;

  @Before
  public void setup() {
    sucess = false;
  }

  @Test
  public void testAuthorizedSubject() {
    ContextPolicyManager contextPolicyManager = new TestPolicyManager();
    contextPolicyManager.setContextPolicy(PATH, getMockContextPolicy());
    AuthorizationFilter loginFilter = new AuthorizationFilter(contextPolicyManager);
    loginFilter.init();
    Subject subject = mock(Subject.class);
    when(subject.isPermitted(any(CollectionPermission.class))).thenReturn(true);
    ThreadContext.bind(subject);

    HttpServletRequest servletRequest = getMockServletRequest();
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);

    FilterChain filterChain = (request, response) -> sucess = true;

    try {
      loginFilter.doFilter(servletRequest, servletResponse, filterChain);
      if (!sucess) {
        fail("Should have called doFilter with a valid Subject");
      }
    } catch (IOException | AuthenticationException e) {
      fail(e.getMessage());
    }
    ThreadContext.unbindSubject();
  }

  @Test
  public void testUnAuthorizedSubject() {
    ContextPolicyManager contextPolicyManager = new TestPolicyManager();
    contextPolicyManager.setContextPolicy(PATH, getMockContextPolicy());
    AuthorizationFilter loginFilter = new AuthorizationFilter(contextPolicyManager);
    loginFilter.init();
    Subject subject = mock(Subject.class);
    when(subject.isPermitted(any(CollectionPermission.class))).thenReturn(false);
    ThreadContext.bind(subject);

    HttpServletRequest servletRequest = getMockServletRequest();
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain =
        (request, response) -> fail("Should not have called doFilter without a valid Subject");

    try {
      loginFilter.doFilter(servletRequest, servletResponse, filterChain);
    } catch (IOException | AuthenticationException e) {
      fail(e.getMessage());
    }
    ThreadContext.unbindSubject();
  }

  @Test
  public void testNoSubject() {
    ContextPolicyManager contextPolicyManager = new TestPolicyManager();
    contextPolicyManager.setContextPolicy(PATH, getMockContextPolicy());
    AuthorizationFilter loginFilter = new AuthorizationFilter(contextPolicyManager);
    loginFilter.init();

    HttpServletRequest servletRequest = getMockServletRequest();
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain =
        (request, response) -> fail("Should not have called doFilter without a valid Subject");

    try {
      loginFilter.doFilter(servletRequest, servletResponse, filterChain);
    } catch (IOException | AuthenticationException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testBadSubject() {
    ContextPolicyManager contextPolicyManager = new TestPolicyManager();
    contextPolicyManager.setContextPolicy(PATH, getMockContextPolicy());
    AuthorizationFilter loginFilter = new AuthorizationFilter(contextPolicyManager);
    loginFilter.init();
    HttpServletRequest servletRequest = getMockServletRequest();
    servletRequest.setAttribute(SecurityConstants.SECURITY_SUBJECT, mock(Subject.class));
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain =
        (request, response) -> fail("Should not have called doFilter without a valid Subject");

    try {
      loginFilter.doFilter(servletRequest, servletResponse, filterChain);
    } catch (IOException | AuthenticationException e) {
      fail(e.getMessage());
    }
  }

  private ContextPolicy getMockContextPolicy() {
    ContextPolicy contextPolicy = mock(ContextPolicy.class);
    when(contextPolicy.getAuthenticationMethods()).thenReturn(Collections.singletonList("BASIC"));
    when(contextPolicy.getAllowedAttributePermissions())
        .thenReturn(
            new CollectionPermission(
                PATH, new KeyValuePermission(PATH, Collections.singleton("permission"))));
    when(contextPolicy.getContextPath()).thenReturn(PATH);
    when(contextPolicy.getRealm()).thenReturn("DDF");

    return contextPolicy;
  }

  private HttpServletRequest getMockServletRequest() {
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getRequestURI()).thenReturn(PATH);
    when(servletRequest.getAttribute(anyString())).thenReturn(null);
    return servletRequest;
  }

  private class TestPolicyManager implements ContextPolicyManager {
    Map<String, ContextPolicy> stringContextPolicyMap = new HashMap<String, ContextPolicy>();

    @Override
    public ContextPolicy getContextPolicy(String path) {
      return stringContextPolicyMap.get(path);
    }

    @Override
    public Collection<ContextPolicy> getAllContextPolicies() {
      return stringContextPolicyMap.values();
    }

    @Override
    public void setContextPolicy(String path, ContextPolicy contextPolicy) {
      stringContextPolicyMap.put(path, contextPolicy);
    }

    @Override
    public boolean isWhiteListed(String path) {
      return false;
    }
  }
}
