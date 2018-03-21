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
//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//
package org.codice.ddf.security.session;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.platform.session.api.HttpSessionInvalidator;
import org.codice.ddf.platform.util.RandomNumberGenerator;
import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom implementation of the {@link org.eclipse.jetty.server.SessionIdManager} that shares
 * session data between sessions in a cluster.
 */
public class AttributeSharingHashSessionIdManager extends HashSessionIdManager {
  // changed this to extend Jetty's impl instead of the abstract class because of a hack in pax web

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AttributeSharingHashSessionIdManager.class);

  static class SharingSessionInvalidator implements HttpSessionInvalidator {

    private final AttributeSharingHashSessionIdManager idManager;

    SharingSessionInvalidator(AttributeSharingHashSessionIdManager idManager) {
      this.idManager = idManager;
    }

    @Override
    public void invalidateSession(
        String subjectName, Function<HttpSession, String> sessionSubjectExtractor) {

      final List<AbstractSession> matchingSessions =
          getHttpSessionStream()
              .filter(s -> subjectName.equals(sessionSubjectExtractor.apply(s)))
              .filter(AbstractSession.class::isInstance)
              .map(AbstractSession.class::cast)
              .distinct()
              .collect(Collectors.toList());
      for (AbstractSession matchingSession : matchingSessions) {
        idManager.removeSession(matchingSession);
        if (matchingSession.isValid()) {
          matchingSession.invalidate();
        }
      }
    }

    private Stream<HttpSession> getHttpSessionStream() {
      return getFullSessions()
          .stream()
          .flatMap(Collection::stream)
          .map(Reference::get)
          .filter(Objects::nonNull);
    }

    private Collection<Set<WeakReference<HttpSession>>> getFullSessions() {
      return Collections.unmodifiableCollection(idManager.sessionMap.values());
    }
  }

  private final Map<String, Set<WeakReference<HttpSession>>> sessionMap = new ConcurrentHashMap<>();

  private void registerSessionManager() {
    Bundle bundle = FrameworkUtil.getBundle(AttributeSharingHashSessionIdManager.class);
    if (bundle == null) {
      LOGGER.error("Error initializing Session Manager");
      return;
    }
    final BundleContext bundleContext = bundle.getBundleContext();
    if (bundleContext == null) {
      LOGGER.error("Error initializing Session Manager");
      return;
    }

    final SharingSessionInvalidator sm = new SharingSessionInvalidator(this);
    final Dictionary<String, Object> props = new DictionaryMap<>();
    props.put(Constants.SERVICE_PID, sm.getClass().getName());
    props.put(Constants.SERVICE_DESCRIPTION, "Sharing Session Invalidator");
    props.put(Constants.SERVICE_VENDOR, "Codice Foundation");
    props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);

    bundleContext.registerService(HttpSessionInvalidator.class.getName(), sm, props);
  }

  public AttributeSharingHashSessionIdManager() {
    super(RandomNumberGenerator.create());
    registerSessionManager();
  }

  public AttributeSharingHashSessionIdManager(Random random) {
    super(RandomNumberGenerator.create());
    registerSessionManager();
  }

  /** @return Collection of String session IDs */
  @Override
  public Collection<String> getSessions() {
    return Collections.unmodifiableCollection(sessionMap.keySet());
  }

  /** @return Collection of Sessions for the passed session ID */
  @Override
  public Collection<HttpSession> getSession(String id) {
    ArrayList<HttpSession> sessions = new ArrayList<>();
    Set<WeakReference<HttpSession>> refs = sessionMap.get(id);
    if (refs != null) {
      for (WeakReference<HttpSession> ref : refs) {
        HttpSession session = ref.get();
        if (session != null) {
          sessions.add(session);
        }
      }
    }
    return sessions;
  }

  @Override
  protected void doStop() throws Exception {
    sessionMap.clear();
    super.doStop();
  }

  /** @see org.eclipse.jetty.server.SessionIdManager#idInUse(String) */
  @Override
  public boolean idInUse(String id) {
    return sessionMap.containsKey(id);
  }

  /** @see org.eclipse.jetty.server.SessionIdManager#addSession(HttpSession) */
  @Override
  public void addSession(HttpSession session) {
    String id = getClusterId(session.getId());
    WeakReference<HttpSession> ref = new WeakReference<>(session);

    synchronized (this) {
      Set<WeakReference<HttpSession>> sessions = sessionMap.get(id);
      if (sessions == null) {
        sessions = new HashSet<>();
        sessionMap.put(id, sessions);
      } else {
        copySessionInfo(session, sessions);
      }
      sessions.add(ref);
    }
  }

  @SuppressWarnings("squid:S2441" /* Value stored in session will not be serialized */)
  private void copySessionInfo(HttpSession session, Set<WeakReference<HttpSession>> sessions) {
    // Check for session already in cluster, copy over session information to new session
    final HttpSession httpSession =
        Optional.of(sessions.iterator())
            .filter(Iterator::hasNext)
            .map(Iterator::next)
            .map(Reference::get)
            .orElse(null);

    if (httpSession == null) {
      return;
    }

    Enumeration enumeration = httpSession.getAttributeNames();
    while (enumeration.hasMoreElements()) {
      Object obj = enumeration.nextElement();
      if (obj instanceof String) {
        Object value = httpSession.getAttribute((String) obj);
        if (value != null) {
          session.setAttribute((String) obj, value);
        }
      }
    }
    session.setMaxInactiveInterval(httpSession.getMaxInactiveInterval());
  }

  /** @see org.eclipse.jetty.server.SessionIdManager#removeSession(HttpSession) */
  @Override
  public void removeSession(HttpSession session) {
    String id = getClusterId(session.getId());

    synchronized (this) {
      Collection<WeakReference<HttpSession>> sessions = sessionMap.get(id);
      if (sessions != null) {
        Iterator<WeakReference<HttpSession>> iter = sessions.iterator();
        while (iter.hasNext()) {
          WeakReference<HttpSession> ref = iter.next();
          HttpSession s = ref.get();
          if (s == null) {
            iter.remove();
          }
          if (s == session) {
            iter.remove();
            break;
          }
        }
        if (sessions.isEmpty()) {
          sessionMap.remove(id);
        }
      }
    }
  }

  /** @see org.eclipse.jetty.server.SessionIdManager#invalidateAll(String) */
  @Override
  public void invalidateAll(String id) {
    Collection<WeakReference<HttpSession>> sessions;
    synchronized (this) {
      sessions = sessionMap.remove(id);
    }

    if (sessions != null) {
      sessions
          .stream()
          .map(Reference::get)
          .filter(Objects::nonNull)
          .filter(AbstractSession.class::isInstance)
          .map(AbstractSession.class::cast)
          .filter(AbstractSession::isValid)
          .forEach(AbstractSession::invalidate);
      sessions.clear();
    }
  }

  /**
   * Get the session ID without any worker ID.
   *
   * @param nodeId the node id
   * @return sessionId without any worker ID.
   */
  @Override
  public String getClusterId(String nodeId) {
    int dot = nodeId.lastIndexOf('.');
    return (dot > 0) ? nodeId.substring(0, dot) : nodeId;
  }

  /**
   * Get the session ID with any worker ID.
   *
   * @param clusterId
   * @param request
   * @return sessionId plus any worker ID.
   */
  @Override
  public String getNodeId(String clusterId, HttpServletRequest request) {
    // used in Ajp13Parser
    String worker =
        request == null ? null : (String) request.getAttribute("org.eclipse.jetty.ajp.JVMRoute");
    if (worker != null) {
      return clusterId + '.' + worker;
    }

    if (_workerName != null) {
      return clusterId + '.' + _workerName;
    }

    return clusterId;
  }

  @Override
  public void renewSessionId(String oldClusterId, String oldNodeId, HttpServletRequest request) {
    // generate a new id
    String newClusterId = newSessionId(request.hashCode());

    synchronized (this) {
      Set<WeakReference<HttpSession>> sessions =
          sessionMap.remove(
              oldClusterId); // get the list of sessions with same id from other contexts
      if (sessions != null) {
        for (Iterator<WeakReference<HttpSession>> iter = sessions.iterator(); iter.hasNext(); ) {
          WeakReference<HttpSession> ref = iter.next();
          HttpSession s = ref.get();
          if (s == null) {
            continue;
          } else {
            if (s instanceof AbstractSession) {
              AbstractSession abstractSession = (AbstractSession) s;
              abstractSession
                  .getSessionManager()
                  .renewSessionId(
                      oldClusterId, oldNodeId, newClusterId, getNodeId(newClusterId, request));
            }
          }
        }
        sessionMap.put(newClusterId, sessions);
      }
    }
  }
}
