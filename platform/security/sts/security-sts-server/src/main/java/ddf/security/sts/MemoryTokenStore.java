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
package ddf.security.sts;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryTokenStore implements TokenStore, Closeable, BusLifeCycleListener {

  private static final Cache<String, SecurityToken> CACHE =
      CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(1000).build();

  private static final Logger LOGGER = LoggerFactory.getLogger(MemoryTokenStore.class);

  @Override
  public void close() {
    LOGGER.trace("Cleaning up token cache");
    CACHE.invalidateAll();
    CACHE.cleanUp();
  }

  @Override
  public void initComplete() {
    // No-Op
  }

  @Override
  public void preShutdown() {
    close();
  }

  @Override
  public void postShutdown() {
    close();
  }

  @Override
  public void add(SecurityToken securityToken) {
    LOGGER.trace("Adding token: {}", securityToken.getId());
    CACHE.put(securityToken.getId(), securityToken);
  }

  @Override
  public void add(String identifier, SecurityToken securityToken) {
    LOGGER.trace("Adding token by id: {}", identifier);
    CACHE.put(identifier, securityToken);
  }

  @Override
  public void remove(String identifier) {
    LOGGER.trace("Removing token: {}", identifier);
    CACHE.invalidate(identifier);
  }

  @Override
  public Collection<String> getTokenIdentifiers() {
    return Collections.unmodifiableCollection(CACHE.asMap().keySet());
  }

  @Override
  public SecurityToken getToken(String identifier) {
    return CACHE.getIfPresent(identifier);
  }
}
