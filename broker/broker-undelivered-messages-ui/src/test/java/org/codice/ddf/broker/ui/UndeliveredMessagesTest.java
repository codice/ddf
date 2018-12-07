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
package org.codice.ddf.broker.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UndeliveredMessagesTest {

  private static final String ADDRESS = "queue.test";

  private static final String QUEUE = "queue.test";

  private static final String BROWSE = "browse";

  private static final String REMOVE_MESSAGE = "removeMessage";

  private static final String RETRY_MESSAGE = "retryMessage";

  private final ObjectName objectName =
      new ObjectName(
          "org.apache.activemq.artemis:broker=\"artemis\",component=addresses,address=\""
              + ADDRESS
              + "\",subcomponent=queues,routing-type=\"anycast\",queue=\""
              + QUEUE
              + "\"");

  private UndeliveredMessages undeliveredMessagesService;

  @Mock private MBeanServer mockMBeanServer;

  public UndeliveredMessagesTest() throws MalformedObjectNameException {}

  @Before
  public void setup() {
    undeliveredMessagesService = new UndeliveredMessages(mockMBeanServer);
    undeliveredMessagesService.init();

    when(mockMBeanServer.isRegistered(objectName)).thenReturn(true);
  }

  @Test
  public void testGetMessages()
      throws MalformedObjectNameException, OpenDataException, MBeanException,
          InstanceNotFoundException, ReflectionException {
    when(mockMBeanServer.invoke(
            objectName, BROWSE, new Object[] {""}, new String[] {String.class.getName()}))
        .thenReturn(createCompositeData("12345messageBody".getBytes()));
    List<CompositeData> undeliveredMessages =
        undeliveredMessagesService.getMessages(ADDRESS, QUEUE);

    validateCompositeData(undeliveredMessages);
    assertThat(undeliveredMessages.get(0).get("body"), is("messageBody"));
  }

  @Test
  public void testGetMessagesWithNullChars()
      throws MalformedObjectNameException, OpenDataException, MBeanException,
          InstanceNotFoundException, ReflectionException {
    byte[] bytes = new byte[] {49, 50, 51, 52, 53, 50, 0, 51, 0, 52, 0, 53};
    when(mockMBeanServer.invoke(
            objectName, BROWSE, new Object[] {""}, new String[] {String.class.getName()}))
        .thenReturn(createCompositeData(bytes));
    List<CompositeData> undeliveredMessages =
        undeliveredMessagesService.getMessages(ADDRESS, QUEUE);

    validateCompositeData(undeliveredMessages);
    assertThat(undeliveredMessages.get(0).get("body"), is("2345"));
  }

  @Test
  public void testGetMessagesMbeanNotRegistered() {
    when(mockMBeanServer.isRegistered(objectName)).thenReturn(false);

    List<CompositeData> undeliveredMessages =
        undeliveredMessagesService.getMessages(ADDRESS, QUEUE);

    assertThat(undeliveredMessages.size(), is(0));
  }

  @Test
  public void testGetMessagesMbeanException()
      throws MBeanException, InstanceNotFoundException, ReflectionException, OpenDataException {
    when(mockMBeanServer.invoke(
            objectName, BROWSE, new Object[] {""}, new String[] {String.class.getName()}))
        .thenThrow(ReflectionException.class);

    List<CompositeData> undeliveredMessages =
        undeliveredMessagesService.getMessages(ADDRESS, QUEUE);

    assertThat(undeliveredMessages.size(), is(0));
  }

  @Test
  public void testDeleteMessage()
      throws MBeanException, InstanceNotFoundException, ReflectionException {
    when(mockMBeanServer.invoke(
            objectName, REMOVE_MESSAGE, new Object[] {1L}, new String[] {long.class.getName()}))
        .thenReturn(true);

    long messageDeleted =
        undeliveredMessagesService.deleteMessages(ADDRESS, QUEUE, Collections.singletonList("1"));

    assertThat(messageDeleted, is(1L));
  }

  @Test
  public void testDeleteMessageNullChars()
      throws MBeanException, InstanceNotFoundException, ReflectionException {
    when(mockMBeanServer.invoke(
            objectName,
            new String(REMOVE_MESSAGE),
            new Object[] {1L},
            new String[] {long.class.getName()}))
        .thenReturn(true);

    long messageDeleted =
        undeliveredMessagesService.deleteMessages(ADDRESS, QUEUE, Collections.singletonList("1"));

    assertThat(messageDeleted, is(1L));
  }

  @Test
  public void testDeleteMessageException()
      throws MBeanException, InstanceNotFoundException, ReflectionException {
    when(mockMBeanServer.invoke(
            objectName, REMOVE_MESSAGE, new Object[] {1L}, new String[] {long.class.getName()}))
        .thenThrow(MBeanException.class);

    long messageDeleted =
        undeliveredMessagesService.deleteMessages(ADDRESS, QUEUE, Collections.singletonList("1"));

    assertThat(messageDeleted, is(0L));
  }

  @Test
  public void testDeleteInvalidMessage() {
    long messageDeleted =
        undeliveredMessagesService.deleteMessages(ADDRESS, QUEUE, Collections.singletonList("2"));

    assertThat(messageDeleted, is(0L));
  }

  @Test
  public void testRetryMessage()
      throws MBeanException, InstanceNotFoundException, ReflectionException {
    when(mockMBeanServer.invoke(
            objectName, RETRY_MESSAGE, new Object[] {1L}, new String[] {long.class.getName()}))
        .thenReturn(true);

    long messageResent =
        undeliveredMessagesService.resendMessages(ADDRESS, QUEUE, Collections.singletonList("1"));

    assertThat(messageResent, is(1L));
  }

  @Test
  public void testRetryMessageException()
      throws MBeanException, InstanceNotFoundException, ReflectionException {
    when(mockMBeanServer.invoke(
            objectName, RETRY_MESSAGE, new Object[] {1L}, new String[] {long.class.getName()}))
        .thenThrow(InstanceNotFoundException.class);

    long messageResent =
        undeliveredMessagesService.resendMessages(ADDRESS, QUEUE, Collections.singletonList("1"));

    assertThat(messageResent, is(0L));
  }

  @Test
  public void testRetryInvalidMessage() {
    long messageResent =
        undeliveredMessagesService.resendMessages(ADDRESS, QUEUE, Collections.singletonList("2"));

    assertThat(messageResent, is(0L));
  }

  @Test
  public void testDestroy() {
    undeliveredMessagesService.destroy();
  }

  private CompositeData[] createCompositeData(Object itemValue) throws OpenDataException {
    CompositeData[] compositeDatas = new CompositeDataSupport[1];
    String[] keys = {"itemName", "body"};
    /* The first 5 bytes are removed because artemis adds unreadable characters to the
    beginning of the message body. */
    Object[] values = {"itemValue", itemValue};
    CompositeType compositeType =
        new CompositeType(
            "typeName",
            "description",
            keys,
            new String[] {"itemDescription", "messageBodyDescription"},
            new OpenType[] {SimpleType.STRING, ArrayType.getPrimitiveArrayType(byte[].class)});
    compositeDatas[0] = new CompositeDataSupport(compositeType, keys, values);
    return compositeDatas;
  }

  private void validateCompositeData(List<CompositeData> undeliveredMessages) {
    assertThat(undeliveredMessages.size(), is(1));
    assertThat(undeliveredMessages.get(0), is(notNullValue()));
    assertThat(undeliveredMessages.get(0).getCompositeType(), is(notNullValue()));
    assertThat(undeliveredMessages.get(0).getCompositeType().containsKey("itemName"), is(true));
    assertThat(undeliveredMessages.get(0).getCompositeType().containsKey("body"), is(true));
    assertThat(
        undeliveredMessages.get(0).getCompositeType().getDescription("itemName"),
        is("itemDescription"));
    assertThat(
        undeliveredMessages.get(0).getCompositeType().getDescription("body"),
        is("messageBodyDescription"));
    assertThat(undeliveredMessages.get(0).getCompositeType().getDescription(), is("description"));

    assertThat(undeliveredMessages.get(0).get("itemName"), is("itemValue"));
  }
}
