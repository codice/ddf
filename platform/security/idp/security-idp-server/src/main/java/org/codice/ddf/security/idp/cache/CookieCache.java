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
package org.codice.ddf.security.idp.cache;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class CookieCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(CookieCache.class);

    private static final int DEFAULT_EXPIRATION_MINUTES = 30;

    private int currentExpiration = DEFAULT_EXPIRATION_MINUTES;

    private Cache<String, DataWrapper> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(DEFAULT_EXPIRATION_MINUTES, TimeUnit.MINUTES)
            .removalListener(new RemovalListenerLogger())
            .build();

    public void clearCache() {
        cache.invalidateAll();
    }

    /**
     * Puts the SAML assertion into the cache
     *
     * @param key   key corresponding to the SAML assertion
     * @param token the SAML assertion to be cached
     */
    public void cacheSamlAssertion(String key, Element token) {
        cache.put(key, new DataWrapper(token));
    }

    public void addActiveSp(String key, String activeSp) {
        DataWrapper dataWrapper = cache.getIfPresent(key);
        if (dataWrapper == null) {
            LOGGER.error("Cannot add sp [{}] for [{}]: does not exist", activeSp, key);
            return;
        }
        synchronized (dataWrapper) {
            dataWrapper.activeSpSet.add(activeSp);
        }
    }

    /**
     * Retrieves the SAML assertion associated with the provided key.
     *
     * @param key the corresponding key for the assertion
     * @return the SecurityToken object associated with the reference, or null
     */
    public Element getSamlAssertion(String key) {
        DataWrapper dataWrapper = cache.getIfPresent(key);
        if (dataWrapper != null) {
            return dataWrapper.element;
        }
        return null;
    }

    public void removeSamlAssertion(String key) {
        DataWrapper dataWrapper = cache.getIfPresent(key);
        if (dataWrapper != null) {
            LOGGER.debug("Expiring Saml assertion due to LogoutRequest\n[{}:{}]",
                    key,
                    dataWrapper.element);
            dataWrapper.element = null;
        }
    }

    public Set<String> getActiveSpSet(String key) {
        DataWrapper dataWrapper = cache.getIfPresent(key);
        if (dataWrapper != null) {
            synchronized (dataWrapper) {
                return new HashSet<>(dataWrapper.activeSpSet);
            }
        }
        return new HashSet<>();
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
                    expirationMinutes,
                    currentExpiration);
            Cache<String, DataWrapper> tmpCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(expirationMinutes, TimeUnit.MINUTES)
                    .removalListener(new RemovalListenerLogger())
                    .build();
            tmpCache.putAll(cache.asMap());
            LOGGER.debug("All cache items updated to expire after {} minutes.", expirationMinutes);
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
    private static class RemovalListenerLogger implements RemovalListener<String, DataWrapper> {

        @Override
        public void onRemoval(RemovalNotification<String, DataWrapper> notification) {
            LOGGER.debug("Expiring SAML ref:assertion {}:{} due to {}.",
                    notification.getKey(),
                    notification.getValue(),
                    notification.getCause()
                            .toString());
        }
    }

    private static class DataWrapper {
        private Element element;

        /**
         * Data access to this variable should be synchronized
         */
        private final Set<String> activeSpSet;

        private DataWrapper(Element element) {
            this.element = element;
            this.activeSpSet = new HashSet<>();
        }
    }

}
