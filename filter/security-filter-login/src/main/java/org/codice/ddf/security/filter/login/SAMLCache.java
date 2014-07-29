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

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SAMLCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(SAMLCache.class);

    private ConcurrentHashMap<String, SecurityToken> cache = new ConcurrentHashMap<String, SecurityToken>();

    public void clearCache() {
        cache.clear();
    }

    /**
     * Puts the SecurityToken (with SAML assertion) into the cache and returns a String key
     * that can be used to retrieve the token in the future.
     *
     * @param token the SecurityToken containing the SAML assertion to be cached
     * @return the key for this element in the cache
     */
    public String put(String realm, SecurityToken token) {
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();
        LOGGER.trace("Adding security token to cache for realm {} and id {}", realm , id);
        cache.put(realm + id, token);
        return id;
    }

    /**
     * Retrieves the SecurityToken associated with the provided realm and unique identifier.
     *
     * @param realm the realm that this request is associated with
     * @param id the reference id for a SAML assertion
     * @return the SecurityToken object associated with the reference, or null
     */
    public SecurityToken get(String realm, String id) {
        SecurityToken token = cache.get(realm + id);
        LOGGER.trace("Retrieving security token with realm {} and id {} - {}.",
            realm, id, token == null ? "not found" : "found");
        return token;
    }

}
