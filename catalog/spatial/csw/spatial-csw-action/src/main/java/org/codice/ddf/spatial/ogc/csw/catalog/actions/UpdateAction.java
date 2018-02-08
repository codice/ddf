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
package org.codice.ddf.spatial.ogc.csw.catalog.actions;

import ddf.catalog.data.Metacard;
import java.io.Serializable;
import java.util.Map;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 *
 * <p>An UpdateAction represents a single update action within a CSW transaction.
 */
public interface UpdateAction {

  /**
   * Returns the metacard that will replace the existing metacard.
   *
   * @return the metacard that will replace the existing metacard.
   */
  Metacard getMetacard();

  /**
   * Returns the {@link QueryConstraintType} that specifies which metacards this update will be
   * applied to.
   *
   * @return the {@link QueryConstraintType} that specifies which metacards this update will be
   *     applied to.
   */
  QueryConstraintType getConstraint();

  /**
   * Returns the specified map of attributes to new values to update in the metacard.
   *
   * @return a map of attributes to new values to update in the metacard.
   */
  Map<String, Serializable> getRecordProperties();

  /**
   * Returns a map that contains the XML namespace prefix to URI mappings declared in the
   * transaction request XML
   *
   * @return a map that contains the XML namespace prefix to URI mappings declared in the
   *     transaction request XML
   */
  Map<String, String> getPrefixToUriMappings();

  /**
   * Returns the type of record being updated, such as csw:Record.
   *
   * @return the type of record being updated, such as csw:Record.
   */
  String getTypeName();

  /**
   * Returns a unique String used to identify this UpdateAction within a transaction.
   *
   * <p>If an error occurs while processing this update action, this String will be included in the
   * exception report response so the specific action within the transaction that caused the error
   * can be identified.
   *
   * @return unique String used to identify this UpdateAction within a transaction.
   */
  String getHandle();
}
