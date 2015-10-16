/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.client;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class RelayStates {

    Cache<String, String> encodedRelayStates = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES).build();

    public String encode(String relayState) {
        String id = UUID.randomUUID().toString();
        encodedRelayStates.put(id, relayState);

        return id;
    }

    public String decode(String relayState) {
        String decodedRelayState = encodedRelayStates.getIfPresent(relayState);

        if (decodedRelayState != null) {
            encodedRelayStates.invalidate(relayState);
        }

        return decodedRelayState;
    }

}
