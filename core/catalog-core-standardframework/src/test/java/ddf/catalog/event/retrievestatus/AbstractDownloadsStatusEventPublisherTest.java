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
package ddf.catalog.event.retrievestatus;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Map;

import org.codice.ddf.notifications.Notification;
import org.junit.BeforeClass;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher.ProductRetrievalStatus;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;

/**
 * Abstract class used to test the DownloadsStatusEventPublisher. <br/>
 * <br/>
 * This class contains test cases to verify that the event admin service is
 * properly called after a download event is triggered. Given the different
 * types of events, implementations of this test should set up the publisher to
 * match their specific event type.
 * 
 * 
 */
public abstract class AbstractDownloadsStatusEventPublisherTest {

    protected static EventAdmin eventAdmin;

    protected static ActionProvider actionProvider;

    protected static Metacard metacard;

    protected static ResourceResponse resourceResponse;

    protected static ResourceRequest resourceRequest;

    protected static Resource resource;

    protected static Map<String, Serializable> properties;

    protected static DownloadsStatusEventPublisher publisher;

    protected static final Logger LOGGER = org.slf4j.LoggerFactory
            .getLogger(AbstractDownloadsStatusEventPublisherTest.class);

    @BeforeClass
    public static void oneTimeSetup() {
        resourceResponse = mock(ResourceResponse.class);
        resourceRequest = mock(ResourceRequest.class);
        resource = mock(Resource.class);
        properties = mock(Map.class);

        when(resource.getName()).thenReturn("testCometDSessionID");
        when(properties.get(Notification.NOTIFICATION_KEY_USER_ID)).thenReturn("testUser");
        when(resourceRequest.getProperties()).thenReturn(properties);
        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);
    }

    @org.junit.Test
    public void testPostRetrievalStatusHappyPath() {
        setupPublisher();

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.STARTED, metacard,
                null, 0L);
        verify(eventAdmin, times(1)).postEvent(any(Event.class));

        // Test with null bytes
        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.STARTED, metacard,
                null, null);
        verify(eventAdmin, times(2)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.CANCELLED, metacard,
                "test detail", 20L);
        verify(eventAdmin, times(3)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.FAILED, metacard,
                "test detail", 250L);
        verify(eventAdmin, times(4)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.RETRYING, metacard,
                "test detail", 350L);
        verify(eventAdmin, times(5)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.COMPLETE, metacard,
                "test detail", 500L);
        verify(eventAdmin, times(6)).postEvent(any(Event.class));
    }

    @org.junit.Test
    public void testPostRetrievalStatusWithNoNameProperty() {
        setupPublisher();

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.STARTED, metacard,
                null, 0L);
        verify(eventAdmin, times(1)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.STARTED, metacard,
                "test detail", 10L);
        verify(eventAdmin, times(2)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.CANCELLED, metacard,
                "test detail", 20L);
        verify(eventAdmin, times(3)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.FAILED, metacard,
                "test detail", 250L);
        verify(eventAdmin, times(4)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.RETRYING, metacard,
                "test detail", 350L);
        verify(eventAdmin, times(5)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.COMPLETE, metacard,
                "test detail", 500L);
        verify(eventAdmin, times(6)).postEvent(any(Event.class));
    }

    @org.junit.Test
    public void testPostRetrievalWithNoStatus() {
        setupPublisherWithNoNotifications();

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.STARTED, metacard,
                null, 0L);
        verify(eventAdmin, times(0)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.STARTED, metacard,
                "test detail", 10L);
        verify(eventAdmin, times(0)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.CANCELLED, metacard,
                "test detail", 20L);
        verify(eventAdmin, times(0)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.FAILED, metacard,
                "test detail", 250L);
        verify(eventAdmin, times(0)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.RETRYING, metacard,
                "test detail", 350L);
        verify(eventAdmin, times(0)).postEvent(any(Event.class));

        publisher.postRetrievalStatus(resourceResponse, ProductRetrievalStatus.COMPLETE, metacard,
                "test detail", 500L);
        verify(eventAdmin, times(0)).postEvent(any(Event.class));
    }

    protected abstract void setupPublisher();

    private void setupPublisherWithNoNotifications() {
        eventAdmin = mock(EventAdmin.class);
        publisher = new DownloadsStatusEventPublisher(eventAdmin, ImmutableList.of(actionProvider));
        publisher.setNotificationEnabled(false);
        publisher.setActivityEnabled(false);
    }
}
