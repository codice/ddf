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
package org.codice.ddf.security.filter.login;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SAMLCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLCache.class);

    private static final int DEFAULT_EXPIRATION_MINUTES = 31;

    private int currentExpiration = DEFAULT_EXPIRATION_MINUTES;

    private Cache<String, SecurityToken> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(DEFAULT_EXPIRATION_MINUTES,
                    TimeUnit.MINUTES).removalListener(new RemovalListenerLogger()).build();

    public void clearCache() {
        cache.invalidateAll();
    }

    /**
     * Puts the SecurityToken (with SAML assertion) into the cache and returns a String key
     * that can be used to retrieve the token in the future.
     *
     * @param token the SecurityToken containing the SAML assertion to be cached
     * @return the key for this element in the cache
     */
    public String put(String realm, SecurityToken token) {
        String id = UUID.randomUUID().toString();
        LOGGER.trace("Adding security token to cache for realm {} and id {}", realm, id);
        cache.put(realm + id, token);
        return id;
    }

    /**
     * Retrieves the SecurityToken associated with the provided realm and unique identifier.
     *
     * @param realm the realm that this request is associated with
     * @param id    the reference id for a SAML assertion
     * @return the SecurityToken object associated with the reference, or null
     */
    public SecurityToken get(String realm, String id) {
        SecurityToken token = cache.getIfPresent(realm + id);
        LOGGER.trace("Retrieving security token with realm {} and id {} - {}.",
                realm, id, token == null ? "not found" : "found");
        return token;
    }

    /**
     * Set the expiration time for the cache. <br/><br/>
     * Note: This will also reset the expiration times of any items currently in the cache.
     *
     * @param expirationMinutes Value (in minutes) to set the cache expiration to.
     */
    public synchronized void setExpirationTime(int expirationMinutes) {
        if (expirationMinutes != currentExpiration) {
            LOGGER.debug(
                    "New expiration value passed in. Changing cache to expire every {} minutes instead of every {}.",
                    expirationMinutes, currentExpiration);
            Cache<String, SecurityToken> tmpCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(expirationMinutes, TimeUnit.MINUTES)
                    .removalListener(new RemovalListenerLogger()).build();
            LOGGER.debug("Adding old cache items to cache with updated expiration.");
            tmpCache.putAll(cache.asMap());
            cache = tmpCache;
            currentExpiration = expirationMinutes;
        } else {
            LOGGER.debug("Incoming time of {} matches current expiration time. Not updating cache.",
                    expirationMinutes);
        }
    }

    /**
     * Listens for removal notifications from the cache and logs each time a removal is performed.
     */
    private class RemovalListenerLogger implements RemovalListener<String, SecurityToken> {

        @Override
        public void onRemoval(RemovalNotification<String, SecurityToken> notification) {
            LOGGER.debug("Expiring SAML ref:assertion {}:{} due to {}.",
                    notification.getKey(),
                    notification.getValue(), notification.getCause().toString());
        }
    }

}
