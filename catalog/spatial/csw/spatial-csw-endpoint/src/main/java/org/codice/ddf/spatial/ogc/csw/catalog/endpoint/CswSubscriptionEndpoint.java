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
 */
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionContainer;
import org.codice.ddf.platform.util.TransformerProperties;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSubscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscription;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscriptionFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import ddf.catalog.data.Metacard;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.event.Subscriber;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.EchoedRequestType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;

@Path("/subscription")
public class CswSubscriptionEndpoint implements CswSubscribe, Subscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswSubscriptionEndpoint.class);

    private static final String CSW_SUBSCRIPTION_TYPE = "CSW";

    private static final String METACARD_SCHEMA = "urn:catalog:metacard";

    private final TransformerManager schemaTransformerManager;

    private final TransformerManager mimeTypeTransformerManager;

    private final TransformerManager inputTransformerManager;

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final Validator validator;

    private final EventProcessor eventProcessor;

    private DatatypeFactory datatypeFactory;

    private final SubscriptionContainer<CswSubscription> subscriptionContainer;

    private final CswSubscriptionFactory cswSubscriptionFactory;

    public CswSubscriptionEndpoint(EventProcessor eventProcessor,
            TransformerManager mimeTypeTransformerManager,
            TransformerManager schemaTransformerManager, TransformerManager inputTransformerManager,
            Validator validator, CswQueryFactory queryFactory,
            SubscriptionContainer<CswSubscription> subscriptionContainer) {
        this.eventProcessor = eventProcessor;
        this.mimeTypeTransformerManager = mimeTypeTransformerManager;
        this.schemaTransformerManager = schemaTransformerManager;
        this.inputTransformerManager = inputTransformerManager;
        this.validator = validator;

        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            LOGGER.debug("Error initializing datatypeFactory", e);
        }

        this.cswSubscriptionFactory = new CswSubscriptionFactory(mimeTypeTransformerManager,
                queryFactory);

        subscriptionContainer.registerSubscriptionFactory(CSW_SUBSCRIPTION_TYPE,
                cswSubscriptionFactory);

        this.subscriptionContainer = subscriptionContainer;
    }

    /**
     * Deletes an active subscription
     *
     * @param requestId the requestId of the subscription to be removed
     * @return Acknowledgment   returns a CSW Acknowledgment message with the subscription that was
     * deleted or an empty 404 if none are found
     * @throws CswException
     */
    @Override
    @DELETE
    @Path("/{requestId}")
    @Produces({MediaType.WILDCARD})
    public Response deleteRecordsSubscription(@PathParam("requestId") String requestId)
            throws CswException {
        CswSubscription subscription = subscriptionContainer.delete(requestId,
                CSW_SUBSCRIPTION_TYPE);
        if (subscription == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }
        return createAcknowledgment(subscription.getOriginalRequest());
    }

    /**
     * Get an active subscription
     *
     * @param requestId the requestId of the subscription to get
     * @return Acknowledgment   returns a CSW Acknowledgment message with the subscription that was
     * found or an empty 404 if none are found
     * @throws CswException
     */
    @Override
    @GET
    @Path("/{requestId}")
    @Produces({MediaType.WILDCARD})
    public Response getRecordsSubscription(@PathParam("requestId") String requestId) {
        CswSubscription subscription = subscriptionContainer.get(requestId, CSW_SUBSCRIPTION_TYPE);
        if (subscription == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }
        return createAcknowledgment(subscription.getOriginalRequest());
    }

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
    @Override
    @PUT
    @Path("/{requestId}")
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.WILDCARD})
    public Response updateRecordsSubscription(@PathParam("requestId") String requestId,
            GetRecordsType request) throws CswException {
        if (!subscriptionContainer.contains(requestId, CSW_SUBSCRIPTION_TYPE)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }

        validateRequest(request, requestId);
        updateSubscription(request, requestId);
        return createAcknowledgment(request);
    }

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
    @Override
    @GET
    @Produces({MediaType.WILDCARD})
    public Response createRecordsSubscription(@QueryParam("") GetRecordsRequest request)
            throws CswException {
        if (request == null) {
            throw new CswException("GetRecordsSubscription request is null");
        } else {
            LOGGER.debug("{} attempting to subscribe.", request.getRequest());
        }
        if (StringUtils.isEmpty(request.getVersion())) {
            request.setVersion(CswConstants.VERSION_2_0_2);
        } else {
            validator.validateVersion(request.getVersion());
        }

        GetRecordsType getRecordsType = request.get202RecordsType();
        validateRequest(getRecordsType, null);
        addSubscription(getRecordsType);
        return createAcknowledgment(getRecordsType);
    }

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
    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.WILDCARD})
    public Response createRecordsSubscription(GetRecordsType request) throws CswException {
        validateRequest(request, null);
        addSubscription(request);
        return createAcknowledgment(request);
    }

    @Override
    @POST
    @Path("/event")
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public Response createEvent(GetRecordsResponseType recordsResponse) throws CswException {
        validateResponseSchema(recordsResponse);
        List<Metacard> metacards = getMetacards(recordsResponse);
        eventProcessor.notifyCreated(metacards.get(0));
        return Response.ok()
                .build();

    }

    @Override
    @PUT
    @Path("/event")
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public Response updateEvent(GetRecordsResponseType recordsResponse) throws CswException {
        validateResponseSchema(recordsResponse);
        List<Metacard> metacards = getMetacards(recordsResponse);
        eventProcessor.notifyUpdated(metacards.get(0), null);
        return Response.ok()
                .build();

    }

    @Override
    @DELETE
    @Path("/event")
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public Response deleteEvent(GetRecordsResponseType recordsResponse) throws CswException {
        validateResponseSchema(recordsResponse);
        List<Metacard> metacards = getMetacards(recordsResponse);
        eventProcessor.notifyDeleted(metacards.get(0));
        return Response.ok()
                .build();

    }

    @Override
    @HEAD
    @Path("/event")
    public Response ping() {
        return Response.ok()
                .build();

    }

    private void validateRequest(GetRecordsType request, String requestId) throws CswException {
        if (request == null) {
            throw new CswException("Request is null");
        } else {
            LOGGER.debug("{} attempting to subscribe.", request.getService());
        }

        request.setRequestId(requestId);

        validator.validateOutputFormat(request.getOutputFormat(), mimeTypeTransformerManager);
        validator.validateOutputSchema(request.getOutputSchema(), schemaTransformerManager);

        if (request.getAbstractQuery() != null) {
            if (!request.getAbstractQuery()
                    .getValue()
                    .getClass()
                    .equals(QueryType.class)) {
                throw new CswException("Unknown QueryType: " + request.getAbstractQuery()
                        .getValue()
                        .getClass());
            }

            QueryType query = (QueryType) request.getAbstractQuery()
                    .getValue();

            validator.validateTypes(query.getTypeNames(), CswConstants.VERSION_2_0_2);
            validator.validateElementNames(query);

            if (query.getConstraint() != null && query.getConstraint()
                    .isSetFilter() && query.getConstraint()
                    .isSetCqlText()) {
                throw new CswException("A Csw Query can only have a Filter or CQL constraint");
            }
        }

        if (request.getResponseHandler() == null || request.getResponseHandler()
                .isEmpty() || StringUtils.isEmpty(request.getResponseHandler()
                .get(0))) {
            throw new CswException(
                    "Unable to create subscription because deliveryMethodUrl is null or empty");
        }
    }

    private void validateResponseSchema(GetRecordsResponseType recordsResponse)
            throws CswException {
        if (!METACARD_SCHEMA.equals(recordsResponse.getSearchResults()
                .getRecordSchema())) {
            throw new CswException(
                    "Only " + METACARD_SCHEMA + " is supported for federated event consumption");
        }
    }

    private List<Metacard> getMetacards(GetRecordsResponseType recordsResponse)
            throws CswException {
        try {
            InputTransformer transformer = inputTransformerManager.getTransformerBySchema(
                    recordsResponse.getSearchResults()
                            .getRecordSchema());
            List<Metacard> metacards = new ArrayList<>();
            for (Object result : recordsResponse.getSearchResults()
                    .getAny()) {
                if (result instanceof Node) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    XMLUtils.transform((Node) result,
                            new TransformerProperties(),
                            new StreamResult(outputStream));
                    InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
                    metacards.add(transformer.transform(is));
                }
            }
            return metacards;
        } catch (IOException | CatalogTransformerException e) {
            String msg = "Could not parse SearchResults in getRecordsResponse";
            LOGGER.debug(msg, e);
            throw new CswException(msg, e);
        }
    }

    private Response createAcknowledgment(GetRecordsType request) {
        AcknowledgementType acknowledgementType = new AcknowledgementType();
        if (request != null) {
            EchoedRequestType echoedRequest = new EchoedRequestType();
            echoedRequest.setAny(objectFactory.createGetRecords(request));
            acknowledgementType.setEchoedRequest(echoedRequest);
            acknowledgementType.setRequestId(request.getRequestId());
        }

        if (datatypeFactory != null) {
            acknowledgementType.setTimeStamp(datatypeFactory.newXMLGregorianCalendar());
        }

        return Response.ok()
                .entity(acknowledgementType)
                .build();
    }

    private void addSubscription(GetRecordsType request) {
        CswSubscription subscription = cswSubscriptionFactory.createCswSubscription(request);
        String serializedRequest = cswSubscriptionFactory.writeoutGetRecordsType(request);
        String deliveryMethodUrl = request.getResponseHandler()
                .get(0);

        String id = subscriptionContainer.insert(subscription,
                CSW_SUBSCRIPTION_TYPE,
                serializedRequest,
                deliveryMethodUrl);

        request.setRequestId(id);
    }

    private void updateSubscription(GetRecordsType request, String id) {
        CswSubscription subscription = cswSubscriptionFactory.createCswSubscription(request);
        String serializedRequest = cswSubscriptionFactory.writeoutGetRecordsType(request);
        String deliveryMethodUrl = request.getResponseHandler()
                .get(0);

        subscriptionContainer.update(subscription,
                CSW_SUBSCRIPTION_TYPE,
                serializedRequest,
                deliveryMethodUrl,
                id);

        request.setRequestId(id);
    }

    @Override
    public boolean deleteSubscription(String subscriptionId) {
        return subscriptionContainer.delete(subscriptionId, CSW_SUBSCRIPTION_TYPE) != null;
    }

    BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(CswSubscriptionEndpoint.class)
                .getBundleContext();
    }
}
