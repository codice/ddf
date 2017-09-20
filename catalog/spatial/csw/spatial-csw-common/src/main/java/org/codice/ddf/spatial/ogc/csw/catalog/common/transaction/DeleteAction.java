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

import java.util.Map;
import net.opengis.cat.csw.v_2_0_2.DeleteType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;

/** A DeleteAction represents a single delete action within a CSW transaction. */
public class DeleteAction extends CswAction {
  private QueryConstraintType queryConstraintType;

  private Map<String, String> prefixToUriMappings;

  /**
   * Constructs a DeleteAction with a {@link DeleteType} and a map of XML namespace prefixes to
   * their respective URIs. The map should contain the prefix to URI mappings declared in the
   * transaction request XML.
   *
   * <p>If an error occurs while processing this delete action, {@link DeleteType#handle} will be
   * included in the exception report response so the specific action within the transaction that
   * caused the error can be identified.
   *
   * @param deleteType the {@code DeleteType} representing the delete action
   * @param prefixToUriMappings the map that contains the XML namespace prefix to URI mappings
   *     declared in the transaction request XML
   */
  public DeleteAction(DeleteType deleteType, Map<String, String> prefixToUriMappings) {
    super(
        StringUtils.defaultIfEmpty(deleteType.getTypeName(), CswConstants.CSW_RECORD),
        StringUtils.defaultIfEmpty(deleteType.getHandle(), ""));
    queryConstraintType = deleteType.getConstraint();
    this.prefixToUriMappings = prefixToUriMappings;
  }

  public QueryConstraintType getConstraint() {
    return queryConstraintType;
  }

  public Map<String, String> getPrefixToUriMappings() {
    return prefixToUriMappings;
  }
}
