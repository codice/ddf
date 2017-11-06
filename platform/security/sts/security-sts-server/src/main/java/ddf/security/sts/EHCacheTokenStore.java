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
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ddf.security.sts;

import java.io.Closeable;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;

public class EHCacheTokenStore implements TokenStore, Closeable, BusLifeCycleListener {

  public static final long DEFAULT_TTL = 3600L;
  public static final long MAX_TTL = DEFAULT_TTL * 12L;

  private Ehcache cache;
  private final ReentrantLock lock = new ReentrantLock();
  private Bus bus;
  private CacheManager cacheManager;
  private long ttl = DEFAULT_TTL;

  public EHCacheTokenStore(String key, Bus b, URL configFileURL) {
    bus = b;
    if (bus != null) {
      b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
    }
    cacheManager = EHCacheUtils.getCacheManager(bus, configFileURL);
    // Cannot overflow to disk as SecurityToken Elements can't be serialized
    @SuppressWarnings("deprecation")
    CacheConfiguration cc =
        EHCacheManagerHolder.getCacheConfiguration(key, cacheManager); // tokens not writable

    Cache newCache = new RefCountCache(cc);
    cache = cacheManager.addCacheIfAbsent(newCache);
    lock.lock();
    try {
      if (cache.getStatus() != Status.STATUS_ALIVE) {
        cache = cacheManager.addCacheIfAbsent(newCache);
      }
      if (cache instanceof RefCountCache) {
        ((RefCountCache) cache).incrementAndGet();
      }
    } finally {
      lock.unlock();
    }

    // Set the TimeToLive value from the CacheConfiguration
    ttl = cc.getTimeToLiveSeconds();
  }

  @SuppressWarnings("squid:S2160" /* Relying on superclass equals/hashcode methods */)
  private static class RefCountCache extends Cache {
    AtomicInteger count = new AtomicInteger();

    RefCountCache(CacheConfiguration cc) {
      super(cc);
    }

    public int incrementAndGet() {
      return count.incrementAndGet();
    }

    public int decrementAndGet() {
      return count.decrementAndGet();
    }
  }

  /**
   * Set a new (default) TTL value in seconds
   *
   * @param newTtl a new (default) TTL value in seconds
   */
  public void setTTL(long newTtl) {
    ttl = newTtl;
  }

  public void add(SecurityToken token) {
    if (token != null && !StringUtils.isEmpty(token.getId())) {
      Element element = new Element(token.getId(), token, getTTL(), getTTL());
      element.resetAccessStatistics();
      cache.put(element);
    }
  }

  public void add(String identifier, SecurityToken token) {
    if (token != null && !StringUtils.isEmpty(identifier)) {
      Element element = new Element(identifier, token, getTTL(), getTTL());
      element.resetAccessStatistics();
      cache.put(element);
    }
  }

  public void remove(String identifier) {
    if (cache != null && !StringUtils.isEmpty(identifier) && cache.isKeyInCache(identifier)) {
      cache.remove(identifier);
    }
  }

  @SuppressWarnings("unchecked")
  public Collection<String> getTokenIdentifiers() {
    if (cache == null) {
      return Collections.emptyList();
    }
    return cache.getKeysWithExpiryCheck();
  }

  public SecurityToken getToken(String identifier) {
    if (cache == null) {
      return null;
    }
    Element element = cache.get(identifier);
    if (element != null && !cache.isExpired(element)) {
      return (SecurityToken) element.getObjectValue();
    }
    return null;
  }

  private int getTTL() {
    int parsedTTL = (int) ttl;
    if (ttl != (long) parsedTTL) {
      // Fall back to 60 minutes if the default TTL is set incorrectly
      parsedTTL = 3600;
    }
    return parsedTTL;
  }

  public void close() {
    if (cacheManager != null) {
      // this step is especially important for global shared cache manager
      if (cache != null) {
        lock.lock();
        try {
          if (cache instanceof RefCountCache && ((RefCountCache) cache).decrementAndGet() == 0) {
            cacheManager.removeCache(cache.getName());
          }
        } finally {
          lock.unlock();
        }
      }

      EHCacheManagerHolder.releaseCacheManger(cacheManager);
      cacheManager = null;
      cache = null;
      if (bus != null) {
        bus.getExtension(BusLifeCycleManager.class).unregisterLifeCycleListener(this);
      }
    }
  }

  public void initComplete() {
    // not implemented; required by contract
  }

  public void preShutdown() {
    close();
  }

  public void postShutdown() {
    close();
  }
}
