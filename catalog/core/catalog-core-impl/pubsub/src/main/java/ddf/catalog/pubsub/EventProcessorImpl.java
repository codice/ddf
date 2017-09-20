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
package ddf.catalog.pubsub;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.event.InvalidSubscriptionException;
import ddf.catalog.event.Subscription;
import ddf.catalog.event.SubscriptionExistsException;
import ddf.catalog.event.SubscriptionNotFoundException;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PreDeliveryPlugin;
import ddf.catalog.plugin.PreSubscriptionPlugin;
import ddf.catalog.pubsub.criteria.contextual.ContextualEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;
import ddf.catalog.pubsub.internal.PubSubThread;
import ddf.catalog.pubsub.internal.SubscriptionFilterVisitor;
import ddf.catalog.pubsub.predicate.Predicate;
import ddf.catalog.util.impl.Requests;
import java.net.URI;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.lucene.store.Directory;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProcessorImpl implements EventProcessor, EventHandler, PostIngestPlugin {
  public static final double EQUATORIAL_RADIUS_IN_METERS = 6378137.0;

  private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessorImpl.class);

  protected EventAdmin eventAdmin;

  protected BundleContext bundleContext;

  protected List<PreSubscriptionPlugin> preSubscription;

  protected List<PreDeliveryPlugin> preDelivery;

  protected CatalogFramework catalog;

  private Map<String, ServiceRegistration> existingSubscriptions;

  private final ExecutorService threadPool =
      Executors.newCachedThreadPool(
          StandardThreadFactoryBuilder.newThreadFactory("eventProcessorThread"));

  public EventProcessorImpl() {
    LOGGER.debug("INSIDE: EventProcessorImpl default constructor");
  }

  public EventProcessorImpl(
      BundleContext bundleContext,
      EventAdmin eventAdmin,
      List<PreSubscriptionPlugin> preSubscription,
      List<PreDeliveryPlugin> preDelivery,
      CatalogFramework catalog) {
    LOGGER.trace("ENTERING: EventProcessorImpl constructor");

    this.bundleContext = bundleContext;
    this.eventAdmin = eventAdmin;
    this.preSubscription = preSubscription;
    this.preDelivery = preDelivery;
    this.catalog = catalog;
    this.existingSubscriptions = new HashMap<>();

    if (this.preSubscription == null) {
      LOGGER.debug("preSubscription plugins list is NULL");
    } else {
      LOGGER.debug("preSubscription plugin list size = {}", this.preSubscription.size());
    }

    if (this.preDelivery == null) {
      LOGGER.debug("preDelivery plugins list is NULL");
    } else {
      LOGGER.debug("preDelivery plugin list size = {}", this.preDelivery.size());
    }

    LOGGER.trace("EXITING: EventProcessorImpl constructor");
  }

  /**
   * Processes an entry by adding properties from the metacard to the event. Then the eventAdmin is
   * used to post the metacard properties as a single event.
   *
   * @param metacard - the metacard to process
   * @param operation - The type of event {@link ddf.catalog.pubsub.internal.PubSubConstants}
   * @param eventAdmin - OSGi EventAdmin service used post events
   */
  public static void processEntry(Metacard metacard, String operation, EventAdmin eventAdmin) {
    String methodName = "processEntry";
    LOGGER.debug("ENTERING: " + methodName);

    if (metacard != null) {
      LOGGER.debug("Input Metacard:{}\n", metacard.toString());
      LOGGER.debug("catalog ID = {}", metacard.getId());
      LOGGER.debug("operation = {}", operation);

      HashMap<String, Object> properties = new HashMap<>(3, 1);

      // Common headers
      properties.put(PubSubConstants.HEADER_OPERATION_KEY, operation);
      properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);

      // ENTRY ID INFORMATION
      // TODO: probably don't need to pass this through since they can get the metacard
      properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());

      try {
        URI uri = metacard.getResourceURI();
        if (uri != null) {
          String productUri = uri.toString();
          LOGGER.debug(
              "Processing incoming entry.  Adding DAD URI to event properties: {}", productUri);
          // TODO: probably just get this info from the Metacard, Probably don't need to
          // create new property for this
          properties.put(PubSubConstants.HEADER_DAD_KEY, productUri);
        }
      } catch (Exception e) {
        LOGGER.debug("Unable to obtain resource URL, will not be considered in subscription", e);
      }

      // CONTENT TYPE INFORMATION
      String type = metacard.getContentTypeName();
      String contentType = "UNKNOWN";
      if (type != null) {
        contentType = type;
      } else {
        LOGGER.debug("contentType is null");
      }

      String version = metacard.getContentTypeVersion();

      contentType = contentType + "," + (version == null ? "" : version);

      LOGGER.debug("contentType = {}", contentType);

      properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, contentType);

      // CONTEXTUAL INFORMATION
      if (metacard.getMetadata() != null) {
        try {
          // Build Lucene search index on entry's entire metadata using
          // default XPaths (specified
          // in ContextualEvaluator) - this index will be used by all
          // contextual predicates that do
          // *NOT* specify any textPaths. (Building index here optimizes
          // code so that this index is
          // not built for every contextual subscription that has no
          // textPaths.)
          Directory index = ContextualEvaluator.buildIndex(metacard.getMetadata());

          // Build contextual info to be sent in event for this entry.
          // Include the default Lucene search
          // index and the entry's metadata (in case subscription has
          // textPaths, then it can create Lucene
          // search indices on the metadata using its textPaths)
          Map<String, Object> contextualMap = new HashMap<>(2, 1);
          contextualMap.put("DEFAULT_INDEX", index);
          contextualMap.put("METADATA", metacard.getMetadata());
          properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
        } catch (Exception e) {
          LOGGER.info("Exception updating context map", e);
        }
      }

      if (eventAdmin != null) {
        eventAdmin.postEvent(new Event(PubSubConstants.PUBLISHED_EVENT_TOPIC_NAME, properties));
      } else {
        LOGGER.debug("Unable to post event since eventAdmin is null.");
      }
    } else {
      LOGGER.debug("Unable to post null metacard.");
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  public void init() {
    String methodName = "init";
    LOGGER.debug("ENTERING: {}", methodName);

    LOGGER.debug("EXITING: {}", methodName);
  }

  public void destroy() {
    String methodName = "destroy";
    LOGGER.debug("ENTERING: {}", methodName);

    LOGGER.debug("EXITING: {}", methodName);
  }

  /**
   * By default the Felix EventAdmin implementation has a timeout of 5000 ms. Your event handler has
   * to return from the handle event method in this time frame. If it does not, it gets Blacklisted.
   * Therefore, this method processes its events in a separate thread than the EventAdmin who called
   * it.
   */
  public void handleEvent(Event event) {
    String methodName = "handleEvent";
    LOGGER.debug("ENTERING: {}", methodName);

    LOGGER.debug("Received event: {}", event.getTopic());

    if (!existingSubscriptions.isEmpty()) {
      String topic = event.getTopic();
      Metacard entry = (Metacard) event.getProperty(EventProcessor.EVENT_METACARD);
      LOGGER.debug("metacard ID = {}", entry.getId());

      new PubSubThread(entry, topic, eventAdmin).start();
    } else {
      LOGGER.debug(
          "No existing subscriptions, so no need to handle event since there is no one listening ...");
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  @Override
  public String createSubscription(Subscription subscription) throws InvalidSubscriptionException {
    String uuid = UUID.randomUUID().toString();
    try {
      createSubscription(subscription, uuid);
    } catch (SubscriptionExistsException e) {
      // This is extremely unlikely to happen. A UUID should never match
      // another subscription ID
      LOGGER.debug("UUID matched previously registered subscription.", e);
      throw new InvalidSubscriptionException(e);
    }

    return uuid;
  }

  @Override
  public void createSubscription(Subscription subscription, String subscriptionId)
      throws InvalidSubscriptionException, SubscriptionExistsException {
    String methodName = "createSubscription";
    LOGGER.debug("ENTERING: {}", methodName);

    LOGGER.debug("Creating Evaluation Criteria... ");

    try {
      for (PreSubscriptionPlugin plugin : preSubscription) {
        LOGGER.debug("Processing subscription with preSubscription plugin");
        subscription = plugin.process(subscription);
      }

      SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
      Predicate finalPredicate = (Predicate) subscription.accept(visitor, null);
      LOGGER.debug("predicate from filter visitor: {}", finalPredicate);

      String[] topics = new String[] {PubSubConstants.PUBLISHED_EVENT_TOPIC_NAME};

      Dictionary<String, String[]> props = new Hashtable<>(1, 1);
      props.put(EventConstants.EVENT_TOPIC, topics);
      ServiceRegistration serviceRegistration =
          bundleContext.registerService(
              EventHandler.class.getName(),
              new PublishedEventHandler(
                  finalPredicate, subscription, preDelivery, catalog, threadPool),
              props);

      existingSubscriptions.put(subscriptionId, serviceRegistration);

      LOGGER.debug("Subscription {} created.", subscriptionId);
    } catch (Exception e) {
      LOGGER.info("Error while creating subscription predicate: ", e);
      throw new InvalidSubscriptionException(e);
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  @Override
  public void updateSubscription(Subscription subscription, String subscriptionId)
      throws SubscriptionNotFoundException {
    String methodName = "updateSubscription";
    LOGGER.debug("ENTERING: {}", methodName);

    try {
      deleteSubscription(subscriptionId);

      createSubscription(subscription, subscriptionId);

      LOGGER.debug("Updated {}", subscriptionId);
    } catch (Exception e) {
      LOGGER.info("Could not update subscription", e);
      throw new SubscriptionNotFoundException(e);
    }

    LOGGER.debug("EXITING: {}", methodName);
  }

  @Override
  public void deleteSubscription(String subscriptionId) throws SubscriptionNotFoundException {
    String methodName = "deleteSubscription";
    LOGGER.debug("ENTERING: {}", methodName);

    try {
      LOGGER.debug("Removing subscription: {}", subscriptionId);
      ServiceRegistration sr = (ServiceRegistration) existingSubscriptions.get(subscriptionId);
      if (sr != null) {
        sr.unregister();
        LOGGER.debug("Removal complete");
        existingSubscriptions.remove(subscriptionId);
      } else {
        LOGGER.debug(
            "Unable to find existing subscription: {}.  May already be deleted.", subscriptionId);
      }

    } catch (Exception e) {
      LOGGER.debug("Could not delete subscription for {}", subscriptionId);
      LOGGER.info("Exception deleting subscription", e);
    }

    LOGGER.debug("EXITING: " + methodName);
  }

  @Override
  public void notifyCreated(Metacard newMetacard) {
    LOGGER.trace("ENTERING: notifyCreated");
    postEvent(EventProcessor.EVENTS_TOPIC_CREATED, newMetacard, null);
    LOGGER.trace("EXITING: notifyCreated");
  }

  @Override
  public void notifyUpdated(Metacard newMetacard, Metacard oldMetacard) {
    LOGGER.trace("ENTERING: notifyUpdated");
    postEvent(EventProcessor.EVENTS_TOPIC_UPDATED, newMetacard, oldMetacard);
    LOGGER.trace("EXITING: notifyUpdated");
  }

  @Override
  public void notifyDeleted(Metacard oldMetacard) {
    LOGGER.trace("ENTERING: notifyDeleted");
    postEvent(EventProcessor.EVENTS_TOPIC_DELETED, oldMetacard, null);
    LOGGER.trace("EXITING: notifyDeleted");
  }

  /**
   * Posts a Metacard to a given topic
   *
   * @param topic - The topic to post the event
   * @param card - The Metacard that will be posted to the topic
   */
  protected void postEvent(String topic, Metacard card, Metacard oldCard) {
    String methodName = "postEvent";
    LOGGER.debug("ENTERING: {}", methodName);

    LOGGER.debug("Posting to topic: {}", topic);

    Dictionary<String, Object> properties = new Hashtable<>(2, 1);
    properties.put(EventProcessor.EVENT_METACARD, card);
    properties.put(EventProcessor.EVENT_TIME, System.currentTimeMillis());
    Event event = new Event(topic, properties);
    eventAdmin.postEvent(event);

    LOGGER.debug("EXITING: {}", methodName);
  }

  @Override
  public CreateResponse process(CreateResponse createResponse) throws PluginExecutionException {
    LOGGER.trace("ENTERING: process (CreateResponse");
    if (Requests.isLocal(createResponse.getRequest())) {
      List<Metacard> createdMetacards = createResponse.getCreatedMetacards();
      for (Metacard currMetacard : createdMetacards) {
        postEvent(EventProcessor.EVENTS_TOPIC_CREATED, currMetacard, null);
      }
    }
    LOGGER.trace("EXITING: process (CreateResponse)");
    return createResponse;
  }

  @Override
  public UpdateResponse process(UpdateResponse updateResponse) throws PluginExecutionException {
    LOGGER.trace("ENTERING: process (UpdateResponse");
    if (Requests.isLocal(updateResponse.getRequest())) {
      List<Update> updates = updateResponse.getUpdatedMetacards();
      for (Update currUpdate : updates) {
        postEvent(
            EventProcessor.EVENTS_TOPIC_UPDATED,
            currUpdate.getNewMetacard(),
            currUpdate.getOldMetacard());
      }
    }
    LOGGER.trace("EXITING: process (UpdateResponse)");
    return updateResponse;
  }

  @Override
  public DeleteResponse process(DeleteResponse deleteResponse) throws PluginExecutionException {
    LOGGER.trace("ENTERING: process (DeleteResponse");
    if (Requests.isLocal(deleteResponse.getRequest())) {
      List<Metacard> deletedMetacards = deleteResponse.getDeletedMetacards();
      for (Metacard currMetacard : deletedMetacards) {
        postEvent(EventProcessor.EVENTS_TOPIC_DELETED, currMetacard, null);
      }
    }
    LOGGER.trace("EXITING: process (DeleteResponse)");
    return deleteResponse;
  }

  /**
   * Enumeration of metacard Date attributes that can be used for subscriptions. In order to use
   * metacard attribute names for getting DateType values, {@link DateType#getDateType(String)}
   * should be used. To get the metacard attribute name for a DateType, {@link
   * DateType#getAttributeName()} should be used, where "dt" is an instance of DateType.
   *
   * <p>The standard {@link DateType#valueOf(String)} and {@link DateType#name()} enum methods will
   * not use the names of the attributes as they appear on the metacard, but as they appear in the
   * enum class as defined below.
   */
  public enum DateType {
    MODIFIED,
    EFFECTIVE,
    EXPIRATION,
    CREATED,
    METACARD_CREATED(Core.METACARD_CREATED),
    METACARD_MODIFIED(Core.METACARD_MODIFIED);

    private final String attributeName;

    DateType() {
      attributeName = this.name();
    }

    DateType(String attributeName) {
      this.attributeName = attributeName;
    }

    public String getAttributeName() {
      return attributeName;
    }

    public static DateType getDateType(String attr) {
      if (attr == null) {
        throw new NullPointerException("Provided DateTime attribute was null.");
      }

      for (DateType dt : DateType.values()) {
        if (dt.getAttributeName().equalsIgnoreCase(attr)) {
          return dt;
        }
      }
      throw new IllegalArgumentException(
          String.format("Provided DateTime attribute %s was not found.", attr));
    }
  }
}
