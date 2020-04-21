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
package org.codice.ddf.lib.metrics.registry;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service that exposing a Micrometer {@link MeterRegistry}. */
public class MeterRegistryServiceImpl implements MeterRegistryService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MeterRegistryServiceImpl.class);
  private MeterRegistry meterRegistry;

  /**
   * Creates a new MeterRegistryServiceImpl.
   *
   * @param meterRegistry the registry exposed by this service
   */
  public MeterRegistryServiceImpl(MeterRegistry meterRegistry) {
    LOGGER.debug("Starting Meter Registry Service...");
    this.meterRegistry = meterRegistry;
  }

  @Override
  public MeterRegistry getMeterRegistry() {
    LOGGER.debug("Getting meter registry");
    return meterRegistry;
  }
}
