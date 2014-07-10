/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.ui.searchui.query.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.codice.ddf.notifications.Notification;
import org.codice.ddf.persistence.PersistentStore;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Test cases for {@link NotificationController}
 */
public class NotificationControllerTest {
    private NotificationController notificationController;

    // NOTE: The ServerSession ID == The ClientSession ID
    private static final String MOCK_SESSION_ID = "1234-5678-9012-3456";

    private ServerSession mockServerSession = mock(ServerSession.class);

    private ServerMessage mockServerMessage = mock(ServerMessage.class);

    private HashMap<String, Object> testEventProperties;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        notificationController = new NotificationController(mock(PersistentStore.class),
                mock(BundleContext.class), mock(EventAdmin.class));

        when(mockServerSession.getId()).thenReturn(MOCK_SESSION_ID);

        testEventProperties = new HashMap<String, Object>();
        testEventProperties.put(Notification.NOTIFICATION_KEY_APPLICATION, "Downloads");
        testEventProperties.put(Notification.NOTIFICATION_KEY_TITLE,
                "SUCCESS - Download Complete - JPEG " + "of San Francisco");
        testEventProperties.put(Notification.NOTIFICATION_KEY_MESSAGE,
                "The download of the 1024 byte JPEG "
                        + "of San Francisco that you requested has completed with a "
                        + "status of: SUCCESS");
        testEventProperties.put(Notification.NOTIFICATION_KEY_TIMESTAMP, new Date().getTime());
        testEventProperties
                .put(Notification.NOTIFICATION_KEY_USER_ID, UUID.randomUUID().toString());

        // TODO: which of these need to be added to Notifications class?
        testEventProperties.put("status", "SUCCESS");
        testEventProperties.put("bytes", 1024);
        testEventProperties.put("option", "test");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link NotificationController#registerUserSession(ServerSession, ServerMessage)}.
     * 
     * Verifies that method throws {@code NullPointerException} when ServerSession is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserSessionWithNullServerSessionThrowsException() {
        // Test null ServerSession
        notificationController.registerUserSession(null, mockServerMessage);
    }

    /**
     * Test method for
     * {@link NotificationController#registerUserSession(ServerSession, ServerMessage)}.
     * 
     * Verifies that method throws {@code NullPointerException} when ServerSession ID is null.
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
                + "referece to the ServerSession", MOCK_SESSION_ID, notificationController
                .getSessionByUserId(MOCK_SESSION_ID).getId());
    }

    /**
     * Test method for {@link NotificationController#getServerSessionByUserId(java.util.String)}
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
                + ServerSession.class.getName() + " object", serverSessionId,
                mockServerSession.getId());
    }

    /**
     * Test method for
     * {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * 
     * Verifies that {@code NullPointerException} is thrown when {@code ServerSession} is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeregisterUserSessionWithNullServerSessonThrowsException() {
        notificationController.deregisterUserSession(null, mockServerMessage);
    }

    /**
     * Test method for
     * {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * 
     * Verifies that {@code NullPointerException} is thrown when {@code ServerSession} ID is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeregisterUserSessionWithNullServerSessionIdThrowsException() {
        when(mockServerSession.getId()).thenReturn(null);
        notificationController.deregisterUserSession(mockServerSession, mockServerMessage);
    }

    /**
     * Test method for
     * {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * 
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
     * 
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
     * 
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
     * 
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
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_USER_ID} property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyUser() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_USER_ID, "");
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * 
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
     * 
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
     * 
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
     * 
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
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when {@code Event}'s
     * {@link Notification#NOTIFICATION_KEY_USER_ID} property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullUser() {
        testEventProperties.put(Notification.NOTIFICATION_KEY_USER_ID, null);
        notificationController.handleEvent(new Event(Notification.NOTIFICATION_TOPIC_BROADCAST,
                testEventProperties));
    }

    /**
     * Test method for deletePersistentNotification(ServerSession serverSession, ServerMessage
     * serverMessage)
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when serverSession is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeletePersistentNotificationThrowsIllegalArgumentOnNullServerSession() {
        notificationController.deletePersistentNotification(null, mockServerMessage);
    }

    /**
     * Test method for deletePersistentNotification(ServerSession serverSession, ServerMessage
     * serverMessage)
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when serverMessage is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeletePersistentNotificationThrowsIllegalArgumentOnNullUserId() {
        notificationController.deletePersistentNotification(mockServerSession, null);
    }
}
