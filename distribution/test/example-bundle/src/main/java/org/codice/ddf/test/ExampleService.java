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
package org.codice.ddf.test;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExampleService.class);

  public static final String PID = "org.codice.ddf.test.ExampleService.pid";

  public static final String EXAMPLE_PROP_NAME = "exampleProp";

  public static final String DEFAULT_EXAMPLE_PROP_VALUE = "defaultExamplePropValue";

  private String prop;

  private BundleContext bundleContext;

  public ExampleService(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  public void setExampleProp(String value) {
    prop = value;
    if (DEFAULT_EXAMPLE_PROP_VALUE.equals(value)) {
      LOGGER.info("exampleProp of ExampleService set to default value.");
    } else {
      LOGGER.info("exampleProp of ExampleService updated to new value: {}", value);
    }
  }

  public String getExampleProp() {
    return prop;
  }

  public String getBundleLocation() {
    return bundleContext.getBundle().getLocation();
  }
}
