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
import org.codice.ddf.spatial.ogc.csw.catalog.api.GetCapabilitiesRequest;

/**
 * JAX-RS Parameter Bean Class for the GetCapabilities request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 */
public class GetCapabilitiesRequestImpl extends CswRequestImpl implements GetCapabilitiesRequest {

  // The following parameters are optional for the GetCapabilities Request
  private String acceptVersions;

  private String sections;

  private String updateSequence;

  private String acceptFormats;

  public GetCapabilitiesRequestImpl() {
    super(CswConstants.GET_CAPABILITIES);
  }

  public GetCapabilitiesRequestImpl(String service) {
    this();
    setService(service);
  }

  @Override
  public String getAcceptVersions() {
    return acceptVersions;
  }

  @Override
  public void setAcceptVersions(String acceptVersions) {
    this.acceptVersions = acceptVersions;
  }

  @Override
  public String getSections() {
    return sections;
  }

  @Override
  public void setSections(String sections) {
    this.sections = sections;
  }

  @Override
  public String getUpdateSequence() {
    return updateSequence;
  }

  @Override
  public void setUpdateSequence(String updateSequence) {
    this.updateSequence = updateSequence;
  }

  @Override
  public String getAcceptFormats() {
    return acceptFormats;
  }

  @Override
  public void setAcceptFormats(String acceptFormats) {
    this.acceptFormats = acceptFormats;
  }
}
