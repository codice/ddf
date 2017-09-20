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
package ddf.catalog.data.types;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Location {

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the altitude of the {@link
   * ddf.catalog.resource.Resource} of the {@link Metacard}. <br>
   */
  String ALTITUDE = "location.altitude-meters";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the country code of the {@link
   * ddf.catalog.resource.Resource} of the {@link Metacard}. <br>
   */
  String COUNTRY_CODE = "location.country-code";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the Coordinate Reference System (CRS) of
   * the {@link ddf.catalog.resource.Resource} of the {@link Metacard}. <br>
   */
  String COORDINATE_REFERENCE_SYSTEM_CODE = "location.crs-code";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the CRS name of the {@link
   * ddf.catalog.resource.Resource} of the {@link Metacard}. <br>
   */
  String COORDINATE_REFERENCE_SYSTEM_NAME = "location.crs-name";
}
