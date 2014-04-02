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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;

/**
 * Test cases for {@link NotificationController} 
 */
public class NotificationControllerTest {
    private NotificationController notificationController;
    
    // NOTE: The ServerSession ID == The ClientSession ID
    private static final String mockSessionId = "1234-5678-9012-3456";
    
    private ServerSession mockServerSession = mock(ServerSession.class);
    private ServerMessage mockServerMessage = mock(ServerMessage.class);
 
    private HashMap<String, Object> testEventProperties;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        notificationController = new NotificationController(mock(BundleContext.class));
        
        when(mockServerSession.getId()).thenReturn(mockSessionId);
        
        testEventProperties = new HashMap<String, Object>();
        testEventProperties.put("application", "Downloads");
        testEventProperties.put("title", "SUCCESS - Download Complete - JPEG "
                + "of San Francisco");
        testEventProperties.put("message", "The download of the 1024 byte JPEG "
                + "of San Francisco that you requested has completed with a "
                + "status of: SUCCESS");
        testEventProperties.put("timestamp", new Date().getTime());
        testEventProperties.put("user", UUID.randomUUID().toString());
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
     * Test method for {@link NotificationController#registerUserSession(ServerSession, ServerMessage)}.
     * Verifies that method throws {@code NullPointerException} when ServerSession is  
     * null.
     */
    @Test(expected = NullPointerException.class)
    public void testRegisterUserSessionWithNullServerSessionThrowsException() {    
        // Test null ServerSession
        notificationController.registerUserSession(null, mockServerMessage);  
    }
    
    /**
     * Test method for {@link NotificationController#registerUserSession(ServerSession, ServerMessage)}.
     * Verifies that method throws {@code NullPointerException} when ServerSession ID  
     * is null.
     */
    @Test(expected = NullPointerException.class)
    public void testRegisterUserSessionWithNullServerSessionIdThrowsException() {    
        // Test null ServerSession ID
        when(mockServerSession.getId()).thenReturn(null);
        notificationController.registerUserSession(mockServerSession, mockServerMessage);
        
    }
    
    /**
     * Test method for {@link NotificationController#registerUserSession(ServerSession, ServerMessage)}.
     */
    @Test
    public void testRegisterUserSession() {    
        // Test traditional handshake
        notificationController.registerUserSession(mockServerSession, mockServerMessage);
        assertEquals(NotificationController.class.getName() + " did not return correctly store a user-id-based "
                + "referece to the ServerSession", mockSessionId, 
                notificationController.getSessionByUserId(mockSessionId).getId()); 
    }
    
    /**
     * Test method for {@link NotificationController#getServerSessionByUserId(java.util.String)}
     */
    @Test
    public void testGetServerSessionByUserId() {
        notificationController.userSessionMap.put(mockSessionId, mockServerSession);
        
        ServerSession serverSession = notificationController.getSessionByUserId(mockSessionId);
        assertNotNull(NotificationController.class.getName() + " returned null for user ID: " + mockSessionId, serverSession);
        
        String serverSessionId = serverSession.getId();
        assertNotNull("ServerSession ID is null", serverSessionId);
        
        assertEquals(NotificationController.class.getName() + " did not return the expected " + 
                     ServerSession.class.getName() + " object", serverSessionId, mockServerSession.getId());
    }
    
    /**
     * Test method for {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * Verifies that {@code NullPointerException} is thrown when 
     * {@code ServerSession} is null.
     */
    @Test(expected = NullPointerException.class)
    public void testDeregisterUserSessionWithNullServerSessonThrowsException() {
        notificationController.deregisterUserSession(null, mockServerMessage);
    }
    
    /**
     * Test method for {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * Verifies that {@code NullPointerException} is thrown when 
     * {@code ServerSession} ID is null.
     */
    @Test(expected = NullPointerException.class) 
    public void testDeregisterUserSessionWithNullServerSessionIdThrowsException() {
        when(mockServerSession.getId()).thenReturn(null);
        notificationController.deregisterUserSession(mockServerSession, mockServerMessage);
    }
    
    /**
     * Test method for {@link NotificationController#deregisterUserSession(ServerSession, ServerMessage)}
     * Verifies that a the method removes the client's user from the 
     * {@code NotificationController}'s known clients.
     */
    @Test
    public void testDeregisterUserSessionRemovesUserFromKnownClients() {
        assertNull(notificationController.getSessionByUserId(mockSessionId));
        notificationController.registerUserSession(mockServerSession, mockServerMessage);
        assertNotNull(notificationController.getSessionByUserId(mockSessionId));
        notificationController.deregisterUserSession(mockServerSession, mockServerMessage);
        assertNull(notificationController.getSessionByUserId(mockSessionId));
    }
    
    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s application property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyApplication() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_APPLICATION_KEY, "");
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s message property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyMessage() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_MESSAGE_KEY, "");
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s title property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyTitle() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_TITLE_KEY, "");
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }
    
    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s user property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyUser() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_USER_KEY, "");
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s application property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullApplication() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_APPLICATION_KEY, null);
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s message property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullMessage() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_MESSAGE_KEY, null);
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s timestamp property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullTimestamp() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_TIMESTAMP_KEY, null);
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }

    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s title property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullTitle() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_TITLE_KEY, null);
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }
    
    /**
     * Test method for {@link NotificationController#handleEvent(org.osgi.service.event.Event)}
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s title property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullUser() {
        testEventProperties.put(
                NotificationController.NOTIFICATION_USER_KEY, null);
        notificationController.handleEvent(new Event(
                NotificationController.NOTIFICATIONS_TOPIC_NAME, 
                testEventProperties));
    }
}
