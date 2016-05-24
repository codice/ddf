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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.persistence.PersistentStore;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.common.HashMapMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Test cases for {@link ActivityController}
 */
public class ActivityControllerTest {
    // NOTE: The ServerSession ID == The ClientSession ID
    private static final String MOCK_SESSION_ID = "1234-5678-9012-3456";

    private static final String MOCK_INVALID_SESSION_ID = "0987-6543-2109-8765";

    private static final String MOCK_ACTIVITY_ID = "12345";

    private static final String MOCK_TITLE = "Download Complete of San Francisco";

    private static final String MOCK_MESSAGE = "The download of the 1024 byte JPEG of San "
            + "Francisco that you requested has completed with a status of: SUCCESS";

    private static final long MOCK_TIMESTAMP = new Date().getTime();

    private static final String MOCK_USER_ID = "validUserId";

    private static final String MOCK_INVALID_USER_ID = "invalidUserId";

    private static final String MOCK_PROGRESS = "55%";

    private static final String MOCK_STATUS = "RUNNING";

    private static final String EXPECTED_COMETD_ACTIVITIES_CHANNEL_PREFIX = "/ddf/activities/";

    private ActivityController activityController;

    private ServerSession mockServerSession = mock(ServerSession.class);

    private ServerMessage mockServerMessage = mock(ServerMessage.class);

    private HashMap<String, Object> testEventProperties;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        activityController = new ActivityController(mock(PersistentStore.class),
                mock(BundleContext.class),
                mock(EventAdmin.class));

        when(mockServerSession.getId()).thenReturn(MOCK_SESSION_ID);
        when(mockServerSession.getAttribute("session")).thenReturn(MOCK_SESSION_ID);


