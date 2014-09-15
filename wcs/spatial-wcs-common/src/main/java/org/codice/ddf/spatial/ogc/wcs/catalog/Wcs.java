/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package org.codice.ddf.spatial.ogc.wcs.catalog;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import net.opengis.wcs.v_1_0_0.CoverageDescription;
import net.opengis.wcs.v_1_0_0.DescribeCoverage;
import net.opengis.wcs.v_1_0_0.GetCapabilities;
import net.opengis.wcs.v_1_0_0.GetCoverage;
import net.opengis.wcs.v_1_0_0.WCSCapabilitiesType;

/**
 * JAX-RS Interface to define an OGC Web Coverage Service (WCS).
 * 
 */
@Path("/")
public interface Wcs {

    /**
     * GetCapabilities - HTTP GET
     * 
     * @param request
     * @return
     */
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    WCSCapabilitiesType getCapabilities(@QueryParam("")
    GetCapabilitiesRequest request) throws WcsException;

    /**
     * GetCapabilities - HTTP POST
     * 
     * @param request
     * @return
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    WCSCapabilitiesType getCapabilities(GetCapabilities request) throws WcsException;

    /**
     * DescribeCoverage - HTTP GET
     * 
     * @param request
     * @return
     */
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    CoverageDescription describeCoverage(@QueryParam("")
    DescribeCoverageRequest request) throws WcsException;

    /**
     * DescribeCoverage - HTTP POST
     * 
     * @param request
     * @return
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    CoverageDescription describeCoverage(DescribeCoverage request) throws WcsException;

    /**
     * GetRecords - HTTP GET
     * 
     * @param request
     * @return
     */
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    GetCoverageResponse getCoverage(@QueryParam("")
    GetCoverageRequest request) throws WcsException;

    /**
     * GetRecords - HTTP POST
     * 
     * @param request
     * @return
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    GetCoverageResponse getCoverage(GetCoverage request) throws WcsException;
    

    /**
     * GetRecords - HTTP POST
     * 
     * @param request
     * @return
     */
    @POST
    @Produces("multipart/mixed")
    @Consumes("text/xml")
    public MultipartBody getCoverageMultiPart(GetCoverage request) throws WcsException;

}
