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
package org.codice.ddf.admin.core.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.system.alerts.Alert;
import org.codice.ddf.system.alerts.SystemNotice;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@RunWith(MockitoJUnitRunner.class)
public class AdminAlertImplTest {

  @Mock PersistentStore persistentStore;

  @Mock EventAdmin eventAdmin;

  @Mock Subject subject;

  @Mock PrincipalCollection principals;

  AdminAlertImpl adminAlert;

  Map<String, Object> solrMap;

  @Before
  public void setup() throws Exception {
    adminAlert = new AdminAlertImpl(persistentStore, eventAdmin);
    solrMap = new HashMap<>();
    solrMap.put(SystemNotice.SYSTEM_NOTICE_ID_KEY + "_txt", "myId");
  }

  @After
  public void tearDown() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void testGetAlerts() throws Exception {
    when(persistentStore.get(eq("alerts"), any())).thenReturn(Collections.singletonList(solrMap));
    List<Map<String, Object>> alertList = adminAlert.getAlerts();
    assertThat(alertList.size(), equalTo(1));
    assertThat(alertList.get(0).get("id"), equalTo("myId"));
  }

  @Test
  public void textGetAlertsEscapedStrings() throws Exception {
    solrMap.put("xss-data_txt", "<script>alert('stuff')</script>");
    when(persistentStore.get(eq("alerts"), any())).thenReturn(Collections.singletonList(solrMap));
    List<Map<String, Object>> alertList = adminAlert.getAlerts();
    assertThat(alertList.size(), equalTo(1));
    assertThat(
        alertList.get(0).get("xss-data"),
        equalTo("&lt;script&gt;alert(&#39;stuff&#39;)&lt;/script&gt;"));
  }

  @Test
  public void testGetAlertsNoAlerts() throws Exception {
    when(persistentStore.get(eq("alerts"), any())).thenReturn(Collections.emptyList());
    List<Map<String, Object>> alertList = adminAlert.getAlerts();
    assertThat(alertList.size(), equalTo(0));
  }

  @Test
  public void testGetAlertsWithSingleDetail() throws Exception {
    solrMap.put(SystemNotice.SYSTEM_NOTICE_DETAILS_KEY + "_txt", "details");
    when(persistentStore.get(eq("alerts"), any())).thenReturn(Collections.singletonList(solrMap));
    List<Map<String, Object>> alertList = adminAlert.getAlerts();
    assertThat(alertList.size(), equalTo(1));
    assertThat(
        alertList.get(0).get(SystemNotice.SYSTEM_NOTICE_DETAILS_KEY), instanceOf(Collection.class));
  }

  @Test
  public void testGetAlertsPersistentStoreError() throws Exception {
    when(persistentStore.get(eq("alerts"), any())).thenThrow(new PersistenceException("error"));
    List<Map<String, Object>> alertList = adminAlert.getAlerts();
    assertThat(alertList.size(), equalTo(1));
    assertThat(
        alertList.get(0).get(Alert.SYSTEM_NOTICE_SOURCE_KEY), equalTo("unable_to_retrieve_alerts"));
  }

  @Test
  public void testDismissAlert() throws Exception {
    ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
    when(subject.getPrincipals()).thenReturn(principals);
    when(principals.byType(any())).thenReturn(Collections.emptyList());
    when(principals.getPrimaryPrincipal()).thenReturn("user");
    ThreadContext.bind(subject);
    adminAlert.dismissAlert("myId");
    verify(eventAdmin).postEvent(captor.capture());
    Event event = captor.getValue();
    assertThat(event.getTopic(), equalTo(Alert.ALERT_DISMISS_TOPIC));
    assertThat(event.getProperty(Alert.ALERT_DISMISSED_BY), equalTo("user"));
  }

  @Test
  public void testDismissAlertNoSubject() throws Exception {
    when(subject.getPrincipals()).thenReturn(null);
    ThreadContext.bind(subject);
    adminAlert.dismissAlert("myId");
    verify(eventAdmin, never()).postEvent(any());
  }

  @Test
  public void testDismissAlertNoId() throws Exception {
    adminAlert.dismissAlert(null);
    verify(eventAdmin, never()).postEvent(any());
    adminAlert.dismissAlert("");
    verify(eventAdmin, never()).postEvent(any());
  }
}
