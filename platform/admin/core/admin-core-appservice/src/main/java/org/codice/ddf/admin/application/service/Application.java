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
package org.codice.ddf.admin.application.service;

import java.util.Set;
import org.apache.karaf.features.BundleInfo;

/**
 * @deprecated going away in a future release. This class defines an application within DDF. An
 *     application is a collection of bundles.
 */
@Deprecated
public interface Application {

  /**
   * Name describing the application.
   *
   * @return name
   */
  @Deprecated
  String getName();

  /**
   * Short description of the application.
   *
   * @return description
   */
  @Deprecated
  String getDescription();

  /**
   * Gets all of the bundles that this application contains.
   *
   * @return Set of the bundles located within the application.
   * @throws ApplicationServiceException
   */
  @Deprecated
  Set<BundleInfo> getBundles();
}
