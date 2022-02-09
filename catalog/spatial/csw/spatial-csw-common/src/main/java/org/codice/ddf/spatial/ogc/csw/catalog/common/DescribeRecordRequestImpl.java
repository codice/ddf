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

import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.DescribeRecordRequest;

/**
 * JAX-RS Parameter Bean Class for the DescribeRecord request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 */
public class DescribeRecordRequestImpl extends CswRequestImpl implements DescribeRecordRequest {

  private String namespace;

  // The following parameters are optional for the DescribeRecord Request
  private String typeName;

  private String outputFormat;

  private String schemaLanguage;

  public DescribeRecordRequestImpl() {
    super(CswConstants.DESCRIBE_RECORD);
  }

  public DescribeRecordRequestImpl(String service, String version) {
    this();
    setService(service);
    setVersion(version);
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public void setTypeName(String typeName) {
    this.typeName = typeName;
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
  public String getSchemaLanguage() {
    return schemaLanguage;
  }

  @Override
  public void setSchemaLanguage(String schemaLanguage) {
    this.schemaLanguage = schemaLanguage;
  }
}
