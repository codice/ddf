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
package org.codice.ddf.spatial.ogc.wps.process.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.opengis.wps.v_2_0.DescribeProcess;
import net.opengis.wps.v_2_0.Dismiss;
import net.opengis.wps.v_2_0.ExecuteRequestType;
import net.opengis.wps.v_2_0.GetCapabilitiesType;
import net.opengis.wps.v_2_0.GetResult;
import net.opengis.wps.v_2_0.GetStatus;
import net.opengis.wps.v_2_0.ProcessOfferings;
import net.opengis.wps.v_2_0.StatusInfo;
import net.opengis.wps.v_2_0.WPSCapabilitiesType;

@Path("/")
public interface Wps {

  /**
   * GetCapabilities - HTTP GET http://hostname:port/path?service=WPS&request=GetCapabilities
   *
   * @param acceptVersions
   * @param sections
   * @param updateSequence
   * @param acceptFormats
   * @return WPSCapabilitiesType
   * @throws WpsException
   */
  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  WPSCapabilitiesType getCapabilities(
      @QueryParam("acceptVersions") String acceptVersions,
      @QueryParam("sections") String sections,
      @QueryParam("updateSequence") String updateSequence,
      @QueryParam("acceptFormats") String acceptFormats);

  /**
   * GetCapabilities - HTTP POST
   *
   * @param getCapabilitiesType
   * @return WPSCapabilitiesType
   * @throws WpsException
   */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  WPSCapabilitiesType getCapabilities(GetCapabilitiesType getCapabilitiesType);

  /**
   * DescribeProcess - HTTP GET
   * http://hostname:port/path?service=WPS&version=2.0.0&request=DescribeProcess&identifier=buffer,viewshed
   * http://hostname:port/path?service=WPS&version=2.0.0&request=DescribeProcess&identifier=ALL
   *
   * @param identifiers
   * @param lang
   * @return ProcessOfferings
   * @throws WpsException
   */
  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  ProcessOfferings describeProcess(
      @QueryParam("identifier") String identifiers, @QueryParam("lang") String lang);

  /**
   * DescribeProcess - HTTP POST
   *
   * @param describeProcess
   * @return ProcessOfferings
   * @throws WpsException
   */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  ProcessOfferings describeProcess(DescribeProcess describeProcess);

  /**
   * DescribeProcess - HTTP POST
   *
   * @param executeRequestType
   * @return Result or StatusInfo for sync and async requests respectively
   * @throws WpsException
   */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  Response execute(ExecuteRequestType executeRequestType);

  /**
   * GetStatus - HTTP GET
   * http://hostname:port/path?service=WPS&version=2.0.0&request=GetStatus&jobId=
   * FB6DD4B0-A2BB-11E3-A5E2-0800200C9A66
   *
   * @param jobId
   * @return StatusInfo
   * @throws WpsException
   */
  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  StatusInfo getStatus(@QueryParam("jobId") String jobId);

  /**
   * GetStatus - HTTP POST
   *
   * @param getStatus
   * @return StatusInfo
   * @throws WpsException
   */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  StatusInfo getStatus(GetStatus getStatus);

  /**
   * GetResult - HTTP GET
   * http://hostname:port/path?service=WPS&version=2.0.0&request=GetResult&jobId=
   * FB6DD4B0-A2BB-11E3-A5E2-0800200C9A66
   *
   * @param jobId
   * @return Result
   * @throws WpsException
   */
  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  Response getResult(@QueryParam("jobId") String jobId);

  /**
   * GetResult - HTTP POST
   *
   * @param getResult
   * @return Result
   * @throws WpsException
   */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  Response getResult(GetResult getResult);

  /**
   * GetResult - HTTP GET http://hostname:port/path?service=WPS&version=2.0.0&request=dismiss&jobId=
   * FB6DD4B0-A2BB-11E3-A5E2-0800200C9A66
   *
   * @param jobId
   * @return StatusInfo
   * @throws WpsException
   */
  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  StatusInfo dismiss(@QueryParam("jobId") String jobId);

  /**
   * GetResult - HTTP POST
   *
   * @param dismiss
   * @return StatusInfo
   * @throws WpsException
   */
  @POST
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  StatusInfo dismiss(Dismiss dismiss);
}
