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
package org.codice.ddf.spatial.ogc.csw.catalog.common.transaction;

import ddf.catalog.data.Metacard;
import java.io.Serializable;
import java.util.Map;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction;

/** An UpdateActionImpl represents a single update action within a CSW transaction. */
public class UpdateActionImpl implements UpdateAction {

  private String typeName;

  private String handle;

  private Metacard metacard;

  private QueryConstraintType queryConstraintType;

  private Map<String, Serializable> recordProperties;

  private Map<String, String> prefixToUriMappings;

  /**
   * Constructs an UpdateActionImpl with the specified metacard, typeName, and handle. This
   * UpdateActionImpl will replace the existing metacard having the same ID as {@code metacard} with
   * {@code metacard}.
   *
   * <p>If an error occurs while processing this update action, {@code handle} will be included in
   * the exception report response so the specific action within the transaction that caused the
   * error can be identified.
   *
   * @param metacard the updated metacard that will replace the existing metacard with the same ID
   * @param typeName the type of record being updated, such as csw:Record
   * @param handle the name to associate with this update action
   */
  public UpdateActionImpl(Metacard metacard, String typeName, String handle) {
    this.typeName = typeName;
    this.handle = handle;
    this.metacard = metacard;
  }

  /**
   * Constructs an UpdateActionImpl with the specified map of attributes to new values to update in
   * the metacard, typeName, handle, constraint, and map of XML namespace prefixes to their
   * respective URIs. {@code prefixToUriMappings} should contain the prefix to URI mappings declared
   * in the transaction request XML.
   *
   * <p>This UpdateActionImpl will update all metacards matching {@code constraint} by updating
   * their attributes according to the attribute names and values specified in {@code
   * recordProperties}.
   *
   * <p>If an error occurs while processing this update action, {@code handle} will be included in
   * the exception report response so the specific action within the transaction that caused the
   * error can be identified.
   *
   * @param recordProperties the map of attribute names to update to their new values
   * @param typeName the type of record being updated, such as csw:Record
   * @param handle the name to associate with this update action
   * @param constraint the {@link QueryConstraintType} that specifies which metacards this update
   *     will be applied to
   * @param prefixToUriMappings the map that contains the XML namespace prefix to URI mappings
   *     declared in the transaction request XML
   */
  public UpdateActionImpl(
      Map<String, Serializable> recordProperties,
      String typeName,
      String handle,
      QueryConstraintType constraint,
      Map<String, String> prefixToUriMappings) {
    this.typeName = typeName;
    this.handle = handle;
    queryConstraintType = constraint;
    this.prefixToUriMappings = prefixToUriMappings;
    this.recordProperties = recordProperties;
  }

  @Override
  public Metacard getMetacard() {
    return metacard;
  }

  @Override
  public QueryConstraintType getConstraint() {
    return queryConstraintType;
  }

  @Override
  public Map<String, Serializable> getRecordProperties() {
    return recordProperties;
  }

  @Override
  public Map<String, String> getPrefixToUriMappings() {
    return prefixToUriMappings;
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public String getHandle() {
    return handle;
  }
}
