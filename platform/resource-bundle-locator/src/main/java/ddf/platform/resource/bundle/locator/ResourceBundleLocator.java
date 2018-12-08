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

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Provides access to resource bundles for keyword internalization.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface ResourceBundleLocator {

  /**
   * Retrieves a resource bundle for a given properties file. By default it uses the default locale
   * for this instance of the java virtual machine
   *
   * @param baseName The base name of the resource bundle
   * @return A resource bundle for the given base name
   * @throws java.util.MissingResourceException if the resource bundle does not exist or if an error
   *     occurs while loading the file
   * @throws SecurityException if read file permissions are not granted for
   *     <ddf.home>/etc/i18n/<bundle_name>
   * @throws IOException if an error occurs while reading or parsing the file
   */
  ResourceBundle getBundle(String baseName) throws IOException;

  /**
   * Retrieves a resource bundle for a given properties file
   *
   * @param baseName The base name of the resource bundle
   * @param locale The locale of the desired resource bundle
   * @return A resource bundle for the given base name and locale
   * @throws java.util.MissingResourceException if the resource bundle does not exist or if an error
   *     occurs while loading the file
   * @throws SecurityException if read file permissions are not granted for
   *     <ddf.home>/etc/i18n/<bundle_name>
   * @throws IOException if an error occurs while reading or parsing the file
   */
  ResourceBundle getBundle(String baseName, Locale locale) throws IOException;
}
