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

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to register interceptors with the cxf InterceptorProviders.
 *
 * @author willisod
 */
public class MetricsFeature extends AbstractFeature {

  private final MetricsInInterceptor metricsIn;

  private final MetricsOutInterceptor metricsOut;

  private static final String CLASS_NAME = "MetricsFeature";

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsFeature.class);

  public MetricsFeature(MeterRegistryService meterRegistryService) {
    metricsIn = new MetricsInInterceptor(meterRegistryService);
    metricsOut = new MetricsOutInterceptor(meterRegistryService);
  }

  @Override
  protected void initializeProvider(InterceptorProvider provider, Bus bus) {
    LOGGER.debug("ENTERING: {}.initializeProvider", CLASS_NAME);
    provider.getInInterceptors().add(metricsIn);
    provider.getOutInterceptors().add(metricsOut);
  }
}
