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
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswRecordCollection;

/**
 * This class represents the domain object for the list of metacards corresponding to the list of
 * CSW records returned in a GetRecords request.
 *
 * @author rodgersh
 */
public class CswRecordCollectionImpl implements CswRecordCollection {

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

  @Override
  public GetRecordsType getRequest() {
    return request;
  }

  @Override
  public void setRequest(GetRecordsType request) {
    this.request = request;
  }

  @Override
  public List<Metacard> getCswRecords() {
    return cswRecords;
  }

  @Override
  public void setCswRecords(List<Metacard> cswRecords) {
    this.cswRecords = cswRecords;
  }

  @Override
  public long getNumberOfRecordsReturned() {
    return numberOfRecordsReturned;
  }

  @Override
  public void setNumberOfRecordsReturned(long numberOfRecordsReturned) {
    this.numberOfRecordsReturned = numberOfRecordsReturned;
  }

  @Override
  public long getNumberOfRecordsMatched() {
    return numberOfRecordsMatched;
  }

  @Override
  public void setNumberOfRecordsMatched(long numberOfRecordsMatched) {
    this.numberOfRecordsMatched = numberOfRecordsMatched;
  }

  @Override
  public boolean isById() {
    return isById;
  }

  @Override
  public void setById(boolean isById) {
    this.isById = isById;
  }

  @Override
  public ElementSetType getElementSetType() {
    return elementSetType;
  }

  @Override
  public void setElementSetType(ElementSetType elementSetType) {
    this.elementSetType = elementSetType;
  }

  @Override
  public List<QName> getElementName() {
    return elementName;
  }

  @Override
  public void setElementName(List<QName> elementName) {
    this.elementName = elementName;
  }

  @Override
  public String getOutputSchema() {
    return outputSchema;
  }

  @Override
  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }

  @Override
  public SourceResponse getSourceResponse() {
    return sourceResponse;
  }

  @Override
  public void setSourceResponse(SourceResponse response) {
    this.sourceResponse = response;
  }

  @Override
  public String getMimeType() {
    return mimeType;
  }

  @Override
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  @Override
  public int getStartPosition() {
    return startPosition;
  }

  @Override
  public void setStartPosition(int start) {
    this.startPosition = start;
  }

  @Override
  public ResultType getResultType() {
    return this.resultType;
  }

  @Override
  public void setResultType(ResultType resultType) {
    this.resultType = resultType;
  }

  @Override
  public boolean isDoWriteNamespaces() {
    return doWriteNamespaces;
  }

  @Override
  public void setDoWriteNamespaces(boolean doWriteNamespaces) {
    this.doWriteNamespaces = doWriteNamespaces;
  }

  @Override
  public void setResource(Resource resource) {
    this.resource = resource;
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  @Override
  public void setResourceProperties(Map<String, Serializable> resourceProperties) {
    this.resourceProperties = resourceProperties;
  }

  @Override
  public Map<String, Serializable> getResourceProperties() {
    return this.resourceProperties;
  }
}
