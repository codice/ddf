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
package ddf.security.http.impl;

import com.google.common.hash.Hashing;
import ddf.security.SecurityConstants;
import ddf.security.audit.SecurityLogger;
import ddf.security.common.PrincipalHolder;
import ddf.security.http.SessionFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;

public class HttpSessionFactory implements SessionFactory {

  private int expirationTime;

  private SecurityLogger securityLogger;

  /**
   * Synchronized method because of jettys getSession method is not thread safe. Additionally,
   * assures a SAML {@link PrincipalHolder} has been set on the {@link
   * SecurityConstants#SECURITY_TOKEN_KEY} attribute
   *
   * @param httpRequest
   * @return
   */
  @Override
  public synchronized HttpSession getOrCreateSession(HttpServletRequest httpRequest) {
    /*
     * DDF-6587 - This check is made so that when simultaneous requests are received for the same
     * context, the second request will not attempt to create a new session.  This method is
     * synchronized so the first request will create a new session object and the second one
     * look up the previously created session object from the jetty session cache.  This is
     * necessary because at this point in the processing chain, jetty was not able to associate
     * a session for either of the simultaneous requests.  Therefore, we need to manually attach
     * the session to the second request.  Otherwise, jetty will attempt to recreate the same
     * session object and fail, causing the second request to fail.
     */
    if (httpRequest instanceof Request) {
      try {
        Request request = (Request) httpRequest;
        SessionHandler sessionHandler = request.getSessionHandler();
        SessionCache sessionCache = sessionHandler.getSessionCache();
        String sessionId =
            sessionHandler.getSessionIdManager().newSessionId(request, System.currentTimeMillis());
        Session cachedSession = sessionCache.get(sessionId);
        if (cachedSession != null
            && cachedSession instanceof HttpSession
            && cachedSession.isValid()) {
          HttpSession session = (HttpSession) cachedSession;
          request.enterSession(session);
          request.setSession(session);
          return session;
        }
      } catch (Exception e) {
        // unable to get session from cache, let a new one get created.
      }
    }

    HttpSession session = httpRequest.getSession(true);
    if (session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY) == null) {
      session.setMaxInactiveInterval(Math.toIntExact(TimeUnit.MINUTES.toSeconds(expirationTime)));
      session.setAttribute(SecurityConstants.SECURITY_TOKEN_KEY, new PrincipalHolder());
      securityLogger.audit(
          "Creating a new session with id {} for client {}.",
          Hashing.sha256().hashString(session.getId(), StandardCharsets.UTF_8).toString(),
          httpRequest.getRemoteAddr());
    }
    return session;
  }

  public void setExpirationTime(int expirationTime) {
    this.expirationTime = expirationTime;
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
