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
package org.codice.ddf.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemMetricsReporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SystemMetricsReporter.class);

  public SystemMetricsReporter() {
    LOGGER.debug("Adding JVM and system metrics to global registry.");
    new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
    new DiskSpaceMetrics(Paths.get(System.getProperty("ddf.home")).toFile())
        .bindTo(Metrics.globalRegistry);
    new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
    try (JvmHeapPressureMetrics jvmHeapPressureMetrics = new JvmHeapPressureMetrics()) {
      jvmHeapPressureMetrics.bindTo(Metrics.globalRegistry);
    }
    try (JvmGcMetrics jvmGcMetrics = new JvmGcMetrics()) {
      jvmGcMetrics.bindTo(Metrics.globalRegistry);
    }
    new JvmThreadMetrics().bindTo(Metrics.globalRegistry);

    new FileDescriptorMetrics().bindTo(Metrics.globalRegistry);
    new ProcessorMetrics().bindTo(Metrics.globalRegistry);
    new UptimeMetrics().bindTo(Metrics.globalRegistry);

    LOGGER.debug("Adding hostname to default tags.");
    Metrics.globalRegistry
        .config()
        .commonTags("host", System.getProperty("org.codice.ddf.system.hostname", "localhost"));
  }
}
