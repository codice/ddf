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

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;

/**
 * This class is extended by the METRICS interceptors used for capturing round trip message latency.
 *
 * @author willisod
 */
public abstract class AbstractMetricsInterceptor extends AbstractPhaseInterceptor<Message> {

  private static final String REGISTRY_NAME = "ddf.METRICS.services";

  private static final String METRICS_PREFIX = "ddf.platform";

  private static final String HISTOGRAM_NAME = "latency";

  private final MeterRegistry meterRegistry;

  final DistributionSummary messageLatency;

  /**
   * Constructor to pass the phase to {@code AbstractPhaseInterceptor} and creates a new histogram.
   *
   * @param phase
   */
  public AbstractMetricsInterceptor(String phase, MeterRegistryService meterRegistryService) {
    super(phase);

    meterRegistry = meterRegistryService.getMeterRegistry();
    messageLatency = meterRegistry.summary(METRICS_PREFIX + "." + HISTOGRAM_NAME);
  }

  protected boolean isClient(Message msg) {
    return msg == null ? false : Boolean.TRUE.equals(msg.get(Message.REQUESTOR_ROLE));
  }

  protected void beginHandlingMessage(Exchange ex) {

    if (null == ex) {
      return;
    }

    LatencyTimeRecorder ltr = ex.get(LatencyTimeRecorder.class);

    if (null != ltr) {
      ltr.beginHandling();
    } else {
      ltr = new LatencyTimeRecorder();
      ex.put(LatencyTimeRecorder.class, ltr);
      ltr.beginHandling();
    }
  }

  protected void endHandlingMessage(Exchange ex) {

    if (null == ex) {
      return;
    }

    LatencyTimeRecorder ltr = ex.get(LatencyTimeRecorder.class);

    if (null != ltr) {
      ltr.endHandling();
      increaseCounter(ltr);
    }
  }

  private void increaseCounter(LatencyTimeRecorder ltr) {
    messageLatency.record(ltr.getLatencyTime());
  }
}
