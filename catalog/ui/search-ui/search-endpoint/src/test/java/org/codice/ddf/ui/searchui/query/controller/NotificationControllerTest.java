/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.ui.searchui.query.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.notifications.Notification;
import org.codice.ddf.persistence.PersistentStore;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.common.HashMapMessage;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Test cases for {@link NotificationController}
 */
public class NotificationControllerTest {
    // NOTE: The ServerSession ID == The ClientSession ID
    private static final String MOCK_SESSION_ID = "1234-5678-9012-3456";

    private static final String MOCK_INVALID_SESSION_ID = "0987-6543-2109-8765";

    private static final String MOCK_APPLICATION = "Downloads";

    private static final String MOCK_TITLE = "SUCCESS - Download Complete - JPEG of San Francisco";

    private static final String MOCK_MESSAGE = "The download of the 1024 byte JPEG of San "
            + "Francisco that you requested has completed with a status of: SUCCESS";

    private static final long MOCK_TIMESTAMP = new Date().getTime();

    private static final String MOCK_USER_ID = "validUserId";

    private static final String MOCK_INVALID_USER_ID = "invalidUserId";

    private static final String EXPECTED_COMETD_NOTIFICATIONS_CHANNEL_PREFIX =
            "/ddf/notifications/";

    private NotificationController notificationController;

    private ServerSession mockServerSession = mock(ServerSession.class);

    private ServerMessage mockServerMessage = mock(ServerMessage.class);

    private HashMap<String, Object> testEventProperties;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        notificationController = new NotificationController(mock(PersistentStore.class),
                mock(BundleContext.class),
                mock(EventAdmin.class));

        when(mockServerSession.getId()).thenReturn(MOCK_SESSION_ID);
        when(mockServerSession.getAttribute("session")).thenReturn(MOCK_SESSION_ID);

