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
package org.codice.ddf.validator.query.unsupportedattributes;

import ddf.platform.resource.bundle.locator.ResourceBundleLocator;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageFormatSupplier implements Supplier<String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageFormatSupplier.class);

  private static final String INTRIGUE_BASE_NAME = "IntrigueBundle";

  private static final String DEFAULT_MESSAGE_FORMAT =
      "The field \"{attribute}\" is not supported by the {sources} Source(s)";

  private ResourceBundleLocator resourceBundleLocator;

  public MessageFormatSupplier(ResourceBundleLocator resourceBundleLocator) {
    this.resourceBundleLocator = resourceBundleLocator;
  }

  @Override
  public String get() {
    return AccessController.doPrivileged(
        (PrivilegedAction<String>)
            () -> {
              try {
                return resourceBundleLocator
                    .getBundle(INTRIGUE_BASE_NAME)
                    .getString("validation.attribute.unsupported");
              } catch (IOException e) {
                LOGGER.debug(
                    "Failed getting {} resource bundle, using default \"{}\"",
                    INTRIGUE_BASE_NAME,
                    DEFAULT_MESSAGE_FORMAT);
                return DEFAULT_MESSAGE_FORMAT;
              }
            });
  }
}