        testEventProperties = new HashMap<>();
        testEventProperties.put(ActivityEvent.ID_KEY, MOCK_ACTIVITY_ID);
        testEventProperties.put(ActivityEvent.TITLE_KEY, MOCK_TITLE);
        testEventProperties.put(ActivityEvent.MESSAGE_KEY, MOCK_MESSAGE);
        testEventProperties.put(ActivityEvent.TIMESTAMP_KEY, MOCK_TIMESTAMP);
        testEventProperties.put(ActivityEvent.USER_ID_KEY, MOCK_USER_ID);
        testEventProperties.put(ActivityEvent.PROGRESS_KEY, MOCK_PROGRESS);
        testEventProperties.put(ActivityEvent.STATUS_KEY, MOCK_STATUS);
    }

    /**
     * Test method for {@link ActivityController#registerUserSession(ServerSession, ServerMessage)}.
     *
     * Verifies that method throws {@code IllegalArgumentException} when
     * ServerSession is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserSessionWithNullServerSessionThrowsException() {
        // Test null ServerSession
        activityController.registerUserSession(null, mockServerMessage);
    }

    /**
     * Test method for {@link ActivityController#registerUserSession(ServerSession, ServerMessage)}.
     *
     * Verifies that method throws {@code IllegalArgumentException} when
     * ServerSession ID is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserSessionWithNullServerSessionIdThrowsException() {
        // Test null ServerSession ID
        when(mockServerSession.getId()).thenReturn(null);
        activityController.registerUserSession(mockServerSession, mockServerMessage);
    }

    /**
     * Test method for {@link ActivityController#registerUserSession(ServerSession, ServerMessage)}.
     */
    @Test
    public void testRegisterUserSession() {
        // Test traditional handshake
        activityController.registerUserSession(mockServerSession, mockServerMessage);
        assertEquals(ActivityController.class.getName()
                        + " did not return correctly store a user-id-based "
                        + "referece to the ServerSession",
                MOCK_SESSION_ID,
                activityController.getSessionByUserId(MOCK_SESSION_ID)
                        .getId());
    }

    /**
     * Test method for {@link ActivityController#getSessionByUserId(java.lang.String)}
     */
    @Test
    public void testGetServerSessionByUserId() {
        activityController.userSessionMap.put(MOCK_SESSION_ID, mockServerSession);

        ServerSession serverSession = activityController.getSessionByUserId(MOCK_SESSION_ID);
        assertNotNull(ActivityController.class.getName() + " returned null for user ID: "
                + MOCK_SESSION_ID, serverSession);

        String serverSessionId = serverSession.getId();
        assertNotNull("ServerSession ID is null", serverSessionId);

        assertEquals(ActivityController.class.getName() + " did not return the expected " +
                        ServerSession.class.getName() + " object",
                serverSessionId,
                mockServerSession.getId());
    }

    /**
     * Test method for {@link ActivityController#deregisterUserSession(ServerSession, ServerMessage)}
     *
     * Verifies that {@code IllegalArgumentException} is thrown when
     * {@code ServerSession} is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeregisterUserSessionWithNullServerSessionThrowsException() {
        activityController.deregisterUserSession(null, mockServerMessage);
    }

    /**
     * Test method for {@link ActivityController#deregisterUserSession(ServerSession, ServerMessage)}
     *
     * Verifies that {@code IllegalArgumentException} is thrown when
     * {@code ServerSession} ID is null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeregisterUserSessionWithNullServerSessionIdThrowsException() {
        when(mockServerSession.getId()).thenReturn(null);
        activityController.deregisterUserSession(mockServerSession, mockServerMessage);
    }

    /**
     * Test method for {@link ActivityController#deregisterUserSession(ServerSession, ServerMessage)}
     *
     * Verifies that a the method removes the client's user from the
     * {@code NotificationController}'s known clients.
     */
    @Test
    public void testDeregisterUserSessionRemovesUserFromKnownClients() {
        assertNull(activityController.getSessionByUserId(MOCK_SESSION_ID));
        activityController.registerUserSession(mockServerSession, mockServerMessage);
        assertNotNull(activityController.getSessionByUserId(MOCK_SESSION_ID));
        activityController.deregisterUserSession(mockServerSession, mockServerMessage);
        assertNull(activityController.getSessionByUserId(MOCK_SESSION_ID));
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
        testEventProperties.put(ActivityEvent.ID_KEY, "");
        activityController.handleEvent(new Event(ActivityEvent.EVENT_TOPIC, testEventProperties));
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
        testEventProperties.put(ActivityEvent.MESSAGE_KEY, "");
        activityController.handleEvent(new Event(ActivityEvent.EVENT_TOPIC, testEventProperties));
    }

    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     *
     * Verifies that {@code IllegalArgumentException} is thrown when
     * {@code Event}'s {@link ActivityEvent#USER_ID_KEY} and
     * {@link ActivityEvent#SESSION_ID_KEY} properties are empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnEmptyUserAndSessionId() {
        testEventProperties.put(ActivityEvent.USER_ID_KEY, "");
        testEventProperties.put(ActivityEvent.SESSION_ID_KEY, "");
        activityController.handleEvent(new Event(ActivityEvent.EVENT_TOPIC, testEventProperties));
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
        testEventProperties.put(ActivityEvent.ID_KEY, null);
        activityController.handleEvent(new Event(ActivityEvent.EVENT_TOPIC, testEventProperties));
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
        testEventProperties.put(ActivityEvent.MESSAGE_KEY, null);
        activityController.handleEvent(new Event(ActivityEvent.EVENT_TOPIC, testEventProperties));
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
        testEventProperties.put(ActivityEvent.TIMESTAMP_KEY, null);
        activityController.handleEvent(new Event(ActivityEvent.EVENT_TOPIC, testEventProperties));
    }

    /**
     * Test method for {@link ActivityController#handleEvent(org.osgi.service.event.Event)}
     *
     * Verifies that {@code IllegalArgumentException} is thrown when
     * {@code Event}'s {@link ActivityEvent#USER_ID_KEY} and
     * {@link ActivityEvent#SESSION_ID_KEY} properties are null.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandleEventThrowsIllegalArgumentExceptionOnNullUserAndSessionId() {
        testEventProperties.put(ActivityEvent.USER_ID_KEY, null);
        testEventProperties.put(ActivityEvent.SESSION_ID_KEY, null);
        activityController.handleEvent(new Event(ActivityEvent.EVENT_TOPIC, testEventProperties));
    }


    /**
     * Verifies that the session lookup works properly if the userId is provided
     */
    @Test
    public void testLookupSessionViaUserIdIfPresent() {
        activityController.userSessionMap.put(MOCK_USER_ID, mockServerSession);
        // check that a session is returned with no provided sessionId
        assertThat(activityController.getSessionById(MOCK_USER_ID, null), is(mockServerSession));
        // checks that the sessionId is not used if userId is present
        assertThat(activityController.getSessionById(MOCK_USER_ID, MOCK_INVALID_SESSION_ID),
                is(mockServerSession));
    }

    /**
     * Verifies that the session lookup properly falls back to sessionId lookup if
     * the userId is not provided or is invalid
     */
    @Test
    public void testFallbackToSessionIdLookupIfUserIdFails() {
        activityController.userSessionMap.put(MOCK_USER_ID, mockServerSession);
        // check the sessionId is used if the userId cannot be found
        assertThat(activityController.getSessionById(MOCK_INVALID_USER_ID, MOCK_SESSION_ID), is(mockServerSession));
        // checks that the sessionId is used if the userID is not provided
        assertThat(activityController.getSessionById("", MOCK_SESSION_ID), is(mockServerSession));
    }

    /**
     * Verifies that sessions are only returned if an identifier is supplied
     */
    @Test
    public void testSessionLookupsWithoutIdentifiersFail() {
        activityController.userSessionMap.put(MOCK_USER_ID, mockServerSession);
        // check that nothing is returned if nothing is given
        assertThat(activityController.getSessionById(null, null), is(nullValue()));
    }

    private void verifyGetPersistedActivitiesWithMessageData(Map<String, Object> messageData,
            boolean expectPublish) {
        Message message = new HashMapMessage();

        message.put(Message.DATA_FIELD, messageData);

        List<Map<String, Object>> activities = new ArrayList<>();
        activities.add(testEventProperties);

        ActivityController spyActivityController = spy(activityController);

        doReturn(activities).when(spyActivityController)
                .getActivitiesForUser(anyString());
        // Don't want queuePersistedMessages to start up a new thread.
        doNothing().when(spyActivityController)
                .queuePersistedMessages(any(ServerSession.class),
                        Matchers.<List<Map<String, Object>>>any(),
                        anyString());

        spyActivityController.getPersistedActivities(mockServerSession, message);

        if (expectPublish) {
            verify(spyActivityController, times(1)).queuePersistedMessages(eq(mockServerSession),
                    eq(activities),
                    startsWith(EXPECTED_COMETD_ACTIVITIES_CHANNEL_PREFIX));
        } else {
            verify(spyActivityController, never()).queuePersistedMessages(any(ServerSession.class),
                    Matchers.<List<Map<String, Object>>>any(),
                    anyString());
        }
    }

    @Test
    public void testGetPersistedActivitiesWithNullData() {
        verifyGetPersistedActivitiesWithMessageData(null, true);
    }

    @Test
    public void testGetPersistedActivitiesWithEmptyData() {
        verifyGetPersistedActivitiesWithMessageData(new HashMap<>(), true);
    }

    @Test
    public void testGetPersistedActivitiesWithData() {
        verifyGetPersistedActivitiesWithMessageData(testEventProperties, false);
    }

    @Test
    public void testHandleEventDeliver() {
        ActivityController spyActivityController = spy(activityController);

        spyActivityController.controllerServerSession = mock(ServerSession.class);

        testEventProperties.put(ActivityEvent.SESSION_ID_KEY, MOCK_SESSION_ID);

        doReturn(mockServerSession).when(spyActivityController)
                .getSessionByUserId(MOCK_USER_ID);

        spyActivityController.handleEvent(new Event(ActivityEvent.EVENT_TOPIC,
                testEventProperties));

        ArgumentCaptor<Object> dataArgumentCaptor = ArgumentCaptor.forClass(Object.class);

        verify(mockServerSession,
                times(1)).deliver(eq(spyActivityController.controllerServerSession),
                startsWith(EXPECTED_COMETD_ACTIVITIES_CHANNEL_PREFIX),
                dataArgumentCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) dataArgumentCaptor.getValue();

        assertThat(data.get(ActivityEvent.ID_KEY), is(MOCK_ACTIVITY_ID));
        assertThat(data.get(ActivityEvent.SESSION_ID_KEY), is(MOCK_SESSION_ID));
        assertThat(data.get(ActivityEvent.TITLE_KEY), is(MOCK_TITLE));
        assertThat(data.get(ActivityEvent.MESSAGE_KEY), is(MOCK_MESSAGE));
        assertThat(data.get(ActivityEvent.TIMESTAMP_KEY), is(MOCK_TIMESTAMP));
        assertThat(data.get(ActivityEvent.PROGRESS_KEY), is(MOCK_PROGRESS));
        assertThat(data.get(ActivityEvent.STATUS_KEY), is(MOCK_STATUS));
    }
}
