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
package org.codice.ddf.security.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of {@link org.eclipse.jetty.server.session.SessionDataStore} communicates
 * with the {@link AttributeSharingHashSessionIdManager} to retrieve the most up-to-date session
 * attributes.
 *
 * <p>There is one <code>SessionDataStore</code> instance for each HTTP context on the Jetty server.
 * In the current way we use pax-web with our jax-rs servers, session attributes are not shared
 * across contexts. Our SSO solution necessitates a way to share those session attributes since the
 * security token is stored on the session as an attribute.
 */
public class AttributeSharingSessionDataStore extends AbstractSessionDataStore {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AttributeSharingSessionDataStore.class);

  private final Map<String, SessionData> sessionDataMap = new HashMap<>();
  private AttributeSharingHashSessionIdManager attributeSharingHashSessionIdManager;

  /**
   * Called by the {@link AttributeSharingHashSessionIdManager} whenever it updates one of its
   * session's attributes.
   *
   * @param id the session id
   * @param sessionAttributes the session's attributes
   */
  public void updateSessionAttributes(String id, Map<String, Object> sessionAttributes) {
    SessionData sessionData;
    synchronized (sessionDataMap) {
      sessionData = sessionDataMap.get(id);
    }

    if (sessionData != null && !sessionData.getAllAttributes().equals(sessionAttributes)) {
      LOGGER.trace(
          "Storing new attributes for session {} at context {}",
          id,
          _context.getCanonicalContextPath());
      synchronized (sessionDataMap) {
        sessionData.clearAllAttributes();
        sessionData.putAllAttributes(sessionAttributes);
      }
    }
  }

  @Override
  public void initialize(SessionContext context) throws Exception {
    super.initialize(context);
    this.attributeSharingHashSessionIdManager =
        (AttributeSharingHashSessionIdManager) context.getSessionHandler().getSessionIdManager();
    attributeSharingHashSessionIdManager.addSessionDataStore(this);
  }

  @Override
  public void doStore(String id, SessionData data, long lastSaveTime) {
    attributeSharingHashSessionIdManager.provideNewSessionAttributes(
        this, id, data.getAllAttributes());

    synchronized (sessionDataMap) {
      sessionDataMap.put(id, data);
    }
  }

  @Override
  public SessionData newSessionData(
      String id, long created, long accessed, long lastAccessed, long maxInactiveMs) {
    SessionData sessionData =
        new SessionData(
            id,
            _context.getCanonicalContextPath(),
            _context.getVhost(),
            created,
            accessed,
            lastAccessed,
            maxInactiveMs);

    Map<String, Object> sessionAttributes =
        attributeSharingHashSessionIdManager.getLatestSessionAttributes(id);
    if (sessionAttributes != null) sessionData.putAllAttributes(sessionAttributes);

    return sessionData;
  }

  @Override
  public Set<String> doGetExpired(Set<String> candidates) {
    final long now = System.currentTimeMillis();

    return candidates.stream().filter(c -> isExpired(c, now)).collect(Collectors.toSet());
  }

  @Override
  public boolean isPassivating() {
    return true;
  }

  @Override
  public boolean exists(String id) {
    return sessionDataMap.containsKey(id);
  }

  @Override
  public SessionData load(String id) {
    SessionData sessionData = sessionDataMap.get(id);

    return sessionData;
  }

  @Override
  public boolean delete(String id) {
    SessionData sessionData;
    synchronized (sessionDataMap) {
      sessionData = sessionDataMap.remove(id);
    }

    return sessionData != null;
  }

  private boolean isExpired(String candidateId, long now) {
    SessionData sessionData;
    synchronized (sessionDataMap) {
      sessionData = sessionDataMap.get(candidateId);
    }
    return sessionData != null && sessionData.getExpiry() < now;
  }
}