        testEventProperties = new HashMap<>();
        testEventProperties.put(Notification.NOTIFICATION_KEY_APPLICATION, MOCK_APPLICATION);
        testEventProperties.put(Notification.NOTIFICATION_KEY_TITLE, MOCK_TITLE);
        testEventProperties.put(Notification.NOTIFICATION_KEY_MESSAGE, MOCK_MESSAGE);
        testEventProperties.put(Notification.NOTIFICATION_KEY_TIMESTAMP, MOCK_TIMESTAMP);
        testEventProperties.put(Notification.NOTIFICATION_KEY_USER_ID, MOCK_USER_ID);
    }

    /**
     * Test method for
     * {@link NotificationController#registerUserSession(ServerSession, ServerMessage)}.
     * <p>
     * Verifies that method throws {@code IllegalArgumentException} when ServerSession is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserSessionWithNullServerSessionThrowsException() {
        // Test null ServerSession
        notificationController.registerUserSession(null, mockServerMessage);
    }

    /**
     * Test method for
     * {@link NotificationController#registerUserSession(ServerSession, ServerMessage)}.
     * <p>
     * Verifies that method throws {@code IllegalArgumentException} when ServerSession ID is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserSessionWithNullServerSessionIdThrowsException() {
        // Test null ServerSession ID
        when(mockServerSession.getId()).thenReturn(null);
        notificationController.registerUserSession(mockServerSession, mockServerMessage);

    }

    /**
     * Test method for
     * {@link NotificationController#registerUserSession(ServerSession, ServerMessage)}.
     */
    @Test
    public void testRegisterUserSession() {
        // Test traditional handshake
        notificationController.registerUserSession(mockServerSession, mockServerMessage);
        assertEquals(NotificationController.class.getName()
                        + " did not return correctly store a user-id-based "
                        + "reference to the ServerSession",
                MOCK_SESSION_ID,
                notificationController.getSessionByUserId(MOCK_SESSION_ID)
                        .getId());
    }

    /**
     * Test method for {@link NotificationController#getSessionByUserId(java.lang.String)}
     */
    @Test
    public void testGetServerSessionByUserId() {
        notificationController.userSessionMap.put(MOCK_SESSION_ID, mockServerSession);

        ServerSession serverSession = notificationController.getSessionByUserId(MOCK_SESSION_ID);
        assertNotNull(NotificationController.class.getName() + " returned null for user ID: "
                + MOCK_SESSION_ID, serverSession);

        String serverSessionId = serverSession.getId();
        assertNotNull("ServerSession ID is null", serverSessionId);

        assertEquals(NotificationController.class.getName() + " did not return the expected "
                        + ServerSession.class.getName() + " object",
                serverSessionId,
                mockServerSession.getId());
    }

    /**
     * Test method for
     * {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code ServerSession} is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeregisterUserSessionWithNullServerSessionThrowsException() {
        notificationController.deregisterUserSession(null, mockServerMessage);
    }

    /**
     * Test method for
     * {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code ServerSession} ID is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeregisterUserSessionWithNullServerSessionIdThrowsException() {
        when(mockServerSession.getId()).thenReturn(null);
        notificationController.deregisterUserSession(mockServerSession, mockServerMessage);
    }

    /**
     * Test method for
     * {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * <p>
     * Verifies that a the method removes the client's user from the {@code NotificationController}
     * 's known clients.
     */
    @Test
    public void testDeregisterUserSessionRemovesUserFromKnownClients() {
        assertNull(notificationController.getSessionByUserId(MOCK_SESSION_ID));
        notificationController.registerUserSession(mockServerSession, mockServerMessage);
        assertNotNull(notificationController.getSessionByUserId(MOCK_SESSION_ID));
        notificationController.deregisterUserSession(mockServerSession, mockServerMessage);
        assertNull(notificationController.getSessionByUserId(MOCK_SESSION_ID));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_APPLICATION} property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyApplication() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_APPLICATION, "");
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_MESSAGE} property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyMessage() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_MESSAGE, "");
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_TITLE property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyTitle() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_TITLE, "");
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_USER_ID} and
     * {@link Notification#NOTIFICATION_KEY_SESSION_ID} properties are null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyUserAndSessionId() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_USER_ID, "");
        testEventProperties.put(Notification.NOTIFICATION_KEY_SESSION_ID, "");
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_APPLICATION} property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullApplication() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_APPLICATION, null);
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_MESSAGE} property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullMessage() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_MESSAGE, null);
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_TIMESTAMP} property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullTimestamp() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_TIMESTAMP, null);
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_TITLE} property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullTitle() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_TITLE, null);
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_USER_ID} and
     * {@link Notification#NOTIFICATION_KEY_SESSION_ID} properties are null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullUserAndSessionId() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_USER_ID, null);
        testEventProperties.put(Notification.NOTIFICATION_KEY_SESSION_ID, null);
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Verifies that the session lookup works properly if the userId is provided
     */
    @Test
    public void testLookupSessionViaUserIdIfPresent() {
        notificationController.userSessionMap.put(MOCK_USER_ID, mockServerSession);
        // checks that a session is returned with no provided sessionId
        assertThat(notificationController.getSessionById(MOCK_USER_ID, null), is(mockServerSession));
        // checks that the sessionId is not used if userId is present
        assertThat(notificationController.getSessionById(MOCK_USER_ID, MOCK_INVALID_SESSION_ID),
                is(mockServerSession));
    }

    /**
     * Verifies that the session lookup properly falls back to sessionId lookup if
     * the userId is not provided or is invalid
     */
    @Test
    public void testFallbackToSessionIdLookupIfUserIdFails() {
        notificationController.userSessionMap.put(MOCK_USER_ID, mockServerSession);
        // checks that the sessionId is used if the userId cannot be found
        assertThat(notificationController.getSessionById(MOCK_INVALID_USER_ID, MOCK_SESSION_ID), is(mockServerSession));
        // checks that the sessionId is used if the userID is not provided
        assertThat(notificationController.getSessionById(null, MOCK_SESSION_ID), is(mockServerSession));
    }

    /**
     * Verifies that sessions are only returned if an identifier is supplied
     */
    @Test
    public void testSessionLookupsWithoutIdentifiersFail() {
        notificationController.userSessionMap.put(MOCK_USER_ID, mockServerSession);
        // checks that nothing is returned if nothing is given
        assertThat(notificationController.getSessionById(null, null), is(nullValue()));
    }



    /**
     * Test method for deletePersistentNotification(ServerSession serverSession, ServerMessage
     * serverMessage)
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when serverSession is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeletePersistentNotificationThrowsIllegalArgumentOnNullServerSession() {
        notificationController.deletePersistentNotification(null, mockServerMessage);
    }

    /**
     * Test method for deletePersistentNotification(ServerSession serverSession, ServerMessage
     * serverMessage)
     * <p>
     * Verifies that {@code IllegalArgumentException} is thrown when serverMessage is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeletePersistentNotificationThrowsIllegalArgumentOnNullUserId() {
        notificationController.deletePersistentNotification(mockServerSession, null);
    }

    private void verifyGetPersistedNotificationsWithMessageData(Map<String, Object> messageData,
            boolean notificationPublished) {
        Message message = new HashMapMessage();
        message.put(Message.DATA_FIELD, messageData);

        List<Map<String, Object>> notifications = new ArrayList<>();
        notifications.add(testEventProperties);

        NotificationController spyNotificationController = spy(notificationController);

        doReturn(notifications).when(spyNotificationController)
                .getNotificationsForUser(anyString());
        // Don't want queuePersistedMessages to start up a new thread.
        doNothing().when(spyNotificationController)
                .queuePersistedMessages(any(ServerSession.class),
                        Matchers.<List<Map<String, Object>>>any(),
                        anyString());

        spyNotificationController.getPersistedNotifications(mockServerSession, message);

        if (notificationPublished) {
            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
            verify(spyNotificationController.eventAdmin,
                    times(1)).postEvent(eventArgumentCaptor.capture());

            Event event = eventArgumentCaptor.getValue();

            assertThat(event.getProperty(Notification.NOTIFICATION_KEY_APPLICATION),
                    is(MOCK_APPLICATION));
            assertThat(event.getProperty(Notification.NOTIFICATION_KEY_MESSAGE), is(MOCK_MESSAGE));
            assertThat(event.getProperty(Notification.NOTIFICATION_KEY_TIMESTAMP),
                    is(ISODateTimeFormat.dateTime()
                            .print(MOCK_TIMESTAMP)));
            assertThat(event.getProperty(Notification.NOTIFICATION_KEY_TITLE), is(MOCK_TITLE));
            assertThat(event.getProperty(Notification.NOTIFICATION_KEY_SESSION_ID),
                    is(MOCK_SESSION_ID));
            assertThat(event.getProperty(Notification.NOTIFICATION_KEY_USER_ID),
                    is(MOCK_SESSION_ID));
        } else {
            verify(spyNotificationController, times(1)).
                    queuePersistedMessages(eq(mockServerSession),
                            eq(notifications),
                            startsWith(EXPECTED_COMETD_NOTIFICATIONS_CHANNEL_PREFIX));
        }
    }

    @Test
    public void testGetPersistedNotificationsWithNullData() {
        verifyGetPersistedNotificationsWithMessageData(null, false);
    }

    @Test
    public void testGetPersistedNotificationsWithEmptyData() {
        verifyGetPersistedNotificationsWithMessageData(new HashMap<>(), false);
    }

    @Test
    public void testGetPersistedNotificationsWithData() {
        verifyGetPersistedNotificationsWithMessageData(testEventProperties, true);
    }
}
