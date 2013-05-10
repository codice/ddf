/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.metrics.interceptor;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.log4j.Logger;

import com.yammer.metrics.Histogram;
import com.yammer.metrics.JmxReporter;
import com.yammer.metrics.MetricRegistry;

/**
 * This class is extended by the metrics interceptors used for capturing round
 * trip message latency.
 * 
 * @author willisod
 * 
 */
public abstract class AbstractMetricsInterceptor extends
        AbstractPhaseInterceptor<Message> {

    private static final Logger LOGGER = Logger
            .getLogger(AbstractMetricsInterceptor.class);

    private static final String REGISTRY_NAME = "ddf.metrics.services";
    private static final String HISTOGRAM_NAME = "Latency";

    private static final MetricRegistry metrics = new MetricRegistry(
            REGISTRY_NAME);
    private static final JmxReporter reporter = JmxReporter
            .forRegistry(metrics).build();

    final Histogram messageLatency;

    /**
     * Constructor to pass the phase to {@code AbstractPhaseInterceptor} and
     * creates a new histogram.
     * 
     * @param phase
     */
    public AbstractMetricsInterceptor(String phase) {

        super(phase);

        messageLatency = metrics.histogram(MetricRegistry.name(HISTOGRAM_NAME));

        reporter.start();
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
            increaseCounter(ex, ltr);
        } else {
            LOGGER.info("can't get the MessageHandling Info");
        }
    }

    private void increaseCounter(Exchange ex, LatencyTimeRecorder ltr) {
        messageLatency.update(ltr.getLatencyTime());
    }

}
