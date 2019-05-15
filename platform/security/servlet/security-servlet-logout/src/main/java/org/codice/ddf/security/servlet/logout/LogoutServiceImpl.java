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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.security.logout.service.LogoutService;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;

public class LogoutServiceImpl implements LogoutService {

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private List<ActionProvider> logoutActionProviders;

  private SessionFactory httpSessionFactory;

  private SecurityManager securityManager;

  @Override
  public String getActionProviders(HttpServletRequest request) throws SecurityServiceException {

    HttpSession session = httpSessionFactory.getOrCreateSession(request);
    SecurityToken token =
        ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION))
            .getSecurityToken();

    Subject subject = securityManager.getSubject(token);
    List<Map<String, String>> actionPropertiesList = new ArrayList<>();

    for (ActionProvider actionProvider : logoutActionProviders) {
      Map<String, String> actionProperties = new HashMap<>();
      Action action = actionProvider.getAction(subject);
      if (action != null) {
        String displayName = SubjectUtils.getName(subject, "", true);
        actionProperties.put("title", action.getTitle());
        actionProperties.put("auth", displayName);
        actionProperties.put("description", action.getDescription());
        actionProperties.put("url", action.getUrl().toString());
        actionPropertiesList.add(actionProperties);
      }
    }

    return GSON.toJson(actionPropertiesList);
  }

  public void setHttpSessionFactory(SessionFactory httpSessionFactory) {
    this.httpSessionFactory = httpSessionFactory;
  }

  public void setLogoutActionProviders(List<ActionProvider> logoutActionProviders) {
    this.logoutActionProviders = logoutActionProviders;
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }
}
