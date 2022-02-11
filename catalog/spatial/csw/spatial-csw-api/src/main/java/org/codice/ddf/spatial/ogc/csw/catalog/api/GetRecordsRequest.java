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
package org.codice.ddf.spatial.ogc.csw.catalog.api;

import java.math.BigInteger;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;

public interface GetRecordsRequest extends CswRequest {
  String getVersion();

  void setVersion(String version);

  String getRequestId();

  void setRequestId(String requestId);

  String getNamespace();

  void setNamespace(String namespace);

  String getResultType();

  void setResultType(String resultType);

  String getOutputFormat();

  void setOutputFormat(String outputFormat);

  String getOutputSchema();

  void setOutputSchema(String outputSchema);

  BigInteger getStartPosition();

  void setStartPosition(BigInteger startPosition);

  BigInteger getMaxRecords();

  void setMaxRecords(BigInteger maxRecords);

  String getTypeNames();

  void setTypeNames(String typeNames);

  String getElementName();

  void setElementName(String elementName);

  String getElementSetName();

  void setElementSetName(String elementSetName);

  String getConstraintLanguage();

  void setConstraintLanguage(String constraintLanguage);

  String getConstraint();

  void setConstraint(String constraint);

  String getSortBy();

  void setSortBy(String sortBy);

  Boolean getDistributedSearch();

  void setDistributedSearch(Boolean distributedSearch);

  BigInteger getHopCount();

  void setHopCount(BigInteger hopCount);

  String getResponseHandler();

  void setResponseHandler(String responseHandler);

  /**
   * Convert the KVP values into a GetRecordsType, validates format of fields and enumeration
   * constraints required to meet the schema requirements of the GetRecordsType. No further
   * validation is done at this point
   *
   * @return GetRecordsType representation of this key-value representation
   * @throws CswException An exception when some field cannot be converted to the equivalent
   *     GetRecordsType value
   */
  GetRecordsType get202RecordsType() throws CswException;
}
