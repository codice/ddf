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
package org.codice.ddf.broker.security.api;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.transaction.Transaction;

/**
 * This class abstracts the implementation of the ActiveMQServerPlugin away from the Artemis
 * Fragment. It is intended to be used in tandem with an implementation of the ActiveMQServerPlugin.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface BrokerMessageInterceptor {

  /**
   * This message accepts a session, transaction, message, direct boolean and a noAutoCreateQueue
   * boolean. Using that information, it can modify the message. For example, it can add properties
   * to the message.
   *
   * @param session
   * @param tx
   * @param message
   * @param direct
   * @param noAutoCreateQueue
   */
  void handleMessage(
      ServerSession session,
      Transaction tx,
      Message message,
      boolean direct,
      boolean noAutoCreateQueue);
}
