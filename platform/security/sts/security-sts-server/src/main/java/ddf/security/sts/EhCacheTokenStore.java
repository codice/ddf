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
import java.util.concurrent.atomic.AtomicInteger;
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

@edu.umd.cs.findbugs.annotations.SuppressWarnings(
  value = "ML_SYNC_ON_FIELD_TO_GUARD_CHANGING_THAT_FIELD"
)
public class EHCacheTokenStore implements TokenStore, Closeable, BusLifeCycleListener {

  public static final long DEFAULT_TTL = 3600L;

  public static final long MAX_TTL = 43200L;

  private Ehcache cache;

  private Bus bus;

  private CacheManager cacheManager;

  private long ttl = 3600L;

  public EHCacheTokenStore(String key, Bus b, URL configFileURL) {
    this.bus = b;
    if (this.bus != null) {
      ((BusLifeCycleManager) b.getExtension(BusLifeCycleManager.class))
          .registerLifeCycleListener(this);
    }

    this.cacheManager = EHCacheUtils.getCacheManager(this.bus, configFileURL);
    CacheConfiguration cc =
        EHCacheManagerHolder.getCacheConfiguration(key, this.cacheManager).overflowToDisk(false);
    EHCacheTokenStore.RefCountCache newCache = new EHCacheTokenStore.RefCountCache(cc);
    this.cache = this.cacheManager.addCacheIfAbsent(newCache);
    Ehcache var6 = this.cache;
    synchronized (this.cache) {
      if (this.cache.getStatus() != Status.STATUS_ALIVE) {
        this.cache = this.cacheManager.addCacheIfAbsent(newCache);
      }

      if (this.cache instanceof EHCacheTokenStore.RefCountCache) {
        ((EHCacheTokenStore.RefCountCache) this.cache).incrementAndGet();
      }
    }

    this.ttl = cc.getTimeToLiveSeconds();
  }

  public void setTTL(long newTtl) {
    this.ttl = newTtl;
  }

  public void add(SecurityToken token) {
    if (token != null && !StringUtils.isEmpty(token.getId())) {
      Element element = new Element(token.getId(), token, this.getTTL(), this.getTTL());
      element.resetAccessStatistics();
      this.cache.put(element);
    }
  }

  public void add(String identifier, SecurityToken token) {
    if (token != null && !StringUtils.isEmpty(identifier)) {
      Element element = new Element(identifier, token, this.getTTL(), this.getTTL());
      element.resetAccessStatistics();
      this.cache.put(element);
    }
  }

  public void remove(String identifier) {
    if (this.cache != null
        && !StringUtils.isEmpty(identifier)
        && this.cache.isKeyInCache(identifier)) {
      this.cache.remove(identifier);
    }
  }

  public Collection<String> getTokenIdentifiers() {
    return this.cache == null ? null : this.cache.getKeysWithExpiryCheck();
  }

  public SecurityToken getToken(String identifier) {
    if (this.cache == null) {
      return null;
    } else {
      Element element = this.cache.get(identifier);
      return element != null && !this.cache.isExpired(element)
          ? (SecurityToken) element.getObjectValue()
          : null;
    }
  }

  private int getTTL() {
    int parsedTTL = (int) this.ttl;
    if (this.ttl != (long) parsedTTL) {
      parsedTTL = 3600;
    }

    return parsedTTL;
  }

  public void close() {
    if (this.cacheManager != null) {
      if (this.cache != null) {
        Ehcache var1 = this.cache;
        synchronized (this.cache) {
          if (this.cache instanceof EHCacheTokenStore.RefCountCache
              && ((EHCacheTokenStore.RefCountCache) this.cache).decrementAndGet() == 0) {
            this.cacheManager.removeCache(this.cache.getName());
          }
        }
      }

      EHCacheManagerHolder.releaseCacheManger(this.cacheManager);
      this.cacheManager = null;
      this.cache = null;
      if (this.bus != null) {
        ((BusLifeCycleManager) this.bus.getExtension(BusLifeCycleManager.class))
            .unregisterLifeCycleListener(this);
      }
    }
  }

  public void initComplete() {}

  public void preShutdown() {
    this.close();
  }

  public void postShutdown() {
    this.close();
  }

  private static class RefCountCache extends Cache {
    AtomicInteger count = new AtomicInteger();

    RefCountCache(CacheConfiguration cc) {
      super(cc);
    }

    public int incrementAndGet() {
      return this.count.incrementAndGet();
    }

    public int decrementAndGet() {
      return this.count.decrementAndGet();
    }
  }
}
