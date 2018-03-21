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
package org.codice.ddf.broker.security;

import java.util.List;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.codice.ddf.broker.security.api.BrokerMessageInterceptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerAuthenticationPlugin implements ActiveMQServerPlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(BrokerAuthenticationPlugin.class);
  private BrokerMessageInterceptor brokerMessageInterceptor;

  @Override
  public void beforeSend(
      ServerSession session,
      Transaction tx,
      Message message,
      boolean direct,
      boolean noAutoCreateQueue) {
    try {
      if (brokerMessageInterceptor == null) {
        BundleContext bundleContext = getBundleContext();
        brokerMessageInterceptor =
            bundleContext.getService(
                ((List<ServiceReference<BrokerMessageInterceptor>>)
                        bundleContext.getServiceReferences(
                            BrokerMessageInterceptor.class, "(name=subjectInjectorPlugin)"))
                    .get(0));
      }
      brokerMessageInterceptor.handleMessage(session, tx, message, direct, noAutoCreateQueue);
    } catch (InvalidSyntaxException e) {
      LOGGER.warn(
          "Could retrieve the Subject Injector Plugin, subject will not be correctly applied to the message.",
          e);
    }
  }

  private BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(BrokerMessageInterceptor.class).getBundleContext();
  }
}
