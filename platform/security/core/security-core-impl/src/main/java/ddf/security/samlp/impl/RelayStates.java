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
package ddf.security.samlp.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RelayStates<T> {

  Cache<String, T> encodedRelayStates =
      CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();

  public String encode(T relayState) {
    String id = UUID.randomUUID().toString();
    encodedRelayStates.put(id, relayState);

    return id;
  }

  public void encode(String key, T relayState) {
    encodedRelayStates.put(key, relayState);
  }

  public T decode(String relayState) {
    return decode(relayState, true);
  }

  public T decode(String relayState, boolean removeAfterDecode) {
    T decodedRelayState = encodedRelayStates.getIfPresent(relayState);

    if (removeAfterDecode && decodedRelayState != null) {
      encodedRelayStates.invalidate(relayState);
    }

    return decodedRelayState;
  }
}
