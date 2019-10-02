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
package ddf.metrics.interceptor;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;

/**
 * CXF out interceptor used to capture HTTP message latency metrics.
 *
 * <p>The {@link MetricsInInterceptor} records the time that a message is received and the out
 * interceptor calculates the total time and saves the result.
 *
 * @author willisod
 */
public class MetricsOutInterceptor extends AbstractMetricsInterceptor {

  private OneWayMessageEndingInterceptor ending = new OneWayMessageEndingInterceptor();

  public MetricsOutInterceptor(MeterRegistryService meterRegistryService) {
    super(Phase.SEND, meterRegistryService);
  }

  /**
   * Handle the out message, including one way messages. Methods on {@link
   * AbstractMetricsInterceptor} are used to get the numbers and save them to YAMMER objects.
   */
  @Override
  public void handleMessage(Message message) throws Fault {

    Exchange ex = message.getExchange();

    if (Boolean.TRUE.equals(message.get(Message.PARTIAL_RESPONSE_MESSAGE))) {
      return;
    }

    if (isClient(message)) {
      if (ex.isOneWay()) {
        message.getInterceptorChain().add(ending);
      }

      beginHandlingMessage(ex);

    } else {
      endHandlingMessage(ex);
    }
  }

  OneWayMessageEndingInterceptor getEndingInterceptor() {

    return ending;
  }

  public class OneWayMessageEndingInterceptor extends AbstractPhaseInterceptor<Message> {

    public OneWayMessageEndingInterceptor() {
      super(Phase.PREPARE_SEND_ENDING);
    }

    public void handleMessage(Message message) throws Fault {
      Exchange ex = message.getExchange();
      endHandlingMessage(ex);
    }

    public void handleFault(Message message) throws Fault {
      Exchange ex = message.getExchange();
      endHandlingMessage(ex);
    }
  }
}
