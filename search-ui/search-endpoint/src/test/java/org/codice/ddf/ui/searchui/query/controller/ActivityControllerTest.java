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

import org.codice.ddf.activities.ActivityEvent;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;

/**
 * Test cases for {@link ActivityController} 
 */
public class ActivityControllerTest {
    private ActivityController acitivityController;
    
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
        acitivityController = new ActivityController(mock(BundleContext.class));
        
        when(mockServerSession.getId()).thenReturn(MOCK_SESSION_ID);
        
        testEventProperties = new HashMap<String, Object>();
        testEventProperties.put(ActivityEvent.ID_KEY, "12345");
        testEventProperties.put(ActivityEvent.TITLE_KEY, "Download Complete"
                + "of San Francisco");
        testEventProperties.put(ActivityEvent.MESSAGE_KEY, "The download of the 1024 byte JPEG "
                + "of San Francisco that you requested has completed with a "
                + "status of: SUCCESS");
        testEventProperties.put(ActivityEvent.TIMESTAMP_KEY, new Date().getTime());
        testEventProperties.put(ActivityEvent.USER_ID_KEY, UUID.randomUUID().toString());
        
        testEventProperties.put(ActivityEvent.PROGRESS_KEY, "55%");
        testEventProperties.put(ActivityEvent.STATUS_KEY, "RUNNING");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }
    
    /**
     * Test method for {@link ActivityController#registerUserSession(ServerSession, ServerMessage)}.
     * 
     * Verifies that method throws {@code NullPointerException} when 
     * ServerSession is null.
     */
    @Test(expected = NullPointerException.class)
    public void testRegisterUserSessionWithNullServerSessionThrowsException() {    
        // Test null ServerSession
        acitivityController.registerUserSession(null, mockServerMessage);  
    }
    
    /**
     * Test method for {@link ActivityController#registerUserSession(ServerSession, ServerMessage)}.
     * 
     * Verifies that method throws {@code NullPointerException} when 
     * ServerSession ID is null.
     */
    @Test(expected = NullPointerException.class)
    public void testRegisterUserSessionWithNullServerSessionIdThrowsException() {    
        // Test null ServerSession ID
        when(mockServerSession.getId()).thenReturn(null);
        acitivityController.registerUserSession(mockServerSession, mockServerMessage);
        
    }
    
    /**
     * Test method for {@link ActivityController#registerUserSession(ServerSession, ServerMessage)}.
     */
    @Test
    public void testRegisterUserSession() {    
        // Test traditional handshake
        acitivityController.registerUserSession(mockServerSession, mockServerMessage);
        assertEquals(ActivityController.class.getName() + " did not return correctly store a user-id-based "
                + "referece to the ServerSession", MOCK_SESSION_ID, 
                acitivityController.getSessionByUserId(MOCK_SESSION_ID).getId()); 
    }
    
    /**
     * Test method for {@link ActivityController#getServerSessionByUserId(java.util.String)}
     */
    @Test
    public void testGetServerSessionByUserId() {
        acitivityController.userSessionMap.put(MOCK_SESSION_ID, mockServerSession);
        
        ServerSession serverSession = acitivityController.getSessionByUserId(MOCK_SESSION_ID);
        assertNotNull(ActivityController.class.getName() + " returned null for user ID: " + MOCK_SESSION_ID, serverSession);
        
        String serverSessionId = serverSession.getId();
        assertNotNull("ServerSession ID is null", serverSessionId);
        
        assertEquals(ActivityController.class.getName() + " did not return the expected " + 
                     ServerSession.class.getName() + " object", serverSessionId, mockServerSession.getId());
    }
    
    /**
     * Test method for {@link ActivityController#deregisterUserSession(ServerSession, ServerMessage)}
     * 
     * Verifies that {@code NullPointerException} is thrown when 
     * {@code ServerSession} is null.
     */
    @Test(expected = NullPointerException.class)
    public void testDeregisterUserSessionWithNullServerSessonThrowsException() {
        acitivityController.deregisterUserSession(null, mockServerMessage);
    }
    
    /**
     * Test method for {@link ActivityController#deregisterUserSession(ServerSession, ServerMessage)}
     * 
     * Verifies that {@code NullPointerException} is thrown when 
     * {@code ServerSession} ID is null.
     */
    @Test(expected = NullPointerException.class) 
    public void testDeregisterUserSessionWithNullServerSessionIdThrowsException() {
        when(mockServerSession.getId()).thenReturn(null);
        acitivityController.deregisterUserSession(mockServerSession, mockServerMessage);
    }
    
    /**
     * Test method for {@link ActivityController#deregisterUserSession(ServerSession, ServerMessage)}
     * 
     * Verifies that a the method removes the client's user from the 
     * {@code NotificationController}'s known clients.
     */
    @Test
    public void testDeregisterUserSessionRemovesUserFromKnownClients() {
        assertNull(acitivityController.getSessionByUserId(MOCK_SESSION_ID));
        acitivityController.registerUserSession(mockServerSession, mockServerMessage);
        assertNotNull(acitivityController.getSessionByUserId(MOCK_SESSION_ID));
        acitivityController.deregisterUserSession(mockServerSession, mockServerMessage);
        assertNull(acitivityController.getSessionByUserId(MOCK_SESSION_ID));
    }
    
    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s {@link ActivityEvent#ID_KEY} 
     * property is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyApplication() {
        testEventProperties.put(
                ActivityEvent.ID_KEY, "");
        acitivityController.handleEvent(new Event(
                ActivityEvent.EVENT_TOPIC_BROADCAST, 
                testEventProperties));
    }

    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s {@link ActivityEvent#MESSAGE_KEY} property is
     * empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyMessage() {
        testEventProperties.put(
                ActivityEvent.MESSAGE_KEY, "");
        acitivityController.handleEvent(new Event(
                ActivityEvent.EVENT_TOPIC_BROADCAST, 
                testEventProperties));
    }
    
    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s {@link ActivityEvent#USER_ID_KEY} property is
     * empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyUser() {
        testEventProperties.put(
                ActivityEvent.USER_ID_KEY, "");
        acitivityController.handleEvent(new Event(
                ActivityEvent.EVENT_TOPIC_BROADCAST, 
                testEventProperties));
    }

    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s {@link ActivityEvent#ID_KEY}
     * property is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullApplication() {
        testEventProperties.put(
                ActivityEvent.ID_KEY, null);
        acitivityController.handleEvent(new Event(
                ActivityEvent.EVENT_TOPIC_BROADCAST, 
                testEventProperties));
    }

    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s {@link ActivityEvent#MESSAGE_KEY} property is
     * null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullMessage() {
        testEventProperties.put(
                ActivityEvent.MESSAGE_KEY, null);
        acitivityController.handleEvent(new Event(
                ActivityEvent.EVENT_TOPIC_BROADCAST, 
                testEventProperties));
    }

    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s {@link ActivityEvent#TIMESTAMP_KEY} property
     * is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullTimestamp() {
        testEventProperties.put(
                ActivityEvent.TIMESTAMP_KEY, null);
        acitivityController.handleEvent(new Event(
                ActivityEvent.EVENT_TOPIC_BROADCAST, 
                testEventProperties));
    }
    
    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     * 
     * Verifies that {@code IllegalArgumentException} is thrown when 
     * {@code Event}'s {@link AcitivityEvent#USER_ID_KEY} property is
     * null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullUser() {
        testEventProperties.put(
                ActivityEvent.USER_ID_KEY, null);
        acitivityController.handleEvent(new Event(
                ActivityEvent.EVENT_TOPIC_BROADCAST, 
                testEventProperties));
    }
}
