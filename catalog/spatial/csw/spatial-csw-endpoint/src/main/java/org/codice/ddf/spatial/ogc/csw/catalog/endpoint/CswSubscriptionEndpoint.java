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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import ddf.catalog.data.Metacard;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.event.Subscriber;
import ddf.catalog.event.Subscription;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.service.SecurityManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.stream.StreamResult;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.EchoedRequestType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.codice.ddf.platform.util.TransformerProperties;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.security.Security;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSubscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscription;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscriptionConfigFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

@Path("/subscription")
public class CswSubscriptionEndpoint implements CswSubscribe, Subscriber {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswSubscriptionEndpoint.class);

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private static final String METACARD_SCHEMA = "urn:catalog:metacard";

  private static final String UUID_URN = "urn:uuid:";

  private final TransformerManager schemaTransformerManager;

  private final TransformerManager mimeTypeTransformerManager;

  private final TransformerManager inputTransformerManager;

  private final ObjectFactory objectFactory = new ObjectFactory();

  private final Validator validator;

  private final CswQueryFactory queryFactory;

  private final EventProcessor eventProcessor;

  private final ClientFactoryFactory clientFactoryFactory;

  private DatatypeFactory datatypeFactory;

  private Map<String, ServiceRegistration<Subscription>> registeredSubscriptions = new HashMap<>();

  private Security security;

  private SecurityManager securityManager;

  public CswSubscriptionEndpoint(
      EventProcessor eventProcessor,
      TransformerManager mimeTypeTransformerManager,
      TransformerManager schemaTransformerManager,
      TransformerManager inputTransformerManager,
      Validator validator,
      CswQueryFactory queryFactory,
      ClientFactoryFactory clientFactoryFactory,
      Security security,
      SecurityManager securityManager) {
    this.eventProcessor = eventProcessor;
    this.mimeTypeTransformerManager = mimeTypeTransformerManager;
    this.schemaTransformerManager = schemaTransformerManager;
    this.inputTransformerManager = inputTransformerManager;
    this.validator = validator;
    this.queryFactory = queryFactory;
    this.clientFactoryFactory = clientFactoryFactory;
    this.security = security;
    this.securityManager = securityManager;

    try {
      this.datatypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      LOGGER.debug("Error initializing datatypeFactory", e);
    }
  }

  /**
   * Deletes an active subscription
   *
   * @param requestId the requestId of the subscription to be removed
   * @return Acknowledgment returns a CSW Acknowledgment message with the subscription that was
   *     deleted or an empty 404 if none are found
   * @throws CswException
   */
  @Override
  @DELETE
  @Path("/{requestId}")
  @Produces({MediaType.WILDCARD})
  public Response deleteRecordsSubscription(@PathParam("requestId") String requestId)
      throws CswException {

    CswSubscription subscription = deleteCswSubscription(requestId);
    if (subscription == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return createAcknowledgment(subscription.getOriginalRequest());
  }

  /**
   * Get an active subscription
   *
   * @param requestId the requestId of the subscription to get
   * @return Acknowledgment returns a CSW Acknowledgment message with the subscription that was
   *     found or an empty 404 if none are found
   * @throws CswException
   */
  @Override
  @GET
  @Path("/{requestId}")
  @Produces({MediaType.WILDCARD})
  public Response getRecordsSubscription(@PathParam("requestId") String requestId) {

    CswSubscription subscription = getSubscription(requestId);
    if (subscription == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return createAcknowledgment(subscription.getOriginalRequest());
  }

  /**
   * Updates an active subscription
   *
   * @param requestId the requestId of the subscription to get
   * @param request the GetRocordsType request which contains the filter that the subscription is
   *     subscribing too and a ResponseHandler URL that will handle the CswRecordCollection response
   *     messages. When an create, update or delete event is received a CswRecordCollection will be
   *     sent via a POST, PUT or DELETE to the ResponseHandler URL.
   * @return Acknowledgment returns a CSW Acknowledgment message with the subscription that was
   *     updated or added
   * @throws CswException
   */
  @Override
  @PUT
  @Path("/{requestId}")
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.WILDCARD})
  public Response updateRecordsSubscription(
      @PathParam("requestId") String requestId, GetRecordsType request) throws CswException {
    if (!hasSubscription(requestId)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return createOrUpdateSubscription(request, requestId);
  }

  /**
   * Create a subscription
   *
   * @param request the GetRecordsRequest request which contains the filter that the subscription is
   *     subscribing too and a ResponseHandler URL that will handle the CswRecordCollection response
   *     messages. When an create, update or delete event is received a CswRecordCollection will be
   *     sent via a POST, PUT or DELETE to the ResponseHandler URL.
   * @return Acknowledgment returns a CSW Acknowledgment message with the subscription that was
   *     added
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
      LOGGER.debug("{} attempting to subscribe.", LogSanitizer.sanitize(request.getRequest()));
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
   * @param request the GetRecordsType request which contains the filter that the subscription is
   *     subscribing too and a ResponseHandler URL that will handle the CswRecordCollection response
   *     messages. When an create, update or delete event is received a CswRecordCollection will be
   *     sent via a POST, PUT or DELETE to the ResponseHandler URL
   * @return Acknowledgment returns a CSW Acknowledgment message with the subscription that was
   *     added
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

  public Response createOrUpdateSubscription(
      GetRecordsType request, String requestId, boolean persist) throws CswException {

    validator.validateOutputFormat(request.getOutputFormat(), mimeTypeTransformerManager);

    validator.validateOutputSchema(request.getOutputSchema(), schemaTransformerManager);

    if (request.getAbstractQuery() != null) {
      if (!request.getAbstractQuery().getValue().getClass().equals(QueryType.class)) {
        throw new CswException(
            "Unknown QueryType: " + request.getAbstractQuery().getValue().getClass());
      }

      QueryType query = (QueryType) request.getAbstractQuery().getValue();

      validator.validateTypes(query.getTypeNames(), CswConstants.VERSION_2_0_2);

      validator.validateElementNames(query);

      if (query.getConstraint() != null
          && query.getConstraint().isSetFilter()
          && query.getConstraint().isSetCqlText()) {
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

  @Override
  @POST
  @Path("/event")
  @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
  public Response createEvent(GetRecordsResponseType recordsResponse) throws CswException {
    validateResponseSchema(recordsResponse);
    List<Metacard> metacards = getMetacards(recordsResponse);
    eventProcessor.notifyCreated(metacards.get(0));
    return Response.ok().build();
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
    return Response.ok().build();
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
    return Response.ok().build();
  }

  @Override
  @HEAD
  @Path("/event")
  public Response ping() {
    return Response.ok().build();
  }

  private void validateResponseSchema(GetRecordsResponseType recordsResponse) throws CswException {
    if (!METACARD_SCHEMA.equals(recordsResponse.getSearchResults().getRecordSchema())) {
      throw new CswException(
          "Only " + METACARD_SCHEMA + " is supported for federated event consumption");
    }
  }

  private List<Metacard> getMetacards(GetRecordsResponseType recordsResponse) throws CswException {
    try {
      InputTransformer transformer =
          inputTransformerManager.getTransformerBySchema(
              recordsResponse.getSearchResults().getRecordSchema());
      List<Metacard> metacards = new ArrayList<>();
      for (Object result : recordsResponse.getSearchResults().getAny()) {
        if (result instanceof Node) {
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          XML_UTILS.transform(
              (Node) result, new TransformerProperties(), new StreamResult(outputStream));
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

    return Response.ok().entity(acknowledgementType).build();
  }

  public synchronized boolean hasSubscription(String subscriptionId) {
    LOGGER.debug("subscriptionUuid = {}", LogSanitizer.sanitize(subscriptionId));
    return registeredSubscriptions.containsKey(subscriptionId);
  }

  private String getSubscriptionUuid(String subscriptionId) {
    if (subscriptionId == null || subscriptionId.isEmpty()) {
      return UUID_URN + UUID.randomUUID().toString();
    } else if (!subscriptionId.startsWith(UUID_URN)) {
      return UUID_URN + subscriptionId;
    }
    return subscriptionId;
  }

  private synchronized CswSubscription getSubscription(String subscriptionId) {
    ServiceRegistration sr = registeredSubscriptions.get(subscriptionId);
    if (sr == null) {
      return null;
    }
    return (CswSubscription) getBundleContext().getService(sr.getReference());
  }

  public CswSubscription createSubscription(GetRecordsType request) throws CswException {
    QueryRequest query = queryFactory.getQuery(request);
    // if it is an empty query we need to create a filterless subscription
    if (((QueryType) request.getAbstractQuery().getValue()).getConstraint() == null) {
      return CswSubscription.getFilterlessSubscription(
          mimeTypeTransformerManager,
          request,
          query,
          clientFactoryFactory,
          security,
          securityManager);
    }
    return new CswSubscription(
        mimeTypeTransformerManager,
        request,
        query,
        clientFactoryFactory,
        security,
        securityManager);
  }

  public synchronized String addOrUpdateSubscription(
      GetRecordsType request, boolean persistSubscription) throws CswException {
    String methodName = "createSubscription";
    LOGGER.trace("ENTERING: {}    (persistSubscription = {})", methodName, persistSubscription);

    if (request.getResponseHandler() == null
        || request.getResponseHandler().isEmpty()
        || StringUtils.isEmpty(request.getResponseHandler().get(0))) {
      throw new CswException(
          "Unable to create subscription because deliveryMethodUrl is null or empty");
    }

    String deliveryMethodUrl = request.getResponseHandler().get(0);

    String subscriptionUuid = getSubscriptionUuid(request.getRequestId());
    LOGGER.debug("subscriptionUuid = {}", subscriptionUuid);
    request.setRequestId(subscriptionUuid);

    // If this subscription already exists, then delete it and re-add it
    // to registry
    if (registeredSubscriptions.containsKey(subscriptionUuid)) {
      LOGGER.debug("Delete existing subscription {} for re-creation", subscriptionUuid);
      deleteCswSubscription(subscriptionUuid);
    }
    CswSubscription sub = createSubscription(request);

    Dictionary<String, String> props = new DictionaryMap<>();
    props.put("subscription-id", subscriptionUuid);
    props.put("event-endpoint", request.getResponseHandler().get(0));

    LOGGER.debug("Registering Subscription");
    ServiceRegistration serviceRegistration =
        getBundleContext().registerService(Subscription.class.getName(), sub, props);

    if (serviceRegistration != null) {
      LOGGER.debug(
          "Subscription registered with bundle ID = {} ",
          serviceRegistration.getReference().getBundle().getBundleId());
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

    LOGGER.trace("EXITING: {}", methodName);
    return subscriptionUuid;
  }

  @Override
  public boolean deleteSubscription(String subscriptionId) {
    try {
      return deleteCswSubscription(subscriptionId) != null;
    } catch (CswException e) {
      return false;
    }
  }

  private synchronized CswSubscription deleteCswSubscription(String subscriptionId)
      throws CswException {
    String methodName = "deleteCswSubscription";
    LogSanitizer logSanitizedId = LogSanitizer.sanitize(subscriptionId);
    LOGGER.trace("ENTERING: {}", methodName);
    LOGGER.trace("subscriptionId = {}", logSanitizedId);

    if (StringUtils.isEmpty(subscriptionId)) {
      throw new CswException(
          "Unable to delete subscription because subscription ID is null or empty");
    }

    CswSubscription subscription = getSubscription(subscriptionId);
    try {
      LOGGER.debug("Removing (unregistering) subscription: {}", logSanitizedId);
      ServiceRegistration sr = registeredSubscriptions.remove(subscriptionId);
      if (sr != null) {
        sr.unregister();
      } else {
        LOGGER.debug("No ServiceRegistration found for subscription: {}", logSanitizedId);
      }

      Configuration subscriptionConfig = getSubscriptionConfiguration(subscriptionId);
      try {
        if (subscriptionConfig != null) {
          LOGGER.debug("Deleting subscription for subscriptionId = {}", logSanitizedId);
          subscriptionConfig.delete();

        } else {
          LOGGER.debug("subscriptionConfig is NULL for ID = {}", logSanitizedId);
        }
      } catch (IOException e) {
        LOGGER.debug(
            "IOException trying to delete subscription's configuration for subscription ID {}",
            subscriptionId,
            e);
      }

      LOGGER.debug("Subscription removal complete");
    } catch (Exception e) {
      LOGGER.debug("Could not delete subscription for {}", logSanitizedId, e);
    }

    LOGGER.trace("EXITING: {}    (status = {})", methodName, false);

    return subscription;
  }

  /**
   * Persist the subscription to the OSGi ConfigAdmin service. Persisted registeredSubscriptions can
   * then be restored if DDF is restarted after a DDF outage or DDF is shutdown. Pass in
   * client-provided subscriptionId and subscription UUID because the filter XML to be persisted for
   * this subscription will be used to restore this subscription and should consist of the exact
   * values the client originally provided.
   */
  private void persistSubscription(
      CswSubscription subscription, String deliveryMethodUrl, String subscriptionUuid) {
    String methodName = "persistSubscription";
    LOGGER.trace("ENTERING: {}", methodName);

    try {
      StringWriter sw = new StringWriter();
      CswQueryFactory.getJaxBContext()
          .createMarshaller()
          .marshal(objectFactory.createGetRecords(subscription.getOriginalRequest()), sw);
      String filterXml = sw.toString();
      ConfigurationAdmin configAdmin = getConfigAdmin();
      // Store filter XML, deliveryMethod URL, this endpoint's factory PID, and subscription ID into
      // OSGi CongiAdmin
      if (filterXml != null && configAdmin != null) {
        Configuration config =
            configAdmin.createFactoryConfiguration(CswSubscriptionConfigFactory.FACTORY_PID, null);

        Dictionary<String, String> props = new DictionaryMap<>();
        props.put(CswSubscriptionConfigFactory.SUBSCRIPTION_ID, subscriptionUuid);
        props.put(CswSubscriptionConfigFactory.FILTER_XML, filterXml);
        props.put(CswSubscriptionConfigFactory.DELIVERY_METHOD_URL, deliveryMethodUrl);
        props.put(CswSubscriptionConfigFactory.SUBSCRIPTION_UUID, subscriptionUuid);

        LOGGER.debug("Done adding persisting subscription to ConfigAdmin");

        config.update(props);
      }
    } catch (JAXBException | IOException e) {
      LOGGER.debug("Unable to persist subscription {}", subscriptionUuid, e);
    }

    LOGGER.trace("EXITING: {}", methodName);
  }

  private ConfigurationAdmin getConfigAdmin() {
    ConfigurationAdmin configAdmin = null;
    BundleContext context = getBundleContext();
    ServiceReference configAdminRef =
        context.getServiceReference(ConfigurationAdmin.class.getName());

    if (configAdminRef != null) {
      configAdmin = (ConfigurationAdmin) context.getService(configAdminRef);
    }

    return configAdmin;
  }

  private Configuration getSubscriptionConfiguration(String subscriptionUuid) {
    String methodName = "getSubscriptionConfiguration";
    LOGGER.trace("ENTERING: {}", methodName);

    String filterStr = getSubscriptionUuidFilter(subscriptionUuid);
    LogSanitizer logSanitizedFilter = LogSanitizer.sanitize(filterStr);
    LOGGER.debug("filterStr = {}", logSanitizedFilter);

    Configuration config = null;

    try {
      org.osgi.framework.Filter filter = getBundleContext().createFilter(filterStr);
      LOGGER.debug("filter.toString() = {}", filter);

      ConfigurationAdmin configAdmin = getConfigAdmin();

      if (configAdmin != null) {
        Configuration[] configs = configAdmin.listConfigurations(filter.toString());

        if (configs == null) {
          LOGGER.debug("Did NOT find a configuration for filter {}", logSanitizedFilter);
        } else if (configs.length != 1) {
          LOGGER.debug("Found multiple configurations for filter {}", logSanitizedFilter);
        } else {
          LOGGER.debug("Found exactly one configuration for filter {}", logSanitizedFilter);
          config = configs[0];
        }
      }
    } catch (InvalidSyntaxException e) {
      LOGGER.debug("Invalid syntax for filter used for searching configuration instances", e);
    } catch (IOException e) {
      LOGGER.debug(
          "IOException trying to list configurations for filter {}", logSanitizedFilter, e);
    }

    LOGGER.trace("EXITING: {}", methodName);

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

  BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(CswSubscriptionEndpoint.class).getBundleContext();
  }
}
