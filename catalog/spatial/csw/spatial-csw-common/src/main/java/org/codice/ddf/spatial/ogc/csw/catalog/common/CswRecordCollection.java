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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.resource.Resource;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ResultType;

/**
 * This class represents the domain object for the list of metacards corresponding to the list of
 * CSW records returned in a GetRecords request.
 *
 * @author rodgersh
 */
public class CswRecordCollection {

  private GetRecordsType request;

  private List<Metacard> cswRecords = new ArrayList<>();

  private long numberOfRecordsReturned;

  private long numberOfRecordsMatched;

  private boolean isById;

  private ElementSetType elementSetType;

  private List<QName> elementName;

  private String outputSchema;

  private SourceResponse sourceResponse;

  private String mimeType;

  private int startPosition;

  private ResultType resultType;

  private boolean doWriteNamespaces;

  private Resource resource;

  private Map<String, Serializable> resourceProperties = new HashMap<>();

  /**
   * Retrieves the request made that generated this set of CSW Records, if applicable
   *
   * @return the {@link GetRecordsType} request
   */
  public GetRecordsType getRequest() {
    return request;
  }

  /**
   * Sets the request used to generate this list of records
   *
   * @param request A {@link GetRecordsType} used to generate this request
   */
  public void setRequest(GetRecordsType request) {
    this.request = request;
  }

  /**
   * Retrieves the list of metacards built from the CSW Records returned in a GetRecordsResponse.
   *
   * @return
   */
  public List<Metacard> getCswRecords() {
    return cswRecords;
  }

  /**
   * Sets the list of metacards built from the CSW Records returned in a GetRecordsResponse.
   *
   * @param cswRecords
   */
  public void setCswRecords(List<Metacard> cswRecords) {
    this.cswRecords = cswRecords;
  }

  public long getNumberOfRecordsReturned() {
    return numberOfRecordsReturned;
  }

  public void setNumberOfRecordsReturned(long numberOfRecordsReturned) {
    this.numberOfRecordsReturned = numberOfRecordsReturned;
  }

  public long getNumberOfRecordsMatched() {
    return numberOfRecordsMatched;
  }

  public void setNumberOfRecordsMatched(long numberOfRecordsMatched) {
    this.numberOfRecordsMatched = numberOfRecordsMatched;
  }

  public boolean isById() {
    return isById;
  }

  public void setById(boolean isById) {
    this.isById = isById;
  }

  public ElementSetType getElementSetType() {
    return elementSetType;
  }

  public void setElementSetType(ElementSetType elementSetType) {
    this.elementSetType = elementSetType;
  }

  public List<QName> getElementName() {
    return elementName;
  }

  public void setElementName(List<QName> elementName) {
    this.elementName = elementName;
  }

  public String getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }

  public SourceResponse getSourceResponse() {
    return sourceResponse;
  }

  public void setSourceResponse(SourceResponse response) {
    this.sourceResponse = response;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public int getStartPosition() {
    return startPosition;
  }

  public void setStartPosition(int start) {
    this.startPosition = start;
  }

  public ResultType getResultType() {
    return this.resultType;
  }

  public void setResultType(ResultType resultType) {
    this.resultType = resultType;
  }

  public boolean isDoWriteNamespaces() {
    return doWriteNamespaces;
  }

  public void setDoWriteNamespaces(boolean doWriteNamespaces) {
    this.doWriteNamespaces = doWriteNamespaces;
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  public Resource getResource() {
    return resource;
  }

  public void setResourceProperties(Map<String, Serializable> resourceProperties) {
    this.resourceProperties = resourceProperties;
  }

  public Map<String, Serializable> getResourceProperties() {
    return this.resourceProperties;
  }
}
