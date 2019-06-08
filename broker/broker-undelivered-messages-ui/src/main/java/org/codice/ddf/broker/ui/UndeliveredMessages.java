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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndeliveredMessages implements UndeliveredMessagesMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(UndeliveredMessages.class);

  private static final String M_BEAN_NAME = ":service=UndeliveredMessages";

  private static final String GET_MESSAGES_OPERATION = "browse";

  private static final String RESEND_MESSAGE_OPERATION = "retryMessage";

  private static final String DELETE_MESSAGE_OPERATION = "removeMessage";

  private final MBeanServer mBeanServer;

  private ObjectName undeliveredMessagesObjectName;

  public UndeliveredMessages(MBeanServer mBeanServer) {
    this.mBeanServer = mBeanServer;
    registerMbean();
  }

  @Override
  public List<CompositeData> getMessages(String address, String queue) {
    List<CompositeData> undeliveredMessages = new ArrayList<>();
    Object compositeDatas =
        invokeMbean(
            createArtemisObjectName(address, queue),
            GET_MESSAGES_OPERATION,
            new Object[] {""},
            new String[] {String.class.getName()});
    if (!(compositeDatas instanceof CompositeData[])) {
      return undeliveredMessages;
    }
    CompositeData[] messages = (CompositeData[]) compositeDatas;
    for (CompositeData message : messages) {
      Set<String> setMessageKeys = message.getCompositeType().keySet();
      String[] messageKeysArray = setMessageKeys.toArray(new String[setMessageKeys.size()]);
      Object[] messageValues = message.getAll(messageKeysArray);
      CompositeType messageCompositeType = message.getCompositeType();

      String[] itemDescription = new String[messageKeysArray.length];
      OpenType<?>[] itemTypes = new OpenType[messageKeysArray.length];
      try {
        for (int i = 0; i < messageKeysArray.length; i++) {
          String messageKey = messageKeysArray[i];
          itemDescription[i] = messageCompositeType.getDescription(messageKey);
          if ("body".equals(messageKeysArray[i])) {
            byte[] messageBodyBytes = (byte[]) message.get("body");
            // Remove unprintable characters from the beginning of the string
            messageValues[i] =
                removeNullCharacters(
                    new String(
                        Arrays.copyOfRange(messageBodyBytes, 5, messageBodyBytes.length),
                        StandardCharsets.UTF_8));

            itemTypes[i] = SimpleType.STRING;
          } else {
            itemTypes[i] = messageCompositeType.getType(messageKey);
          }
        }

        undeliveredMessages.add(
            new CompositeDataSupport(
                new CompositeType(
                    messageCompositeType.getTypeName(),
                    messageCompositeType.getDescription(),
                    messageKeysArray,
                    itemDescription,
                    itemTypes),
                messageKeysArray,
                messageValues));
      } catch (OpenDataException e) {
        LOGGER.warn(
            "Unable to retrieve messages from the broker. For more information, set "
                + "logging level to DEBUG.");
        LOGGER.debug("Unable to retrieve messages from the broker.", e);
      }
    }
    return undeliveredMessages;
  }

  @Override
  public long resendMessages(String address, String queue, List<String> messageIds) {
    return messageOperation(RESEND_MESSAGE_OPERATION, address, queue, messageIds);
  }

  @Override
  public long deleteMessages(String address, String queue, List<String> messageIds) {
    return messageOperation(DELETE_MESSAGE_OPERATION, address, queue, messageIds);
  }

  public long messageOperation(
      String operationName, String address, String queue, List<String> messageId) {
    return messageId
        .stream()
        .map(Long::valueOf)
        .map(
            id ->
                invokeMbean(
                    createArtemisObjectName(address, queue),
                    operationName,
                    new Object[] {id},
                    new String[] {long.class.getName()}))
        .filter(flag -> flag instanceof Boolean && (Boolean) flag)
        .count();
  }

  public void init() {
    LOGGER.debug("Starting " + getClass().getName() + " MBean.");
  }

  public void destroy() {
    try {
      if (undeliveredMessagesObjectName != null && mBeanServer != null) {
        mBeanServer.unregisterMBean(undeliveredMessagesObjectName);
        LOGGER.debug("Unregistered MBean: [{}]", undeliveredMessagesObjectName);
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Exception unregistering MBean: [{}]. For more information, set logging level "
              + "to DEBUG. ",
          undeliveredMessagesObjectName);
      LOGGER.debug("Exception unregistering MBean: [{}].", undeliveredMessagesObjectName, e);
    }
  }

  private String removeNullCharacters(String message) {
    return message.replace("\0", "");
  }

  private Object invokeMbean(
      ObjectName objectName,
      String operationName,
      Object[] argumentValues,
      String[] argumentTypes) {
    Object returnObject = null;
    if (mBeanServer.isRegistered(objectName)) {
      try {
        returnObject = mBeanServer.invoke(objectName, operationName, argumentValues, argumentTypes);
      } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
        LOGGER.warn(
            "Unable to invoke operation: {} on Mbean: [{}]. For more information, "
                + "set logging level to DEBUG. ",
            operationName,
            objectName);
        LOGGER.debug(
            "Unable to invoke operation: {} on Mbean: [{}].", operationName, objectName, e);
      }
    } else {
      LOGGER.warn(
          "Could not invoke operation: {} on Mbean: [{}]. Mbean server is not registered.",
          operationName,
          objectName);
    }
    return returnObject;
  }

  private void registerMbean() {
    try {
      undeliveredMessagesObjectName =
          new ObjectName(UndeliveredMessages.class.getName() + M_BEAN_NAME);
    } catch (MalformedObjectNameException e) {
      LOGGER.warn(
          "Unable to create MBean: [{}]. For more " + "information, set logging level to DEBUG.",
          undeliveredMessagesObjectName);
      LOGGER.debug("Unable to create MBean: [{}].", undeliveredMessagesObjectName, e);
    }
    if (mBeanServer == null) {
      LOGGER.warn(
          "Could not register MBean: [{}], MBean server is null.", undeliveredMessagesObjectName);
      return;
    }
    try {
      try {
        mBeanServer.registerMBean(this, undeliveredMessagesObjectName);
        LOGGER.info("Registered MBean under object name: {}", undeliveredMessagesObjectName);
      } catch (InstanceAlreadyExistsException e) {
        // Try to remove and re-register
        mBeanServer.unregisterMBean(undeliveredMessagesObjectName);
        mBeanServer.registerMBean(this, undeliveredMessagesObjectName);
        LOGGER.info("Re-registered MBean: [{}]", undeliveredMessagesObjectName);
      }
    } catch (MBeanRegistrationException
        | InstanceNotFoundException
        | InstanceAlreadyExistsException
        | NotCompliantMBeanException e) {
      LOGGER.warn(
          "Could not register MBean: [{}]. For more information, set " + "logging level to DEBUG.",
          undeliveredMessagesObjectName);
      LOGGER.debug("Could not register MBean: [{}].", undeliveredMessagesObjectName, e);
    }
  }

  private ObjectName createArtemisObjectName(String address, String queue) {
    try {
      return new ObjectName(
          "org.apache.activemq.artemis:broker=\"artemis\",component=addresses,address=\""
              + address
              + "\",subcomponent=queues,routing-type=\"anycast\",queue=\""
              + queue
              + "\"");
    } catch (MalformedObjectNameException e) {
      LOGGER.warn(
          "Unable to create the Artemis ObjectName, with the given the address: {}, and "
              + "queue name: {}. For more information, set logging level to DEBUG.",
          address,
          queue);
      LOGGER.debug(
          "Unable to create the Artemis ObjectName, with the given the address: {}, and "
              + "queue name: {}.",
          address,
          queue,
          e);
    }
    return null;
  }
}
