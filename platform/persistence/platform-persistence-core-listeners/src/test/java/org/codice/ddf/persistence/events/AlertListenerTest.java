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
package org.codice.ddf.persistence.events;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.system.alerts.Alert;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.event.Event;

public class AlertListenerTest {

  private PersistentStore persistentStore;

  private AlertListener alertListener;

  @Before
  public void setup() throws Exception {
    persistentStore = mock(PersistentStore.class);

    alertListener = new AlertListener(persistentStore);
    alertListener.init();
  }

  @Test
  public void testHandleEventFirstAlert() throws Exception {
    SystemNotice notice =
        new SystemNotice("test-source", NoticePriority.IMPORTANT, "title", new HashSet());
    Map<String, Object> props = notice.getProperties();

    props.put("event.topics", "decanter/alert/test");
    Alert alert = new Alert(props);

    ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

    Event event = new Event("decanter/alert/test", notice.getProperties());
    when(persistentStore.get(any(), any())).thenReturn(new ArrayList<>());

    alertListener.handleEvent(event);
    verify(persistentStore).add(eq("alerts"), any(PersistentItem.class));
  }

  @Test
  public void testHandleEventExistingAlert() throws Exception {
    SystemNotice notice =
        new SystemNotice("test-source", NoticePriority.IMPORTANT, "title", new HashSet());
    Map<String, Object> props = notice.getProperties();
    props.put("event.topics", "decanter/alert/test");
    Alert alert = new Alert(props);
    ArgumentCaptor<PersistentItem> captor = ArgumentCaptor.forClass(PersistentItem.class);

    Event event = new Event("decanter/alert/test", alert.getProperties());
    when(persistentStore.get(any(), any())).thenReturn(Collections.singletonList(toSolr(alert)));
    alertListener.handleEvent(event);
    verify(persistentStore).add(eq("alerts"), captor.capture());
    PersistentItem item = captor.getValue();
    assertThat(item.getLongProperty(Alert.ALERT_COUNT), equalTo(2L));
  }

  @Test
  public void testHandleNonSystemNoticeAlert() throws Exception {
    ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
    Map<String, String> genericAlert = new HashMap<>();
    genericAlert.put("property1", "value1");
    genericAlert.put(Alert.SYSTEM_NOTICE_SOURCE_KEY, "source");
    when(persistentStore.get(any(), any())).thenReturn(new ArrayList<>());
    Event event = new Event("decanter/alert/test", genericAlert);
    alertListener.handleEvent(event);

    verify(persistentStore).add(eq("alerts"), any(PersistentItem.class));
  }

  @Test
  public void testHandleInvalidNonSystemNoticeAlert() throws Exception {
    Map<String, String> genericAlert = new HashMap<>();
    genericAlert.put("property1", "value1");

    when(persistentStore.get(any(), any())).thenReturn(new ArrayList<>());
    Event event = new Event("decanter/alert/test", genericAlert);
    alertListener.handleEvent(event);

    verify(persistentStore, never()).add(eq("alerts"), any(PersistentItem.class));
  }

  @Test
  public void testHandleEventDismiss() throws Exception {
    Alert alert = new Alert("test-source", NoticePriority.IMPORTANT, "title", new HashSet());
    ArgumentCaptor<PersistentItem> captor = ArgumentCaptor.forClass(PersistentItem.class);
    Map<String, String> dismissEvent = new HashMap<>();
    dismissEvent.put(Alert.SYSTEM_NOTICE_ID_KEY, alert.getId());
    dismissEvent.put(Alert.ALERT_DISMISSED_BY, "test-user");
    Event event = new Event(Alert.ALERT_DISMISS_TOPIC, dismissEvent);
    when(persistentStore.get(any(), any())).thenReturn(Collections.singletonList(toSolr(alert)));
    alertListener.handleEvent(event);
    verify(persistentStore).add(eq("alerts"), captor.capture());
    PersistentItem item = captor.getValue();
    assertThat(item.getTextProperty(Alert.ALERT_DISMISSED_BY), equalTo("test-user"));
    assertThat(item.getTextProperty(Alert.ALERT_STATUS), equalTo(Alert.ALERT_DISMISSED_STATUS));
  }

  @Test
  public void testHandleEventDismissNoId() throws Exception {
    Map<String, String> dismissEvent = new HashMap<>();
    dismissEvent.put(Alert.ALERT_DISMISSED_BY, "test-user");
    Event event = new Event(Alert.ALERT_DISMISS_TOPIC, dismissEvent);
    alertListener.handleEvent(event);
    verify(persistentStore, never()).add(any(), any(PersistentItem.class));
  }

  @Test
  public void testHandleEventDismissBadId() throws Exception {
    Map<String, String> dismissEvent = new HashMap<>();
    dismissEvent.put(Alert.SYSTEM_NOTICE_ID_KEY, "bad-id");
    dismissEvent.put(Alert.ALERT_DISMISSED_BY, "test-user");
    Event event = new Event(Alert.ALERT_DISMISS_TOPIC, dismissEvent);
    when(persistentStore.get(any(), any())).thenReturn(new ArrayList<>());
    alertListener.handleEvent(event);
    verify(persistentStore, never()).add(any(), any(PersistentItem.class));
  }

  @Test
  public void testHandleEventDismissNoDismissedBy() throws Exception {
    SystemNotice alert =
        new SystemNotice("test-source", NoticePriority.IMPORTANT, "title", new HashSet());
    ArgumentCaptor<PersistentItem> captor = ArgumentCaptor.forClass(PersistentItem.class);
    Map<String, String> dismissEvent = new HashMap<>();
    dismissEvent.put(Alert.SYSTEM_NOTICE_ID_KEY, alert.getId());
    Event event = new Event(Alert.ALERT_DISMISS_TOPIC, dismissEvent);
    when(persistentStore.get(any(), any())).thenReturn(Collections.singletonList(toSolr(alert)));
    alertListener.handleEvent(event);
    verify(persistentStore, never()).add(any(), any(PersistentItem.class));
  }

  private Map<String, Object> toSolr(SystemNotice alert) {
    Map<String, Object> map = new HashMap<>();
    for (Map.Entry entry : alert.getProperties().entrySet()) {
      map.put(entry.getKey() + "_txt", entry.getValue());
    }
    return map;
  }
}
