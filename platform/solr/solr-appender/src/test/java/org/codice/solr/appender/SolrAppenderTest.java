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
package org.codice.solr.appender;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.system.alerts.SystemNotice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.event.Event;

public class SolrAppenderTest {

  private PersistentStore persistentStore;

  private ScheduledExecutorService executorService;

  private ScheduledFuture scheduledFuture;

  private SolrAppender solrAppender;

  @Before
  public void setup() {
    persistentStore = mock(PersistentStore.class);
    executorService = mock(ScheduledExecutorService.class);
    scheduledFuture = mock(ScheduledFuture.class);

    solrAppender = new SolrAppender(persistentStore, executorService);
    when(executorService.scheduleAtFixedRate(any(), anyLong(), anyLong(), any()))
        .thenReturn(scheduledFuture);
    solrAppender.init();
  }

  @Test
  public void testHandleEvent() throws Exception {
    SystemNotice notice = new SystemNotice();
    Event event = new Event("decanter/collect", notice.getProperties());
    solrAppender.setBatchSize(0);
    solrAppender.handleEvent(event);
    verify(persistentStore).add(eq("decanter"), any(Collection.class));
  }

  @Test
  public void testHandleEventNoId() throws Exception {
    ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
    Map<String, String> map = new HashMap<>();
    map.put("property", "value");
    Event event = new Event("decanter/collect", map);
    solrAppender.setBatchSize(0);
    solrAppender.handleEvent(event);
    verify(persistentStore).add(eq("decanter"), captor.capture());
    Map<String, Object> item = (Map<String, Object>) captor.getValue().iterator().next();
    assertNotNull(item.get("id_txt"));
  }

  @Test
  public void testFlushItemsEmptyQueue() throws Exception {
    solrAppender.flushItems();
    verify(persistentStore, never()).add(eq("decanter"), any(Collection.class));
  }

  @Test
  public void testHandleEventSizeBatching() throws Exception {
    SystemNotice notice = new SystemNotice();
    Event event = new Event("decanter/collect", notice.getProperties());
    solrAppender.setBatchSize(1);
    solrAppender.handleEvent(event);
    verify(persistentStore, never()).add(eq("decanter"), any(Collection.class));
    solrAppender.handleEvent(event);
    verify(persistentStore).add(eq("decanter"), any(Collection.class));
  }

  @Test
  public void testSetPeriod() {
    solrAppender.setPeriod(5);
    verify(scheduledFuture).cancel(false);
  }

  @Test
  public void testSetPeriodSamePeriod() {
    solrAppender.setPeriod(10);
    verify(scheduledFuture, never()).cancel(anyBoolean());
  }
}
