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

import org.codice.ddf.spatial.ogc.csw.catalog.api.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.api.GetRecordByIdRequest;

/**
 * JAX-RS Parameter Bean Class for the GetRecordById request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 */
public class GetRecordByIdRequestImpl extends CswRequestImpl implements GetRecordByIdRequest {

  private String id;

  private String elementSetName;

  private String outputFormat;

  private String outputSchema;

  public GetRecordByIdRequestImpl() {
    super(CswConstants.GET_RECORD_BY_ID);
  }

  public GetRecordByIdRequestImpl(String service) {
    this();
    setService(service);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getElementSetName() {
    return elementSetName;
  }

  @Override
  public void setElementSetName(String elementSetName) {
    this.elementSetName = elementSetName;
  }

  @Override
  public String getOutputFormat() {
    return outputFormat;
  }

  @Override
  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  @Override
  public String getOutputSchema() {
    return outputSchema;
  }

  @Override
  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }
}
