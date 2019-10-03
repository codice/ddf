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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.common;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import net.opengis.wfs.v_1_1_0.DescribeFeatureTypeType;
import net.opengis.wfs.v_1_1_0.GetCapabilitiesType;
import net.opengis.wfs.v_1_1_0.GetFeatureType;
import net.opengis.wfs.v_1_1_0.WFSCapabilitiesType;
import org.apache.ws.commons.schema.XmlSchema;
import org.codice.ddf.spatial.ogc.wfs.catalog.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;

/** JAX-RS Interface to define a WFS server. */
@Path("/")
public interface Wfs {

  /** GetCapabilites - HTTP GET */
  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  WFSCapabilitiesType getCapabilities(@QueryParam("") GetCapabilitiesRequest request)
      throws WfsException;

  /** GetCapabilites - HTTP POST */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  WFSCapabilitiesType getCapabilities(GetCapabilitiesType getCapabilitesRequest)
      throws WfsException;

  /** DescribeFeatureType - HTTP GET */
  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  XmlSchema describeFeatureType(@QueryParam("") DescribeFeatureTypeRequest request)
      throws WfsException;

  /** DescribeFeatureType - HTTP POST */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  XmlSchema describeFeatureType(DescribeFeatureTypeType describeFeatureRequest) throws WfsException;

  /** GetFeature */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  WfsFeatureCollection getFeature(GetFeatureType getFeature) throws WfsException;
}
