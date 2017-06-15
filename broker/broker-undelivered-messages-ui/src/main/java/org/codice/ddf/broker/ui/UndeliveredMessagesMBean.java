/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.broker.ui;

import java.util.List;

import javax.management.openmbean.CompositeData;

/**
 * Interface for the Undelivered Messages MBean. Allows exposing operations to perform on a message
 * broker's queues or topics and is meant to abstract away the details of various broker's Mbean
 * implementations.
 */
public interface UndeliveredMessagesMBean {

    /**
     * Gets messages from a queue/topic given its address and module.
     *
     * @param address Address name of the queue/topic
     * @param queue   Queue name of the queue/topic
     * @return List of messages in the form of CompositeData
     */
    List<CompositeData> getMessages(String address, String queue);

    /**
     * Resends messages from a queue/topic given its address, module and list of message ids of the
     * messages to resend.
     *
     * @param address    Address name of the queue/topic
     * @param queue      Queue name of the queue/topic
     * @param messageIds List of message ids of each message to resend
     * @return The number of messages that are resent
     */
    long resendMessages(String address, String queue, List<String> messageIds);

    /**
     * Deletes messages from a queue/topic given its address, module and list of message ids of the
     * messages to delete.
     *
     * @param address    Address name of the queue/topic
     * @param queue      Queue name of the queue/topic
     * @param messageIds List of message ids of each message to delete
     * @return The number of messages that are deleted
     */
    long deleteMessages(String address, String queue, List<String> messageIds);
}
