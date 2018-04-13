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
package org.codice.ddf.branding;

import java.io.IOException;
import java.util.List;

public interface BrandingRegistry {

  /**
   * This method returns the name portion of the name & version of the product, as defined by the
   * highest priority {@link BrandingPlugin} in the system.. E.g, if the full name is "DDF v1.0.0",
   * this will return "DDF"
   *
   * @return a String representing the name of the product or in the case where the name cannot be
   *     found, the String "DDF"
   */
  String getProductName();

  /**
   * This method takes in a {@link BrandingPlugin} method reference (a {@link
   * BrandingRegistry.BrandingMethod} and calls it on the highest priority {@link BrandingPlugin} in
   * the system.
   *
   * @return a String with the value of the {@link BrandingPlugin}'s method call or if the method
   *     call cannot be evaluated, an empty String ""
   */
  String getAttributeFromBranding(BrandingMethod supplier);

  /**
   * This method returns the version number portion of the name & version of the product, as defined
   * by the highest priority {@link BrandingPlugin} in the system.. E.g, if the full name is "DDF
   * v1.0.0", this will return "v1.0.0"
   *
   * @return a String representing the version number of the product or in the case where the
   *     version number cannot be found, an empty String ""
   */
  String getProductVersion();

  /**
   * Sets the {@link List} of {@link BrandingPlugin}s on the {@link BrandingRegistry}
   *
   * @param brandingPlugins list of branding plugins associated with the system
   */
  void setBrandingPlugins(List<BrandingPlugin> brandingPlugins);

  /**
   * Gets the {@link List} of {@link BrandingPlugin}s from the {@link BrandingRegistry}
   *
   * @return the list of branding plugins associated with the system
   */
  List<BrandingPlugin> getBrandingPlugins();

  /**
   * A functional interface for {@link BrandingPlugin} methods (cannot use {@link
   * java.util.function.Function} since some of them throw Exceptions)
   */
  @FunctionalInterface
  interface BrandingMethod {
    String apply(BrandingPlugin b) throws IOException;
  }
}
