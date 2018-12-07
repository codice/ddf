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
package org.codice.ddf.catalog.pubsub.command;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.event.Subscriber;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DeleteCommandTest {
  private static final String SUBSCRIPTION_ID_PROPERTY_KEY = "subscription-id";

  private static final String MY_SUBSCRIPTION_ID =
      "my.contextual.id|http://172.18.14.169:8088/mockCatalogEventConsumerBinding?WSDL";

  private static final String YOUR_SUBSCRIPTION_ID =
      "your.contextual.id|http://172.18.14.169:8088/mockCatalogEventConsumerBinding?WSDL";

  @Test
  public void testDeleteNoSubscriptionsRegistered() throws Exception {
    DeleteCommand deleteCommand = new DeleteCommand();
    deleteCommand.id = "my.subscription.id";

    BundleContext bundleContext = mock(BundleContext.class);
    deleteCommand.setBundleContext(bundleContext);

    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), anyString()))
        .thenReturn(new ServiceReference[] {});

    PrintStream realSystemOut = System.out;

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    System.setOut(new PrintStream(buffer));

    // when
    deleteCommand.execute();

    /* cleanup */
    System.setOut(realSystemOut);

    // then
    assertThat(
        buffer.toString(),
        startsWith(
            DeleteCommand.RED_CONSOLE_COLOR
                + DeleteCommand.NO_SUBSCRIPTIONS_FOUND_MSG
                + DeleteCommand.DEFAULT_CONSOLE_COLOR));

    buffer.close();
  }

  @Test
  public void testDeleteSingleSubscriptionById() throws Exception {
    DeleteCommand deleteCommand = new DeleteCommand();

    BundleContext bundleContext = mock(BundleContext.class);
    deleteCommand.setBundleContext(bundleContext);

    ServiceReference mySubscription = mock(ServiceReference.class);
    when(mySubscription.getPropertyKeys()).thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(mySubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(MY_SUBSCRIPTION_ID);

    ServiceReference yourSubscription = mock(ServiceReference.class);
    when(yourSubscription.getPropertyKeys())
        .thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(yourSubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY))
        .thenReturn(YOUR_SUBSCRIPTION_ID);

    String ldapFilter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + MY_SUBSCRIPTION_ID + ")";

    // Return Subscription's ServiceReference upon first invocation (when searching for
    // subscription to be deleted),
    // and return no ServiceReference upon second invocation (when searching to verify
    // subscription was deleted).
    // NOTE: List of comma-delimited return values specified in the thenReturn() method is
    // Mockito's way of
    // supporting stubbing of consecutive calls to same mocked method.
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(ldapFilter)))
        .thenReturn(new ServiceReference[] {mySubscription}, new ServiceReference[] {});

    Subscriber mockSubscriber = mock(Subscriber.class);
    ServiceReference mockSubscriberServiceRef = mock(ServiceReference.class);
    when(bundleContext.getServiceReferences(
            eq(DeleteCommand.SUBSCRIBER_SERVICE_PID), nullable(String.class)))
        .thenReturn(new ServiceReference[] {mockSubscriberServiceRef});
    when(bundleContext.getService(any(ServiceReference.class))).thenReturn(mockSubscriber);
    when(mockSubscriber.deleteSubscription(anyString())).thenReturn(true);

    PrintStream realSystemOut = System.out;

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    System.setOut(new PrintStream(buffer));

    // when
    deleteCommand.id = MY_SUBSCRIPTION_ID;
    deleteCommand.execute();

    /* cleanup */
    System.setOut(realSystemOut);

    // then
    assertThat(buffer.toString(), containsString(DeleteCommand.DELETE_MSG + MY_SUBSCRIPTION_ID));
    assertThat(
        buffer.toString(), containsString("Deleted 1 subscriptions out of 1 subscriptions found."));

    buffer.close();
  }

  @Test
  public void testDeleteMultipleSubscriptionsById() throws Exception {
    DeleteCommand deleteCommand = new DeleteCommand();

    BundleContext bundleContext = mock(BundleContext.class);
    deleteCommand.setBundleContext(bundleContext);

    ServiceReference mySubscription = mock(ServiceReference.class);
    when(mySubscription.getPropertyKeys()).thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(mySubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(MY_SUBSCRIPTION_ID);

    ServiceReference yourSubscription = mock(ServiceReference.class);
    when(yourSubscription.getPropertyKeys())
        .thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(yourSubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY))
        .thenReturn(YOUR_SUBSCRIPTION_ID);

    ServiceReference[] refs = new ServiceReference[] {mySubscription, yourSubscription};
    String ldapFilter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=my*)";
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(ldapFilter)))
        .thenReturn(refs);
    ldapFilter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + MY_SUBSCRIPTION_ID + ")";
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(ldapFilter)))
        .thenReturn(new ServiceReference[] {});
    ldapFilter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + YOUR_SUBSCRIPTION_ID + ")";
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(ldapFilter)))
        .thenReturn(new ServiceReference[] {});

    Subscriber mockSubscriber = mock(Subscriber.class);
    ServiceReference mockSubscriberServiceRef = mock(ServiceReference.class);
    when(bundleContext.getServiceReferences(
            eq(DeleteCommand.SUBSCRIBER_SERVICE_PID), nullable(String.class)))
        .thenReturn(new ServiceReference[] {mockSubscriberServiceRef});
    when(bundleContext.getService(any(ServiceReference.class))).thenReturn(mockSubscriber);
    when(mockSubscriber.deleteSubscription(anyString())).thenReturn(true);

    PrintStream realSystemOut = System.out;

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    System.setOut(new PrintStream(buffer));

    // when
    deleteCommand.id = "my*";
    deleteCommand.execute();

    /* cleanup */
    System.setOut(realSystemOut);

    // then
    assertThat(buffer.toString(), containsString(DeleteCommand.DELETE_MSG + MY_SUBSCRIPTION_ID));
    assertThat(buffer.toString(), containsString(DeleteCommand.DELETE_MSG + YOUR_SUBSCRIPTION_ID));
    assertThat(
        buffer.toString(), containsString("Deleted 2 subscriptions out of 2 subscriptions found."));

    buffer.close();
  }

  @Test
  public void testDeleteMultipleSubscriptionsByLdapFilter() throws Exception {
    DeleteCommand deleteCommand = new DeleteCommand();

    BundleContext bundleContext = mock(BundleContext.class);
    deleteCommand.setBundleContext(bundleContext);

    ServiceReference mySubscription = mock(ServiceReference.class);
    when(mySubscription.getPropertyKeys()).thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(mySubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(MY_SUBSCRIPTION_ID);

    ServiceReference yourSubscription = mock(ServiceReference.class);
    when(yourSubscription.getPropertyKeys())
        .thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(yourSubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY))
        .thenReturn(YOUR_SUBSCRIPTION_ID);

    String ldapFilter = "(& (subscription-id=my*) (subscription-id=*WSDL))";
    ServiceReference[] refs = new ServiceReference[] {mySubscription, yourSubscription};
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(ldapFilter)))
        .thenReturn(refs);

    // Return empty ServiceReference lists when getting ServiceReference by explicit
    // subscription ID as this invocation
    // is when DeleteCommand is verifying the subscription was deleted.
    String filter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + MY_SUBSCRIPTION_ID + ")";
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(filter)))
        .thenReturn(new ServiceReference[] {});
    filter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + YOUR_SUBSCRIPTION_ID + ")";
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(filter)))
        .thenReturn(new ServiceReference[] {});

    Subscriber mockSubscriber = mock(Subscriber.class);
    ServiceReference mockSubscriberServiceRef = mock(ServiceReference.class);
    when(bundleContext.getServiceReferences(
            eq(DeleteCommand.SUBSCRIBER_SERVICE_PID), nullable(String.class)))
        .thenReturn(new ServiceReference[] {mockSubscriberServiceRef});
    when(bundleContext.getService(any(ServiceReference.class))).thenReturn(mockSubscriber);
    when(mockSubscriber.deleteSubscription(anyString())).thenReturn(true);

    PrintStream realSystemOut = System.out;

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    System.setOut(new PrintStream(buffer));

    // when
    deleteCommand.id = ldapFilter;
    deleteCommand.ldapFilter = true;
    deleteCommand.execute();

    /* cleanup */
    System.setOut(realSystemOut);

    // then
    assertThat(buffer.toString(), containsString(DeleteCommand.DELETE_MSG + MY_SUBSCRIPTION_ID));
    assertThat(buffer.toString(), containsString(DeleteCommand.DELETE_MSG + YOUR_SUBSCRIPTION_ID));
    assertThat(
        buffer.toString(), containsString("Deleted 2 subscriptions out of 2 subscriptions found."));

    buffer.close();
  }

  @Test
  public void testUnableToDeleteSubscription() throws Exception {
    DeleteCommand deleteCommand = new DeleteCommand();

    BundleContext bundleContext = mock(BundleContext.class);
    deleteCommand.setBundleContext(bundleContext);

    ServiceReference mySubscription = mock(ServiceReference.class);
    when(mySubscription.getPropertyKeys()).thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(mySubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(MY_SUBSCRIPTION_ID);

    ServiceReference yourSubscription = mock(ServiceReference.class);
    when(yourSubscription.getPropertyKeys())
        .thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(yourSubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY))
        .thenReturn(YOUR_SUBSCRIPTION_ID);

    String ldapFilter = "(& (subscription-id=my*) (subscription-id=*WSDL))";
    ServiceReference[] refs = new ServiceReference[] {mySubscription, yourSubscription};
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(ldapFilter)))
        .thenReturn(refs);

    // Return empty ServiceReference list for mySubscriptionId but return actual
    // ServiceReference for yourSubscriptionId
    // when getting ServiceReference by explicit subscription ID as this invocation
    // is when DeleteCommand is verifying the subscription was deleted, and want to simulate
    // that yourSubscriptionId was unable
    // to be deleted.
    String filter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + MY_SUBSCRIPTION_ID + ")";
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(filter)))
        .thenReturn(new ServiceReference[] {});
    filter = "(" + SUBSCRIPTION_ID_PROPERTY_KEY + "=" + YOUR_SUBSCRIPTION_ID + ")";
    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), eq(filter)))
        .thenReturn(new ServiceReference[] {yourSubscription});

    Subscriber mockSubscriber = mock(Subscriber.class);
    ServiceReference mockSubscriberServiceRef = mock(ServiceReference.class);
    when(bundleContext.getServiceReferences(
            eq(DeleteCommand.SUBSCRIBER_SERVICE_PID), nullable(String.class)))
        .thenReturn(new ServiceReference[] {mockSubscriberServiceRef});
    when(bundleContext.getService(any(ServiceReference.class))).thenReturn(mockSubscriber);
    when(mockSubscriber.deleteSubscription(eq(MY_SUBSCRIPTION_ID))).thenReturn(true);
    when(mockSubscriber.deleteSubscription(eq(YOUR_SUBSCRIPTION_ID))).thenReturn(false);

    PrintStream realSystemOut = System.out;

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    System.setOut(new PrintStream(buffer));

    // when
    deleteCommand.id = ldapFilter;
    deleteCommand.ldapFilter = true;
    deleteCommand.execute();

    /* cleanup */
    System.setOut(realSystemOut);

    // then
    assertThat(buffer.toString(), containsString(DeleteCommand.DELETE_MSG + MY_SUBSCRIPTION_ID));
    assertThat(
        buffer.toString(),
        containsString(
            DeleteCommand.RED_CONSOLE_COLOR
                + DeleteCommand.UNABLE_TO_DELETE_MSG
                + YOUR_SUBSCRIPTION_ID
                + DeleteCommand.DEFAULT_CONSOLE_COLOR));

    buffer.close();
  }

  @Test
  public void testDeleteNoSubscriptionsMatchId() throws Exception {
    DeleteCommand deleteCommand = new DeleteCommand();

    BundleContext bundleContext = mock(BundleContext.class);
    deleteCommand.setBundleContext(bundleContext);

    ServiceReference mySubscription = mock(ServiceReference.class);
    when(mySubscription.getPropertyKeys()).thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(mySubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(MY_SUBSCRIPTION_ID);

    ServiceReference yourSubscription = mock(ServiceReference.class);
    when(yourSubscription.getPropertyKeys())
        .thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(yourSubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY))
        .thenReturn(YOUR_SUBSCRIPTION_ID);

    when(bundleContext.getServiceReferences(
            eq(SubscriptionsCommand.SERVICE_PID), eq(MY_SUBSCRIPTION_ID)))
        .thenReturn(new ServiceReference[] {mySubscription});
    when(bundleContext.getServiceReferences(
            eq(SubscriptionsCommand.SERVICE_PID), eq(YOUR_SUBSCRIPTION_ID)))
        .thenReturn(new ServiceReference[] {yourSubscription});

    Subscriber mockSubscriber = mock(Subscriber.class);
    ServiceReference mockSubscriberServiceRef = mock(ServiceReference.class);
    when(bundleContext.getServiceReferences(
            eq(DeleteCommand.SUBSCRIBER_SERVICE_PID), nullable(String.class)))
        .thenReturn(new ServiceReference[] {mockSubscriberServiceRef});
    when(bundleContext.getService(any(ServiceReference.class))).thenReturn(mockSubscriber);
    when(mockSubscriber.deleteSubscription(anyString())).thenReturn(true);

    PrintStream realSystemOut = System.out;

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    System.setOut(new PrintStream(buffer));

    // when
    deleteCommand.id = "abc*";
    deleteCommand.execute();

    /* cleanup */
    System.setOut(realSystemOut);

    // then
    assertThat(
        buffer.toString(),
        startsWith(
            DeleteCommand.RED_CONSOLE_COLOR
                + DeleteCommand.NO_SUBSCRIPTIONS_FOUND_MSG
                + DeleteCommand.DEFAULT_CONSOLE_COLOR));

    buffer.close();
  }

  @Test
  public void testDeleteNoSubscribersFound() throws Exception {
    DeleteCommand deleteCommand = new DeleteCommand();

    BundleContext bundleContext = mock(BundleContext.class);
    deleteCommand.setBundleContext(bundleContext);

    ServiceReference mySubscription = mock(ServiceReference.class);
    when(mySubscription.getPropertyKeys()).thenReturn(new String[] {SUBSCRIPTION_ID_PROPERTY_KEY});
    when(mySubscription.getProperty(SUBSCRIPTION_ID_PROPERTY_KEY)).thenReturn(MY_SUBSCRIPTION_ID);

    when(bundleContext.getServiceReferences(eq(SubscriptionsCommand.SERVICE_PID), anyString()))
        .thenReturn(new ServiceReference[] {mySubscription});

    when(bundleContext.getServiceReferences(eq(DeleteCommand.SUBSCRIBER_SERVICE_PID), anyString()))
        .thenReturn(new ServiceReference[] {});

    PrintStream realSystemOut = System.out;

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    System.setOut(new PrintStream(buffer));

    // when
    deleteCommand.id = "my*";
    deleteCommand.execute();

    /* cleanup */
    System.setOut(realSystemOut);

    // then
    assertThat(
        buffer.toString(),
        startsWith(
            DeleteCommand.RED_CONSOLE_COLOR
                + DeleteCommand.NO_SUBSCRIBERS_FOUND_MSG
                + DeleteCommand.DEFAULT_CONSOLE_COLOR));

    buffer.close();
  }
}
