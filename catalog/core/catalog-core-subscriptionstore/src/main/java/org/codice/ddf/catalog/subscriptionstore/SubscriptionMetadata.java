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
package org.codice.ddf.catalog.subscriptionstore;

import static java.lang.String.format;
import static org.apache.commons.lang.Validate.notNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.event.Subscription;

/**
 * {@link SubscriptionMetadata} wraps a subscription provider-specific message, typically in serialized
 * form. This can be an HTTP body for a REST endpoint or any data structure that results in a valid
 * {@link ddf.catalog.event.Subscription} when provided to an appropriate
 * {@link org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory}. SubscriptionMetadata
 * also exposes additional, top-level metadata about the subscription (type and id) making it useful for
 * querying the central {@link SubscriptionStore} in various ways.
 * <p>
 * Every unique consumer of the central subscription service must have a unique <i>type</i> for their
 * subscriptions, and their instances of SubscriptionMetadata must always reflect this.
 */
public class SubscriptionMetadata {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionMetadata.class);

    private static final String URN_UUID = "urn:uuid:";

    private final String id;

    private final String type;

    // TODO: Make byte array and use hashing algorithm - Solr's version always wins
    private final String messageBody;

    private final URL callbackUrl;

    private ServiceRegistration registration;

    private Subscription subscription;

    public SubscriptionMetadata(String type, String messageBody, String callbackUrl) {
        this(type,
                messageBody,
                callbackUrl,
                URN_UUID + UUID.randomUUID()
                        .toString());
    }

    SubscriptionMetadata(String type, String messageBody, String callbackUrl, String id) {
        this.type = type;
        this.messageBody = messageBody;
        this.callbackUrl = validateCallbackUrl(callbackUrl);
        this.id = id;

        this.registration = null;
        this.subscription = null;

        logCreated();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public URL getCallbackUrl() {
        return callbackUrl;
    }

    public void setRegistration(ServiceRegistration registration) {
        notNull(registration, "Service registration cannot be null");
        this.registration = registration;
    }

    public Optional<ServiceRegistration> getRegistration() {
        return Optional.ofNullable(registration);
    }

    public void setSubscription(Subscription subscription) {
        notNull(subscription, "Cached subscription object cannot be null");
        this.subscription = subscription;
    }

    public Optional<Subscription> getSubscription() {
        return Optional.ofNullable(subscription);
    }

    private URL validateCallbackUrl(String callbackUrl) {
        URL url;
        try {
            url = URI.create(callbackUrl)
                    .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(format(
                    "Invalid subscription request: callback URL [%s] was malformed",
                    callbackUrl));
        }
        return url;
    }

    private void logCreated() {
        LOGGER.debug("Created subscription request object: {} | {} | {}",
                id,
                type,
                callbackUrl.toString());
    }
}
