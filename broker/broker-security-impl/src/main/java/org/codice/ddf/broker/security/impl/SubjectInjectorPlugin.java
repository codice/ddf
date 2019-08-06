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
package org.codice.ddf.broker.security.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.protocol.amqp.broker.AMQPMessage;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.codice.ddf.broker.security.api.BrokerMessageInterceptor;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.STSAuthenticationTokenFactory;
import org.codice.ddf.security.util.SAMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SubjectInjectorPlugin implements BrokerMessageInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(SubjectInjectorPlugin.class);

  private static final Cache<String, Subject> SUBJECT_CACHE =
      CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

  private static SecurityManager securityManager;

  private static Set<String> configuredAddresses;

  @Override
  public void handleMessage(
      ServerSession session,
      Transaction tx,
      Message message,
      boolean direct,
      boolean noAutoCreateQueue) {
    if (!configuredAddresses.contains(message.getAddress())) {
      return;
    }
    String subjectAsString = getStringSubjectFromSession(session);

    message.putStringProperty("subject", subjectAsString);
    if (message instanceof AMQPMessage) {
      Map applicationPropertiesMap =
          ((AMQPMessage) message).getProtonMessage().getApplicationProperties().getValue();
      applicationPropertiesMap.put("subject", subjectAsString);
      ((AMQPMessage) message)
          .getProtonMessage()
          .setApplicationProperties(new ApplicationProperties(applicationPropertiesMap));
    }
  }

  /* Suppressed because Sonarqube thinks this should be a static method, but blueprint
   * does not work with static methods
   */
  @SuppressWarnings("squid:S2696")
  public void setConfiguredAddresses(Set<String> addresses) {
    configuredAddresses = new HashSet<>(addresses);
  }

  private Element getSubjectAsElement(ServerSession session) {
    try {
      Object token =
          SUBJECT_CACHE
              .get(session.getUsername(), () -> this.cacheAndReturnSubject(session))
              .getPrincipals()
              .byType(SecurityAssertion.class)
              .stream()
              .filter(securityAssertion -> securityAssertion.getToken() instanceof SecurityToken)
              .findFirst()
              .orElse(null);
      if (token instanceof SecurityToken) {
        return ((SecurityToken) token).getToken();
      } else {
        return null;
      }
    } catch (ExecutionException e) {
      LOGGER.warn("Could not get Subject from token", e.getCause());
      return null;
    }
  }

  @VisibleForTesting
  String getStringSubjectFromSession(ServerSession session) {
    return SAMLUtils.getInstance().getSubjectAsStringNoSignature(getSubjectAsElement(session));
  }

  /* Suppressed because Sonarqube thinks this should be a static method, but blueprint
   * does not work with static methods
   */
  @SuppressWarnings("squid:S2696")
  public void setSecurityManager(SecurityManager securityManager) {
    SubjectInjectorPlugin.securityManager = securityManager;
  }

  public SecurityManager getSecurityManager() {
    return securityManager;
  }

  public void clearCache() {
    SUBJECT_CACHE.invalidateAll();
  }

  @VisibleForTesting
  static Cache<String, Subject> getSubjectCache() {
    return SUBJECT_CACHE;
  }

  @VisibleForTesting
  Subject cacheAndReturnSubject(ServerSession session) throws SecurityServiceException {
    BaseAuthenticationToken usernamePasswordToken =
        new STSAuthenticationTokenFactory()
            .fromUsernamePassword(
                session.getUsername(), session.getPassword(), session.getDefaultAddress());
    return securityManager.getSubject(usernamePasswordToken);
  }
}
