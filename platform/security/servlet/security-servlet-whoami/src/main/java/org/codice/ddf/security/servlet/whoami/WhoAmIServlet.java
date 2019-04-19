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
package org.codice.ddf.security.servlet.whoami;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.security.SecurityConstants;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhoAmIServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(WhoAmIServlet.class);

  private static final long serialVersionUID = 5538001643612956658L;

  private static SessionFactory httpSessionFactory;

  private static SecurityManager securityManager;

  private static Gson gson =
      new GsonBuilder()
          .serializeNulls()
          .setPrettyPrinting()
          .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
          .serializeNulls()
          .create();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    resp.setHeader("Cache-Control", "no-cache, no-store");
    resp.setHeader("Pragma", "no-cache");

    HttpSession session = httpSessionFactory.getOrCreateSession(req);
    SecurityToken token =
        ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION))
            .getSecurityToken();

    String whoAmIJson = "";
    try {
      WhoAmI whoAmI = new WhoAmI(securityManager.getSubject(token));
      whoAmIJson = gson.toJson(whoAmI);
    } catch (SecurityServiceException e) {
      LOGGER.debug("Unable to get subject from token.", e);
    }

    resp.setContentType("application/json");
    try {
      resp.getWriter().print(whoAmIJson);
    } catch (IOException ex) {
      LOGGER.debug("Unable to write to response for /whoami", ex);
    }
  }

  public void setHttpSessionFactory(SessionFactory sessionFactory) {
    httpSessionFactory = sessionFactory; // NOSONAR Blueprint cannot use a static setter but
    // servlet field should be final and/or static.
  }

  public void setSecurityManager(SecurityManager manager) {
    securityManager = manager; // NOSONAR
  }
}
