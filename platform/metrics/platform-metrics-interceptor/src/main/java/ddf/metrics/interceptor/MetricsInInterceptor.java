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
import org.apache.cxf.phase.Phase;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;

/**
 * CXF in interceptor used to capture HTTP message latency metrics.
 *
 * <p>This interceptor records the time that a message is received and the {@link
 * MetricsOutInterceptor} calculates the total time and saves the result.
 *
 * @author willisod
 */
public class MetricsInInterceptor extends AbstractMetricsInterceptor {

  static final String TIME_IN = "TimeIn";

  public MetricsInInterceptor(MeterRegistryService meterRegistryService) {
    super(Phase.RECEIVE, meterRegistryService);
  }

  @Override
  public void handleMessage(Message message) throws Fault {

    Exchange ex = message.getExchange();

    if (null == ex) {
      return;
    }

    if (isClient(message)) {
      if (!ex.isOneWay()) {
        endHandlingMessage(ex);
      }
    } else {
      beginHandlingMessage(ex);
    }
  }
}
