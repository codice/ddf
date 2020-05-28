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
package ddf.catalog.event.retrievestatus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher.ProductRetrievalStatus;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.security.service.impl.SubjectUtils;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.notifications.Notification;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;

/**
 * Abstract class used to test the DownloadsStatusEventPublisher. <br>
 * <br>
 * This class contains test cases to verify that the event admin service is properly called after a
 * download event is triggered. Given the different types of events, implementations of this test
 * should set up the publisher to match their specific event type.
 */
public abstract class AbstractDownloadsStatusEventPublisherTest {

  protected static final Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(AbstractDownloadsStatusEventPublisherTest.class);

  private static final String USER_ID = "testSubjectUser";

  private static final String DEFAULT_USER_ID = "";

  private static final String SESSION_ID = "123456";

  protected static EventAdmin eventAdmin;

  protected static ActionProvider actionProvider;

  protected static Metacard metacard;

  protected static ResourceResponse resourceResponse;

  protected static ResourceRequest resourceRequest;

  protected static Resource resource;

  protected static Map<String, Serializable> properties;

  protected static DownloadsStatusEventPublisher publisher;

  protected static String downloadIdentifier;

  protected Event curEvent;

  @BeforeClass
  public static void oneTimeSetup() {
    resourceResponse = mock(ResourceResponse.class);
    resourceRequest = mock(ResourceRequest.class);
    resource = mock(Resource.class);
    properties = new HashMap<String, Serializable>();
    properties.put(Notification.NOTIFICATION_KEY_USER_ID, USER_ID);
    properties.put(Notification.NOTIFICATION_KEY_SESSION_ID, SESSION_ID);

    when(resource.getName()).thenReturn("testSessionID");
    when(resourceRequest.getProperties()).thenReturn(properties);
    when(resourceRequest.containsPropertyName(Notification.NOTIFICATION_KEY_USER_ID))
        .thenReturn(true);
    when(resourceRequest.getPropertyValue(Notification.NOTIFICATION_KEY_USER_ID))
        .thenReturn(properties.get(Notification.NOTIFICATION_KEY_USER_ID));
    when(resourceRequest.containsPropertyName(Notification.NOTIFICATION_KEY_SESSION_ID))
        .thenReturn(true);
    when(resourceRequest.getPropertyValue(Notification.NOTIFICATION_KEY_SESSION_ID))
        .thenReturn(properties.get(Notification.NOTIFICATION_KEY_SESSION_ID));
    when(resourceResponse.getResource()).thenReturn(resource);
    when(resourceResponse.getRequest()).thenReturn(resourceRequest);
  }

  @Before
  public void setUpTest() {
    eventAdmin = mock(EventAdmin.class);
    Mockito.doAnswer(
            new Answer<Object>() {

              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                curEvent = (Event) args[0];
                return null;
              }
            })
        .when(eventAdmin)
        .postEvent(any(Event.class));

