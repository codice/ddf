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

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.resource.Resource;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ResultType;

public interface CswRecordCollection {
  /**
   * Retrieves the request made that generated this set of CSW Records, if applicable
   *
   * @return the {@link GetRecordsType} request
   */
  GetRecordsType getRequest();

  /**
   * Sets the request used to generate this list of records
   *
   * @param request A {@link GetRecordsType} used to generate this request
   */
  void setRequest(GetRecordsType request);

  /**
   * Retrieves the list of metacards built from the CSW Records returned in a GetRecordsResponse.
   *
   * @return
   */
  List<Metacard> getCswRecords();

  /**
   * Sets the list of metacards built from the CSW Records returned in a GetRecordsResponse.
   *
   * @param cswRecords
   */
  void setCswRecords(List<Metacard> cswRecords);

  long getNumberOfRecordsReturned();

  void setNumberOfRecordsReturned(long numberOfRecordsReturned);

  long getNumberOfRecordsMatched();

  void setNumberOfRecordsMatched(long numberOfRecordsMatched);

  boolean isById();

  void setById(boolean isById);

  ElementSetType getElementSetType();

  void setElementSetType(ElementSetType elementSetType);

  List<QName> getElementName();

  void setElementName(List<QName> elementName);

  String getOutputSchema();

  void setOutputSchema(String outputSchema);

  SourceResponse getSourceResponse();

  void setSourceResponse(SourceResponse response);

  String getMimeType();

  void setMimeType(String mimeType);

  int getStartPosition();

  void setStartPosition(int start);

  ResultType getResultType();

  void setResultType(ResultType resultType);

  boolean isDoWriteNamespaces();

  void setDoWriteNamespaces(boolean doWriteNamespaces);

  Resource getResource();

  void setResource(Resource resource);

  Map<String, Serializable> getResourceProperties();

  void setResourceProperties(Map<String, Serializable> resourceProperties);
}
