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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.osgi.service.event.EventAdmin;

import com.google.common.collect.ImmutableList;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;

/**
 * 
 * Tests that the activity events are properly published when simulating
 * download events.
 * 
 */
public class ActivityEventPublisherTest extends AbstractDownloadsStatusEventPublisherTest {

    @Override
    protected void setupPublisher() {
        eventAdmin = mock(EventAdmin.class);
        metacard = mock(Metacard.class);
        when(metacard.getId()).thenReturn("12345");
        actionProvider = mock(ActionProvider.class);
        Action downloadAction = mock(Action.class);
        try {
            when(actionProvider.getAction(metacard)).thenReturn(downloadAction);
            when(downloadAction.getUrl()).thenReturn(new URL("http://example.com/download"));
        } catch (Exception e) {
            LOGGER.warn("Could not set download action URL", e);
        }
        publisher = new DownloadsStatusEventPublisher(eventAdmin, ImmutableList.of(actionProvider));
        publisher.setNotificationEnabled(false);
    }

}
