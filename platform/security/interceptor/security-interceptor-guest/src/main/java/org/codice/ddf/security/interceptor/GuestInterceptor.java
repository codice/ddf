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
package org.codice.ddf.security.interceptor;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionPrincipalDefault;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.shiro.subject.PrincipalCollection;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Interceptor for guest access to SOAP endpoints. */
public class GuestInterceptor extends AbstractWSS4JInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(GuestInterceptor.class);

  private static final String WSS4J_CHECK_STRING = WSS4JInInterceptor.class.getName() + ".DONE";

  private SecurityManager securityManager;

  private Map<String, Subject> cachedGuestSubjectMap = new HashMap<>();

  public GuestInterceptor(SecurityManager securityManager) {
    super();
    LOGGER.trace("Constructing Legacy Guest Interceptor.");
    this.securityManager = securityManager;
    setPhase(Phase.PRE_PROTOCOL);
    // make sure this interceptor runs before the WSS4J one in the same Phase, otherwise it won't
    // work
    Set<String> before = getBefore();
    before.add(WSS4JInInterceptor.class.getName());
    before.add(PolicyBasedWSS4JInInterceptor.class.getName());
    LOGGER.trace("Exiting Legacy Guest Interceptor constructor.");
  }

  @Override
  public void handleMessage(SoapMessage message) throws Fault {

    if (message != null) {

      HttpServletRequest request =
          (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
      LOGGER.debug("Getting new Guest user token");
      // synchronize the step of requesting the assertion, it is not thread safe
      Principal principal = null;

      Subject subject = getSubject(request.getRemoteAddr());
      PrincipalCollection principals = subject.getPrincipals();
      SecurityAssertion securityAssertion = principals.oneByType(SecurityAssertion.class);
      if (securityAssertion != null) {
        principal = new SecurityAssertionPrincipalDefault(securityAssertion);
      } else {
        LOGGER.debug("Subject did not contain a security assertion");
      }

      message.put(SecurityContext.class, new DefaultSecurityContext(principal, null));
      message.put(WSS4J_CHECK_STRING, Boolean.TRUE);

    } else {
      LOGGER.debug("Incoming SOAP message is null - guest interceptor makes no sense.");
    }
  }

  private synchronized Subject getSubject(String ipAddress) {
    Subject subject = cachedGuestSubjectMap.get(ipAddress);
    if (Security.getInstance().tokenAboutToExpire(subject)) {
      GuestAuthenticationToken token = new GuestAuthenticationToken(ipAddress);
      LOGGER.debug("Getting new Guest user token for {}", ipAddress);
      try {
        subject = securityManager.getSubject(token);
        cachedGuestSubjectMap.put(ipAddress, subject);
      } catch (SecurityServiceException sse) {
        LOGGER.info("Unable to request subject for guest user.", sse);
      }

    } else {
      LOGGER.debug("Using cached Guest user token for {}", ipAddress);
    }
    return subject;
  }
}
