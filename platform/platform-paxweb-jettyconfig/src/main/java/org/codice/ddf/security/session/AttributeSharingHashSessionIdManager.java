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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.platform.session.api.HttpSessionInvalidator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom implementation of the {@link org.eclipse.jetty.server.SessionIdManager} that holds the
 * latest session attributes for all sessions in the Jetty server. This allows {@link
 * AttributeSharingSessionDataStore} instances to retrieve these session attributes when a new
 * session is being created in their context for a session that exists in another context.
 *
 * <p>Note: Needed to hook into Jetty with a <code>SessionIdManager</code> because it's the only way
 * to be notified when a session is invalidated across all contexts. Note: This extends Jetty's impl
 * instead of the abstract class because of a hack in pax-web.
 */
public class AttributeSharingHashSessionIdManager extends DefaultSessionIdManager {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AttributeSharingHashSessionIdManager.class);

  static class SharingSessionInvalidator implements HttpSessionInvalidator {

    private final AttributeSharingHashSessionIdManager idManager;

    SharingSessionInvalidator(AttributeSharingHashSessionIdManager idManager) {
      this.idManager = idManager;
    }

    @Override
    public void invalidateSession(
        String subjectName, Function<Map<String, Object>, String> sessionSubjectExtractor) {

      final Optional<String> sessionIdOptional =
          getSessionAttributeMapsStream()
              .filter(e -> subjectName.equals(sessionSubjectExtractor.apply(e.getValue())))
              .map(e -> e.getKey())
              .findFirst();

      if (sessionIdOptional.isPresent()) {
        idManager.invalidateSession(sessionIdOptional.get());
      }
    }

    private Stream<Entry<String, Map<String, Object>>> getSessionAttributeMapsStream() {
      return Collections.unmodifiableMap(idManager.latestSessionAttributes).entrySet().stream();
    }
  }

  private List<AttributeSharingSessionDataStore> dataStores = new ArrayList<>();
  private Map<String, Map<String, Object>> latestSessionAttributes = new ConcurrentHashMap();

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

  public AttributeSharingHashSessionIdManager(Server server) {
    super(server);
    registerSessionManager();
  }

  public AttributeSharingHashSessionIdManager(Server server, Random random) {
    super(server, random);
    registerSessionManager();
  }

  /** @see org.eclipse.jetty.server.SessionIdManager#invalidateAll(String) */
  @Override
  public void invalidateAll(String id) {
    synchronized (this) {
      latestSessionAttributes.remove(id);
    }

    super.invalidateAll(id);
  }

  /**
   * Called by the {@link SharingSessionInvalidator} to invalidate a session, given its id.
   *
   * @param id the session id
   */
  private void invalidateSession(String id) {
    SessionHandler[] tmp = (SessionHandler[]) _server.getChildHandlersByClass(SessionHandler.class);
    if (tmp != null && tmp.length > 0) {
      tmp[0].getSession(id).invalidate();
    }
  }

  protected void addSessionDataStore(AttributeSharingSessionDataStore dataStore) {
    dataStores.add(dataStore);
  }

  /**
   * Called by {@link AttributeSharingSessionDataStore} when it's storing a session's attributes.
   * This method will determine if they're different from what's in the cache. If they are
   * different, it will store it and notify all <code>AttributeSharingSessionDataStore</code>
   * instances.
   *
   * @param callingDataStore the datastore providing the attributes
   * @param id the session id
   * @param sessionAttributes the session attributes
   */
  protected void provideNewSessionAttributes(
      AttributeSharingSessionDataStore callingDataStore,
      String id,
      Map<String, Object> sessionAttributes) {
    // Make sure these attributes are different than the latest.
    if (!sessionAttributes.equals(getLatestSessionAttributes(id))) {
      LOGGER.debug("Pushing new session attributes to all web contexts for session {}", id);
      latestSessionAttributes.put(id, sessionAttributes);
      dataStores.forEach(
          ds -> {
            if (ds != callingDataStore) {
              ds.updateSessionAttributes(id, sessionAttributes);
            }
          });
    }
  }

  /**
   * Called by {@link AttributeSharingSessionDataStore} when a new session is being created in their
   * context for a session that exists in another context.
   *
   * @param id the session id
   * @return the session attributes
   */
  protected Map<String, Object> getLatestSessionAttributes(String id) {
    return latestSessionAttributes.get(id);
  }
}
