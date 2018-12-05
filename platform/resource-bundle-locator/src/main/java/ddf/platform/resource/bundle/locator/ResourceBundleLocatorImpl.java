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
package ddf.platform.resource.bundle.locator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceBundleLocatorImpl implements ResourceBundleLocator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceBundleLocatorImpl.class);

  private File resourceBundleBaseDir;

  @Override
  public ResourceBundle getBundle(String baseName) throws IOException {
    return getBundle(baseName, Locale.getDefault());
  }

  @Override
  public ResourceBundle getBundle(String baseName, Locale locale) throws IOException {
    File resourceBundleDir = resourceBundleBaseDir;
    if (resourceBundleDir == null) {
      resourceBundleDir = new File(String.format("%s/etc/i18n/", System.getProperty("ddf.home")));
    }

    URL[] urls = {resourceBundleDir.toURI().toURL()};

    try (URLClassLoader loader =
        AccessController.doPrivileged(
            (PrivilegedAction<URLClassLoader>) () -> new URLClassLoader(urls))) {
      return ResourceBundle.getBundle(baseName, locale, loader);
    }
  }

  public void setResourceBundleBaseDir(String resourceBundleBaseDir) {
    this.resourceBundleBaseDir = new File(resourceBundleBaseDir);
  }
}
