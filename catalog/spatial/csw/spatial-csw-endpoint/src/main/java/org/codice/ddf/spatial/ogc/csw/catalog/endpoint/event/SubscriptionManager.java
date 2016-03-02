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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.InvalidSubscriptionException;
import ddf.catalog.event.Subscription;

/**
 * SubscriptionManager manages registeredSubscriptions for add, delete, and persistence.
 */
public class SubscriptionManager {

    /**
     * The log4j BASE_LOGGER for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionManager.class);

    /**
     * The ManagedServiceFactory PID for the subscription's callback web service adapter,
     * used to dynamically instantiate the web service's adapter
     */
    private static final String FACTORY_PID = "CSW_Subscription";

    public static final String SUBSCRIPTION_ID = "subscriptionId";

    public static final String FILTER_XML = "filterXml";

    public static final String DELIVERY_METHOD_URL = "deliveryMethodUrl";

    public static final String SUBSCRIPTION_UUID = "subscriptionUuid";

    private Map<String, ServiceRegistration<Subscription>> registeredSubscriptions =
            new ConcurrentHashMap<>();

    public boolean hasSubscription(String subscriptionId) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subscriptionUuid = {}", subscriptionId);
        }

        return registeredSubscriptions.containsKey(subscriptionId);
    }

    /**
     * Concatenate the subscription Id and deliveryMethodUrl to create subscriptionUuid.
     *
     * @param subscriptionId - in String format
     * @return a subscriptionUuid filter in String format
     * @parm deliveryMethodUrl
     * - in String format
     */
    public String getSubscriptionUuid(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            return UUID.randomUUID()
                    .toString();
        }
        return subscriptionId;
    }

    public Subscription getSubscription(BundleContext context, String subscriptionId) {
        ServiceRegistration sr = (ServiceRegistration) registeredSubscriptions.get(subscriptionId);
        if (sr == null) {
            return null;
        }
        return (Subscription) context.getService(sr.getReference());
    }

    public String updateSubscription(BundleContext context, Subscription sub, String subscriptionId,
            String deliveryMethodUrl) throws InvalidSubscriptionException {
        if (!hasSubscription(subscriptionId)) {
            throw new InvalidSubscriptionException(
                    "Unable to update subscription because subscription ID was not found");
        }
        return addOrUpdateSubscription(context, sub, subscriptionId, deliveryMethodUrl);
    }

    public String addSubscription(BundleContext context, Subscription sub, String deliveryMethodUrl)
            throws InvalidSubscriptionException {
        return addOrUpdateSubscription(context, sub, null, deliveryMethodUrl);
    }

    public String addOrUpdateSubscription(BundleContext context, Subscription sub,
            String subscriptionId, String deliveryMethodUrl) throws InvalidSubscriptionException {
        String methodName = "createSubscription";
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ENTERING: {}    (persistSubscription = {})", methodName);
        }

        if (StringUtils.isEmpty(deliveryMethodUrl)) {
            throw new InvalidSubscriptionException(
                    "Unable to create subscription because deliveryMethodUrl is null or empty");
        }

        // Create unique subscription ID. The client provided subscription ID is not guaranteed to be unique across
        // all registeredSubscriptions, especially if multiple clients are adding registeredSubscriptions. Hence the need to generate a
        // subscription ID unique across all clients by concatenating the client-provided subscription ID with the
        // client-provided delivery method URL.
        String subscriptionUuid = getSubscriptionUuid(subscriptionId);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("subscriptionUuid = {}", subscriptionUuid);
        }

        Dictionary<String, String> props = new Hashtable<>();
        props.put("subscription-id", subscriptionUuid);

        // If this subscription already exists, then delete it and re-add it
        // to registry
        if (registeredSubscriptions.containsKey(subscriptionUuid)) {
            LOGGER.debug("Delete existing subscription {} for re-creation", subscriptionUuid);
            deleteSubscription(context, subscriptionUuid);
        }

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
            //            if(sub instanceof Serializable)
            //            {
            //                persistSubscription(context,
            //                        sub,
            //                        deliveryMethodUrl,
            //                        subscriptionId,
            //                        subscriptionUuid);
            //            }
        } else {
            LOGGER.debug("Subscription registration failed");
        }

        LOGGER.debug("EXITING: {}", methodName);
        return subscriptionUuid;

    }

    public boolean deleteSubscription(BundleContext context, String subscriptionId)
            throws InvalidSubscriptionException {
        String methodName = "deleteSubscription";
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ENTERING: {}", methodName);
            LOGGER.debug("subscriptionId = {}", subscriptionId);
        }

        if (StringUtils.isEmpty(subscriptionId)) {
            throw new InvalidSubscriptionException(
                    "Unable to delete subscription because subscription ID is null or empty");
        }

        boolean status = false;

        try {
            LOGGER.debug("Removing (unregistering) subscription: {}", subscriptionId);
            ServiceRegistration sr = (ServiceRegistration) registeredSubscriptions.get(
                    subscriptionId);
            if (sr != null) {
                sr.unregister();
            } else {
                LOGGER.debug("No ServiceRegistration found for subscription: {}", subscriptionId);
            }

            Configuration subscriptionConfig = getSubscriptionConfiguration(context,
                    subscriptionId);
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

            LOGGER.debug("Removing subscription from internal hash map");
            registeredSubscriptions.remove(subscriptionId);
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
    protected void persistSubscription(BundleContext context, Subscription subscription,
            String deliveryMethodUrl, String subscriptionId, String subscriptionUuid) {
        String methodName = "persistSubscription";
        LOGGER.debug("ENTERING: {}", methodName);

        // Convert Subscription (which is an OGC Filter) to XML for persistence into OSGi CongiAdmin
        String filterXml = null;

        // Store filter XML, deliveryMethod URL, this endpoint's factory PID, and subscription ID into OSGi CongiAdmin
        if (filterXml != null) {
            try {
                Configuration config = getConfigAdmin(context).createFactoryConfiguration(
                        FACTORY_PID,
                        null);

                Dictionary<String, String> props = new Hashtable<String, String>();
                props.put(SUBSCRIPTION_ID, subscriptionId);
                props.put(FILTER_XML, filterXml);
                props.put(DELIVERY_METHOD_URL, deliveryMethodUrl);
                props.put(SUBSCRIPTION_UUID, subscriptionUuid);

                LOGGER.debug("Done adding persisting subscription to ConfigAdmin");

                config.update(props);
            } catch (IOException e) {
                LOGGER.warn("Unable to persist subscription " + subscriptionId, e);
            }
        }

        LOGGER.debug("EXITING: {}", methodName);
    }

    private ConfigurationAdmin getConfigAdmin(BundleContext context) {
        ConfigurationAdmin configAdmin = null;

        ServiceReference configAdminRef =
                context.getServiceReference(ConfigurationAdmin.class.getName());

        if (configAdminRef != null) {
            configAdmin = (ConfigurationAdmin) context.getService(configAdminRef);
        }

        return configAdmin;
    }

    private Configuration getSubscriptionConfiguration(BundleContext context,
            String subscriptionUuid) {
        String methodName = "getSubscriptionConfiguration";
        LOGGER.debug("ENTERING: {}", methodName);

        String filterStr = getSubscriptionUuidFilter(subscriptionUuid);
        LOGGER.debug("filterStr = {}", filterStr);

        Configuration config = null;

        try {
            org.osgi.framework.Filter filter = context.createFilter(filterStr);
            LOGGER.debug("filter.toString() = {}", filter.toString());

            Configuration[] configs = getConfigAdmin(context).listConfigurations(filter.toString());

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
    public String getSubscriptionUuidFilter(String subscriptionUuid) {

        return "(subscriptionUuid=" + subscriptionUuid + ")";

    }

}
