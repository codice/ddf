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
package org.codice.ddf.ui.searchui.query.controller;

import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import ddf.security.assertion.SecurityAssertion;
import java.security.Principal;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The {@code AbstractEventController} handles the processing and routing of events. */
@Service
public abstract class AbstractEventController implements EventHandler {
  public static final java.lang.String EVENT_TOPIC_CANCEL = "download/action/cancel";

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventController.class);

  private final ExecutorService executorService =
      Executors.newCachedThreadPool(
          StandardThreadFactoryBuilder.newThreadFactory("abstractEventThread"));

  @Inject BayeuxServer bayeux;

  @Session ServerSession controllerServerSession;

  PersistentStore persistentStore;

  EventAdmin eventAdmin;

  ConcurrentHashMap<String, ServerSession> userSessionMap = new ConcurrentHashMap<>();

  /**
   * Establishes {@code AbstractEventController} as a listener to events published by the OSGi
   * eventing framework on the event's root topic
   *
   * @param bundleContext
   */
  public AbstractEventController(
      PersistentStore persistentStore, BundleContext bundleContext, EventAdmin eventAdmin) {
    Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
    dictionary.put(EventConstants.EVENT_TOPIC, getControllerRootTopic());

    bundleContext.registerService(EventHandler.class.getName(), this, dictionary);

    this.persistentStore = persistentStore;
    this.eventAdmin = eventAdmin;
  }

  /**
   * Obtains the {@link ServerSession} associated with a given user id.
   *
   * @param userId The id of the user associated with the {@code ServerSession} to be retrieved.
   * @return The {@code ServerSession} associated with the received userId or null if the user does
   *     not have an established {@code ServerSession}
   */
  public ServerSession getSessionByUserId(String userId) {
    return userSessionMap.get(userId);
  }

  /**
   * Obtains the {@link ServerSession} associated with a given sessionId. Retrieval using {@link
   * #getSessionByUserId(String)} is the preferred method. This method is used when a userId is not
   * available.
   *
   * @param sessionId The sessionId associated with the {@code ServerSession} to be retrieved.
   * @return The {@code ServerSession} associated with the received sessionId or null if the session
   *     does not have an established {@code ServerSession}
   */
  public ServerSession getSessionBySessionId(String sessionId) {
    return userSessionMap.searchValues(
        1,
        value -> {
          if (value.getId().equals(sessionId)) {
            return value;
          }
          return null;
        });
  }

  /**
   * Obtains the {@link ServerSession} associated with a given userId or sessionId. It
   *
   * @param sessionId The sessionId associated with the {@code ServerSession} to be retrieved.
   * @return The {@code ServerSession} associated with the received sessionId or null if the session
   *     does not have an established {@code ServerSession}
   */
  public ServerSession getSessionById(String userId, String sessionId) {
    ServerSession session = null;
    if (userId != null) {
      session = getSessionByUserId(userId);
    }
    if (session == null && sessionId != null) {
      session = getSessionBySessionId(sessionId);
    }
    return session;
  }

  /**
   * Listens to the /meta/disconnect {@link org.cometd.bayeux.Channel} for clients disconnecting and
   * deregisters the user. This should be invoked in order to remove {@code AbstractEventController}
   * references to invalid {@link ServerSession}s.
   *
   * @param serverSession The {@code ServerSession} object associated with the client that is
   *     disconnecting
   * @param serverMessage The {@link ServerMessage} that was sent from the client on the
   *     /meta/disconnect Channel
   */
  @Listener("/meta/disconnect")
  public void deregisterUserSession(ServerSession serverSession, ServerMessage serverMessage) {
    LOGGER.debug("\nServerSession: {}\nServerMessage: {}", serverSession, serverMessage);

    if (null == serverSession) {
      throw new IllegalArgumentException("ServerSession is null");
    }

    ddf.security.Subject subject = null;
    try {
      subject =
          (ddf.security.Subject)
              bayeux.getContext().getRequestAttribute(SecurityConstants.SECURITY_SUBJECT);
    } catch (Exception e) {
      LOGGER.debug("Couldn't grab user subject from Shiro.", e);
    }

    String userId = getUserId(serverSession, subject);

    if (null == userId) {
      throw new IllegalArgumentException("User ID is null");
    }

    userSessionMap.remove(userId);
  }

  /**
   * Called by {@link ddf.catalog.event.retrievestatus.DownloadStatusInfoImpl.cancelDownload} to
   * fire a cancel event.
   *
   * @param userId The Id assigned to the user who is downloading.
   * @param downloadIdentifier The randomly generated downloadId string assigned to the download at
   *     its start.
   */
  public void adminCancelDownload(String userId, String downloadIdentifier) {
    String downloadId = userId + downloadIdentifier;

    Map<String, Object> propMap = new HashMap<>();
    propMap.put(ActivityEvent.DOWNLOAD_ID_KEY, downloadId);

    Event event = new Event(ActivityEvent.EVENT_TOPIC_DOWNLOAD_CANCEL, propMap);
    eventAdmin.postEvent(event);
  }

  /**
   * Enables private message delivery to a given user. As of CometD version 2.8.0, this must be
   * called from the canHandshake method of a {@link org.cometd.bayeux.server.SecurityPolicy}. See
   * <a href=
   * "http://stackoverflow.com/questions/22695516/null-serversession-on-cometd-meta-handshake"
   * >Obtaining user and session information for private message delivery</a> for more information.
   *
   * @param serverSession The {@link ServerSession} on which to deliver messages to the user for the
   *     user.
   * @param serverMessage The {@link ServerMessage} containing the userId property with which to
   *     associate the {@code ServerSession}.
   * @throws IllegalArgumentException when the received {@code ServerSession} or the {@code
   *     ServerSession}'s id is null.
   */
  public void registerUserSession(final ServerSession serverSession, ServerMessage serverMessage)
      throws IllegalArgumentException {
    if (bayeux == null || bayeux.getContext() == null) {
      LOGGER.info("CometD server has not initialized yet.");
      return;
    }

    LOGGER.debug("ServerSession: {}\nServerMessage: {}", serverSession, serverMessage);

    if (null == serverSession) {
      throw new IllegalArgumentException("ServerSession is null");
    }

    ddf.security.Subject subject = null;
    try {
      subject =
          (ddf.security.Subject)
              bayeux.getContext().getRequestAttribute(SecurityConstants.SECURITY_SUBJECT);
    } catch (Exception e) {
      LOGGER.debug("Couldn't grab user subject from Shiro.", e);
    }

    String userId = getUserId(serverSession, subject);

    if (null == userId) {
      throw new IllegalArgumentException("User ID is null");
    }

    // check if user is a guest. Register with sessionId to avoid cross-notifications
    if (userId.startsWith(SubjectUtils.GUEST_DISPLAY_NAME + "@")) {
      userId = serverSession.getId();
    }

    userSessionMap.put(userId, serverSession);

    LOGGER.debug(
        "Added ServerSession for user {} to userSessionMap - New map: {}", userId, userSessionMap);
  }

  protected void queuePersistedMessages(
      final ServerSession serverSession, List<Map<String, Object>> messages, final String topic) {
    for (Map<String, Object> notification : messages) {
      final Map<String, Object> propMap = new HashMap<>();
      propMap.putAll(notification);

      executorService.submit(
          new Runnable() {
            @Override
            public void run() {
              int maxAttempts = 10;
              int attempts = 0;
              while (!serverSession.isConnected() && attempts < maxAttempts) {
                try {
                  TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
                attempts++;
                LOGGER.trace(
                    "Attempt {} of {} to send notifications back to client.",
                    attempts,
                    maxAttempts);
              }

              LOGGER.trace("Sending notifications back to client.");
              serverSession.deliver(controllerServerSession, topic, propMap);
            }
          });
    }
  }

  protected String getUserId(ServerSession serverSession, Subject subject) {
    String userId = null;
    if (subject != null && subject.getPrincipals() != null) {
      PrincipalCollection principalCollection = subject.getPrincipals();
      for (Object principal : principalCollection.asList()) {
        if (principal instanceof SecurityAssertion) {
          SecurityAssertion assertion = (SecurityAssertion) principal;

          Principal jPrincipal = assertion.getPrincipal();
          userId = jPrincipal.getName();
          break;
        }
      }
      if (null == userId) {
        userId = serverSession.getId();
      }
    } else {
      userId = serverSession.getId();
    }
    return userId;
  }

  /**
   * Obtains the root topic of the controller that should be used when registering the eventhandler.
   *
   * @return String representation of a root topic.
   */
  public abstract String getControllerRootTopic();
}
