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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Subscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscription;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscriptionConfigFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.Subscription;
import ddf.catalog.operation.QueryRequest;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.EchoedRequestType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;

public class CswSubscriptionEndpoint implements Subscribe {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswSubscriptionEndpoint.class);

    private Map<String, ServiceRegistration<Subscription>> registeredSubscriptions =
            new ConcurrentHashMap<>();

    private ObjectFactory objectFactory = new ObjectFactory();

    private static final String UUID_URN = "urn:uuid:";

    private DatatypeFactory datatypeFactory;

    private BundleContext context;

    private final TransformerManager schemaTransformerManager;

    private final TransformerManager mimeTypeTransformerManager;

    private Validator validator;

    private CswQueryFactory queryFactory;

    public CswSubscriptionEndpoint(BundleContext context,
            TransformerManager mimeTypeTransformerManager,
            TransformerManager schemaTransformerManager, Validator validator,
            CswQueryFactory queryFactory) {
        this.context = context;
        this.mimeTypeTransformerManager = mimeTypeTransformerManager;
        this.schemaTransformerManager = schemaTransformerManager;
        this.validator = validator;
        this.queryFactory = queryFactory;

        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("Error initializing datatypeFactory", e);
        }
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
        CswSubscription subscription = getSubscription(requestId);
        if (subscription == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }
        deleteSubscription(requestId);
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
    public Response getRecordsSubscription(@PathParam("requestId") String requestId)
            throws CswException {

        CswSubscription subscription = getSubscription(requestId);
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
        if (!hasSubscription(requestId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .build();
        }
        return createOrUpdateSubscription(request, requestId);
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

        LOGGER.trace("Exiting getRecordsSubscription");

        return createOrUpdateSubscription(request.get202RecordsType(), null);
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
        return createOrUpdateSubscription(request, null);
    }

    public Response createOrUpdateSubscription(GetRecordsType request, String requestId)
            throws CswException {
        if (request == null) {
            throw new CswException("Request is null");
        } else {
            LOGGER.debug("{} attempting to subscribe.", request.getService());
        }
        request.setRequestId(null);
        return createOrUpdateSubscription(request, requestId, true);
    }

    public Response createOrUpdateSubscription(GetRecordsType request, String requestId,
            boolean persist) throws CswException {

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

            if (query.getConstraint() != null &&
                    query.getConstraint()
                            .isSetFilter() && query.getConstraint()
                    .isSetCqlText()) {
                throw new CswException("A Csw Query can only have a Filter or CQL constraint");
            }
        }

        if (requestId != null) {
            request.setRequestId(requestId);
        }
        addOrUpdateSubscription(request, persist);

        LOGGER.trace("Exiting getRecordsSubscription.");

        return createAcknowledgment(request);
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

    public boolean hasSubscription(String subscriptionId) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subscriptionUuid = {}", subscriptionId);
        }
        return registeredSubscriptions.containsKey(subscriptionId);
    }

    private String getSubscriptionUuid(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            return UUID_URN+UUID.randomUUID()
                    .toString();
        }else if(!subscriptionId.startsWith(UUID_URN)){
            return UUID_URN+subscriptionId;
        }
        return subscriptionId;
    }

    private CswSubscription getSubscription(String subscriptionId) {
        ServiceRegistration sr = (ServiceRegistration) registeredSubscriptions.get(subscriptionId);
        if (sr == null) {
            return null;
        }
        return (CswSubscription) context.getService(sr.getReference());
    }

    public CswSubscription createSubscription(GetRecordsType request) throws CswException {
        QueryRequest query = queryFactory.getQuery(request);

        return new CswSubscription(mimeTypeTransformerManager, request, query);
    }

    public synchronized String addOrUpdateSubscription(GetRecordsType request,
            boolean persistSubscription) throws CswException {
        String methodName = "createSubscription";
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ENTERING: {}    (persistSubscription = {})", methodName);
        }

        if (request.getResponseHandler() == null || request.getResponseHandler()
                .isEmpty() || StringUtils.isEmpty(request.getResponseHandler()
                .get(0))) {
            throw new CswException(
                    "Unable to create subscription because deliveryMethodUrl is null or empty");
        }

        String deliveryMethodUrl = request.getResponseHandler()
                .get(0);

        String subscriptionUuid = getSubscriptionUuid(request.getRequestId());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subscriptionUuid = {}", subscriptionUuid);
        }
        request.setRequestId(subscriptionUuid);

        Dictionary<String, String> props = new Hashtable<>();
        props.put("subscription-id", subscriptionUuid);

        // If this subscription already exists, then delete it and re-add it
        // to registry
        if (registeredSubscriptions.containsKey(subscriptionUuid)) {
            LOGGER.debug("Delete existing subscription {} for re-creation", subscriptionUuid);
            deleteSubscription(subscriptionUuid);
        }
        CswSubscription sub = createSubscription(request);
        LOGGER.debug("Registering Subscription");
        ServiceRegistration serviceRegistration =
                context.registerService(Subscription.class.getName(), sub, props);

        if (serviceRegistration != null) {
            LOGGER.debug("Subscription registered with bundle ID = {} ",
                    serviceRegistration.getReference()
                            .getBundle()
                            .getBundleId());
            registeredSubscriptions.put(subscriptionUuid, serviceRegistration);
            // Pass in client-provided subscriptionId vs. subscription UUID because
            // the filter XML to be persisted for this subscription will be used to
            // restore this subscription and should consist of the exact values the
            // client originally provided.
            if (persistSubscription) {
                persistSubscription(sub, deliveryMethodUrl, subscriptionUuid);
            }
        } else {
            LOGGER.debug("Subscription registration failed");
        }

        LOGGER.debug("EXITING: {}", methodName);
        return subscriptionUuid;

    }

    private synchronized boolean deleteSubscription(String subscriptionId) throws CswException {
        String methodName = "deleteSubscription";
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ENTERING: {}", methodName);
            LOGGER.debug("subscriptionId = {}", subscriptionId);
        }

        if (StringUtils.isEmpty(subscriptionId)) {
            throw new CswException(
                    "Unable to delete subscription because subscription ID is null or empty");
        }

        boolean status = false;

        try {
            LOGGER.debug("Removing (unregistering) subscription: {}", subscriptionId);
            ServiceRegistration sr = (ServiceRegistration) registeredSubscriptions.remove(
                    subscriptionId);
            if (sr != null) {
                sr.unregister();
            } else {
                LOGGER.debug("No ServiceRegistration found for subscription: {}", subscriptionId);
            }

            Configuration subscriptionConfig = getSubscriptionConfiguration(subscriptionId);
            try {
                if (subscriptionConfig != null) {
                    LOGGER.debug("Deleting subscription for subscriptionId = {}", subscriptionId);
                    subscriptionConfig.delete();

                    // Subscription removal is only successful if able to remove from OSGi registry and
                    // ConfigAdmin service
                    if (sr != null) {
                        status = true;
                    }
                } else {
                    LOGGER.debug("subscriptionConfig is NULL for ID = {}", subscriptionId);
                }
            } catch (IOException e) {
                LOGGER.error(
                        "IOException trying to delete subscription's configuration for subscription ID "
                                + subscriptionId,
                        e);
            }

            LOGGER.info("Subscription removal complete");
        } catch (Exception e) {
            LOGGER.error("Could not delete subscription for " + subscriptionId, e);
        }

        LOGGER.debug("EXITING: {}    (status = {})", methodName, status);

        return status;
    }

    /**
     * Persist the subscription to the OSGi ConfigAdmin service. Persisted registeredSubscriptions can then be restored if DDF
     * is restarted after a DDF outage or DDF is shutdown.
     * Pass in client-provided subscriptionId and subscription UUID because the filter XML to be persisted for this subscription will be used to
     * restore this subscription and should consist of the exact values the
     * client originally provided.
     */
    private void persistSubscription(CswSubscription subscription, String deliveryMethodUrl,
            String subscriptionUuid) {
        String methodName = "persistSubscription";
        LOGGER.debug("ENTERING: {}", methodName);

        try {
            StringWriter sw = new StringWriter();
            CswQueryFactory.getJaxBContext()
                    .createMarshaller()
                    .marshal(objectFactory.createGetRecords(subscription.getOriginalRequest()), sw);
            String filterXml = sw.toString();

            // Store filter XML, deliveryMethod URL, this endpoint's factory PID, and subscription ID into OSGi CongiAdmin
            if (filterXml != null) {
                Configuration config = getConfigAdmin().createFactoryConfiguration(
                        CswSubscriptionConfigFactory.FACTORY_PID,
                        null);

                Dictionary<String, String> props = new Hashtable<>();
                props.put(CswSubscriptionConfigFactory.SUBSCRIPTION_ID, subscriptionUuid);
                props.put(CswSubscriptionConfigFactory.FILTER_XML, filterXml);
                props.put(CswSubscriptionConfigFactory.DELIVERY_METHOD_URL, deliveryMethodUrl);
                props.put(CswSubscriptionConfigFactory.SUBSCRIPTION_UUID, subscriptionUuid);

                LOGGER.debug("Done adding persisting subscription to ConfigAdmin");

                config.update(props);
            }
        } catch (JAXBException | IOException e) {
            LOGGER.warn("Unable to persist subscription " + subscriptionUuid, e);
        }

        LOGGER.debug("EXITING: {}", methodName);
    }

    private ConfigurationAdmin getConfigAdmin() {
        ConfigurationAdmin configAdmin = null;

        ServiceReference configAdminRef =
                context.getServiceReference(ConfigurationAdmin.class.getName());

        if (configAdminRef != null) {
            configAdmin = (ConfigurationAdmin) context.getService(configAdminRef);
        }

        return configAdmin;
    }

    private Configuration getSubscriptionConfiguration(String subscriptionUuid) {
        String methodName = "getSubscriptionConfiguration";
        LOGGER.debug("ENTERING: {}", methodName);

        String filterStr = getSubscriptionUuidFilter(subscriptionUuid);
        LOGGER.debug("filterStr = {}", filterStr);

        Configuration config = null;

        try {
            org.osgi.framework.Filter filter = context.createFilter(filterStr);
            LOGGER.debug("filter.toString() = {}", filter.toString());

            Configuration[] configs = getConfigAdmin().listConfigurations(filter.toString());

            if (configs == null) {
                LOGGER.debug("Did NOT find a configuration for filter {}", filterStr);
            } else if (configs.length != 1) {
                LOGGER.debug("Found multiple configurations for filter {}", filterStr);
            } else {
                LOGGER.debug("Found exactly one configuration for filter {}", filterStr);
                config = configs[0];
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.warn("Invalid syntax for filter used for searching configuration instances", e);
        } catch (IOException e) {
            LOGGER.warn("IOException trying to list configurations for filter {}", filterStr, e);
        }

        LOGGER.debug("EXITING: {}", methodName);

        return config;
    }

    /**
     * Concatenate the key and the give value to get the subscriptionUuid filter string.
     *
     * @param subscriptionUuid - in String format
     * @return a subscriptionUuid filter in String format
     */
    private String getSubscriptionUuidFilter(String subscriptionUuid) {

        return "(subscriptionUuid=" + subscriptionUuid + ")";

    }
}
