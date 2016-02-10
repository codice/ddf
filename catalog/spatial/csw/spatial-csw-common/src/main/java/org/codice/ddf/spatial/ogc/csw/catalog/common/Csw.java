/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;

import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;

/**
 * JAX-RS Interface to define an OGC Catalogue Service for Web (CSW).
 */
@Path("/")
public interface Csw {

    /**
     * GetCapabilities - HTTP GET
     *
     * @param request
     * @return
     */
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    CapabilitiesType getCapabilities(@QueryParam("") GetCapabilitiesRequest request)
            throws CswException;

    /**
     * GetCapabilities - HTTP POST
     *
     * @param request
     * @return
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    CapabilitiesType getCapabilities(GetCapabilitiesType request) throws CswException;

    /**
     * DescribeRecord - HTTP GET
     *
     * @param request
     * @return
     */
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    DescribeRecordResponseType describeRecord(@QueryParam("") DescribeRecordRequest request)
            throws CswException;

    /**
     * DescribeRecord - HTTP POST
     *
     * @param request
     * @return
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    DescribeRecordResponseType describeRecord(DescribeRecordType request) throws CswException;

    /**
     * GetRecords - HTTP GET
     *
     * @param request
     * @return
     */
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    CswRecordCollection getRecords(@QueryParam("") GetRecordsRequest request) throws CswException;

    /**
     * GetRecords - HTTP POST
     *
     * @param request
     * @return
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    CswRecordCollection getRecords(GetRecordsType request) throws CswException;

    /**
     * GetRecordById - HTTP GET
     *
     * @param request
     * @return
     */
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    CswRecordCollection getRecordById(@QueryParam("") GetRecordByIdRequest request)
            throws CswException;

    /**
     * GetRecordById - HTTP POST
     *
     * @param request
     * @return
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    CswRecordCollection getRecordById(GetRecordByIdType request) throws CswException;

    /**
     * Transaction - HTTP POST
     *
     * @param request
     * @return
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    TransactionResponseType transaction(CswTransactionRequest request) throws CswException;

    /**
     * Deletes an active subscription
     *
     * @param requestId the requestId of the subscription to be removed
     * @return Acknowledgment   returns a CSW Acknowledgment message with the subscription that was
     * deleted or an empty 404 if none are found
     * @throws CswException
     */
    @DELETE
    @Path("/subscription/{requestId}")
    @Produces({MediaType.WILDCARD})
    Response deleteRecordsSubscription(@PathParam("requestId") String requestId)
            throws CswException;

    /**
     * Get an active subscription
     *
     * @param requestId the requestId of the subscription to get
     * @return Acknowledgment   returns a CSW Acknowledgment message with the subscription that was
     * found or an empty 404 if none are found
     * @throws CswException
     */
    @GET
    @Path("/subscription/{requestId}")
    @Produces({MediaType.WILDCARD})
    Response getRecordsSubscription(@PathParam("requestId") String requestId) throws CswException;

    /**
     * Updates an active subscription
     *
     * @param requestId the requestId of the subscription to get
     * @param request   the GetRocordsType request which contains the filter that the
     *                  subscription is subscribing too and a ResponseHandler URL that will
     *                  handle the CswRecordCollection response messages. When an create, update
     *                  or delete event is received a CswRecordCollection will be sent via a
     *                  POST, PUT or DELETE to the ResponseHandler URL.
     * @return Acknowledgment   returns a CSW Acknowledgment message with the subscription that was
     * updated or added
     * @throws CswException
     */
    @PUT
    @Path("/subscription/{requestId}")
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.WILDCARD})
    Response updateRecordsSubscription(@PathParam("requestId") String requestId,
            GetRecordsType request) throws CswException;

    /**
     * Create a subscription
     *
     * @param request the GetRecordsRequest request which contains the filter that the
     *                subscription is subscribing too and a ResponseHandler URL that will
     *                handle the CswRecordCollection response messages. When an create, update
     *                or delete event is received a CswRecordCollection will be sent via a
     *                POST, PUT or DELETE to the ResponseHandler URL.
     * @return Acknowledgment   returns a CSW Acknowledgment message with the subscription that was
     * added
     * @throws CswException
     */
    @GET
    @Path("/subscription")
    @Produces({MediaType.WILDCARD})
    Response createRecordsSubscription(@QueryParam("") GetRecordsRequest request)
            throws CswException;

    /**
     * Create a subscription
     *
     * @param request the GetRecordsType request which contains the filter that the
     *                subscription is subscribing too and a ResponseHandler URL that will
     *                handle the CswRecordCollection response messages. When an create, update
     *                or delete event is received a CswRecordCollection will be sent via a
     *                POST, PUT or DELETE to the ResponseHandler URL
     * @return Acknowledgment   returns a CSW Acknowledgment message with the subscription that was
     * added
     * @throws CswException
     */
    @POST
    @Path("/subscription")
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.WILDCARD})
    Response createRecordsSubscription(GetRecordsType request) throws CswException;
}