    metacard = mock(Metacard.class);
    when(metacard.getId()).thenReturn("12345");
    downloadIdentifier = UUID.randomUUID().toString();
  }

  @After
  public void tearDownTest() {
    // Remove the security from the thread after every test
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  /** Calls the retrieval status test with a security subject */
  @org.junit.Test
  public void testPostRetrievalStatusWithSecurity() {
    setupPublisher();
    addSecurity();
    testPostRetrievalStatus(USER_ID);
  }

  /**
   * Calls the retrieval status test with no security subject (simulating no security in the
   * system).
   */
  @org.junit.Test
  public void testPostRetrievalStatusWithoutSecurity() {
    setupPublisher();
    testPostRetrievalStatus(DEFAULT_USER_ID);
  }

  /**
   * Tests that the retrieval status is properly sent to the event admin.
   *
   * @param correctUser user to check for in the event property
   */
  private void testPostRetrievalStatus(String correctUser) {
    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.CANCELLED,
        metacard,
        "test detail",
        20L,
        downloadIdentifier);
    verify(eventAdmin, times(1)).postEvent(any(Event.class));
    assertEquals(correctUser, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.FAILED,
        metacard,
        "test detail",
        250L,
        downloadIdentifier);
    verify(eventAdmin, times(2)).postEvent(any(Event.class));
    assertEquals(correctUser, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.RETRYING,
        metacard,
        "test detail",
        350L,
        downloadIdentifier);
    verify(eventAdmin, times(3)).postEvent(any(Event.class));
    assertEquals(correctUser, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.COMPLETE,
        metacard,
        "test detail",
        500L,
        downloadIdentifier);
    verify(eventAdmin, times(4)).postEvent(any(Event.class));
    assertEquals(correctUser, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));
  }

  /**
   * Verifies client can set parameter to only publish a notification, no corresponding activity.
   */
  @org.junit.Test
  public void testPostRetrievalStatusNotificationOnly() {
    setupPublisher();
    publisher.setActivityEnabled(true);
    publisher.setNotificationEnabled(true);
    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.CANCELLED,
        metacard,
        "test detail",
        20L,
        downloadIdentifier,
        true,
        false);
    // By expecting only 1 invocation of postEvent(), this verifies only a notification was sent,
    // and no activity. No way to check absence of ActivityEvent.
    verify(eventAdmin, times(1)).postEvent(any(Event.class));
    assertEquals(DEFAULT_USER_ID, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));
  }

  /**
   * Verifies client can set parameter to only publish an activity, no corresponding notification.
   */
  @org.junit.Test
  public void testPostRetrievalStatusActivityOnly() {
    setupPublisher();
    publisher.setActivityEnabled(true);
    publisher.setNotificationEnabled(true);
    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.CANCELLED,
        metacard,
        "test detail",
        20L,
        downloadIdentifier,
        false,
        true);
    // By expecting only 1 invocation of postEvent(), this verifies only an activity was sent,
    // and no notification. No way to check absence of Notification.
    verify(eventAdmin, times(1)).postEvent(any(Event.class));
    assertEquals(DEFAULT_USER_ID, curEvent.getProperty(ActivityEvent.USER_ID_KEY));
  }

  /**
   * Verifies client can set parameters to publish an activity and its corresponding notification.
   * This is the default behavior.
   */
  @org.junit.Test
  public void testPostRetrievalStatusSendNotificationAndActivity() {
    setupPublisher();
    publisher.setActivityEnabled(true);
    publisher.setNotificationEnabled(true);
    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.CANCELLED,
        metacard,
        "test detail",
        20L,
        downloadIdentifier,
        true,
        true);
    // Expect 2 invocations of postEvent(), one for Notification and one for ActivityEvent
    verify(eventAdmin, times(2)).postEvent(any(Event.class));
    assertEquals(DEFAULT_USER_ID, curEvent.getProperty(ActivityEvent.USER_ID_KEY));
    assertEquals(DEFAULT_USER_ID, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));

    // Also verifying the method with default values to always send both notification and activity
    // is working.
    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.CANCELLED,
        metacard,
        "test detail",
        20L,
        downloadIdentifier);
    verify(eventAdmin, times(4)).postEvent(any(Event.class));
    assertEquals(DEFAULT_USER_ID, curEvent.getProperty(ActivityEvent.USER_ID_KEY));
    assertEquals(DEFAULT_USER_ID, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));
  }

  /**
   * Calls the no name property test with a security subject (simulating security in the system).
   */
  @org.junit.Test
  public void testPostRetrievalStatusWithNoNamePropertyWithSecurity() {
    setupPublisher();
    addSecurity();
    testPostRetrievalStatusWithNoNameProperty(USER_ID);
  }

  /**
   * Calls the no name property test with no security subject (simulating no security in the
   * system).
   */
  @org.junit.Test
  public void testPostRetrievalStatusWithNoNamePropertyWithoutSecurity() {
    setupPublisher();
    testPostRetrievalStatusWithNoNameProperty(DEFAULT_USER_ID);
  }

  /**
   * Tests that the event admin is properly called when a status is sent with no name property.
   *
   * @param correctUser user to check for in the event property
   */
  private void testPostRetrievalStatusWithNoNameProperty(String correctUser) {
    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.CANCELLED,
        metacard,
        "test detail",
        20L,
        downloadIdentifier);
    verify(eventAdmin, times(1)).postEvent(any(Event.class));
    assertEquals(correctUser, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.FAILED,
        metacard,
        "test detail",
        250L,
        downloadIdentifier);
    verify(eventAdmin, times(2)).postEvent(any(Event.class));
    assertEquals(correctUser, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.RETRYING,
        metacard,
        "test detail",
        350L,
        downloadIdentifier);
    verify(eventAdmin, times(3)).postEvent(any(Event.class));
    assertEquals(correctUser, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.COMPLETE,
        metacard,
        "test detail",
        500L,
        downloadIdentifier);
    verify(eventAdmin, times(4)).postEvent(any(Event.class));
    assertEquals(correctUser, curEvent.getProperty(Notification.NOTIFICATION_KEY_USER_ID));
  }

  @org.junit.Test
  public void testPostRetrievalWithNoStatus() {
    setupPublisherWithNoNotifications();

    publisher.postRetrievalStatus(
        resourceResponse, ProductRetrievalStatus.STARTED, metacard, null, 0L, downloadIdentifier);
    verify(eventAdmin, times(0)).postEvent(any(Event.class));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.STARTED,
        metacard,
        "test detail",
        10L,
        downloadIdentifier);
    verify(eventAdmin, times(0)).postEvent(any(Event.class));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.CANCELLED,
        metacard,
        "test detail",
        20L,
        downloadIdentifier);
    verify(eventAdmin, times(0)).postEvent(any(Event.class));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.FAILED,
        metacard,
        "test detail",
        250L,
        downloadIdentifier);
    verify(eventAdmin, times(0)).postEvent(any(Event.class));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.RETRYING,
        metacard,
        "test detail",
        350L,
        downloadIdentifier);
    verify(eventAdmin, times(0)).postEvent(any(Event.class));

    publisher.postRetrievalStatus(
        resourceResponse,
        ProductRetrievalStatus.COMPLETE,
        metacard,
        "test detail",
        500L,
        downloadIdentifier);
    verify(eventAdmin, times(0)).postEvent(any(Event.class));
  }

  protected abstract void setupPublisher();

  private void setupPublisherWithNoNotifications() {
    actionProvider = mock(ActionProvider.class);
    eventAdmin = mock(EventAdmin.class);
    publisher = new DownloadsStatusEventPublisher(eventAdmin, ImmutableList.of(actionProvider));
    publisher.setSubjectOperations(new SubjectUtils());
    publisher.setNotificationEnabled(false);
    publisher.setActivityEnabled(false);
  }

  private void addSecurity() {
    org.apache.shiro.mgt.SecurityManager secManager = new DefaultSecurityManager();
    PrincipalCollection principals = new SimplePrincipalCollection(USER_ID, "testrealm");
    Subject subject =
        new Subject.Builder(secManager)
            .principals(principals)
            .session(new SimpleSession())
            .authenticated(true)
            .buildSubject();
    ThreadContext.bind(secManager);
    ThreadContext.bind(subject);
  }
}
