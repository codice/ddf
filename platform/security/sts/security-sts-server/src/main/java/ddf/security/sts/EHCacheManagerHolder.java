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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.apache.wss4j.common.util.Loader;

public final class EHCacheManagerHolder {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(EHCacheManagerHolder.class);
  private static final String NEW_INSTANCE = "newInstance";
  private static final String CREATE = "create";
  private static final ConcurrentHashMap<String, AtomicInteger> COUNTS =
      new ConcurrentHashMap<>(8, 0.75f, 2);
  private static Method cacheManagerCreateMethodNoArg;
  private static Method createMethodURLArg;
  private static Method cacheManagerCreateMethodConfigurationArg;

  static {
    // these methods are either completely available or absent (valid assumption from ehcache 2.5.0
    // to 2.7.2 so far)
    try {
      // from 2.5.2
      cacheManagerCreateMethodNoArg = CacheManager.class.getMethod(NEW_INSTANCE, (Class<?>[]) null);
      createMethodURLArg = CacheManager.class.getMethod(NEW_INSTANCE, URL.class);
      cacheManagerCreateMethodConfigurationArg =
          CacheManager.class.getMethod(NEW_INSTANCE, Configuration.class);
    } catch (NoSuchMethodException e) {
      try {
        // before 2.5.2
        cacheManagerCreateMethodNoArg = CacheManager.class.getMethod(CREATE, (Class<?>[]) null);
        createMethodURLArg = CacheManager.class.getMethod(CREATE, URL.class);
        cacheManagerCreateMethodConfigurationArg =
            CacheManager.class.getMethod(CREATE, Configuration.class);
      } catch (Exception ex) {
        // ignore
        LOG.warn(ex.getMessage());
      }
    }
  }

  private EHCacheManagerHolder() {
    // utility
  }

  public static CacheConfiguration getCacheConfiguration(String key, CacheManager cacheManager) {
    CacheConfiguration cc = cacheManager.getConfiguration().getCacheConfigurations().get(key);
    if (cc == null && key.contains("-")) {
      cc =
          cacheManager
              .getConfiguration()
              .getCacheConfigurations()
              .get(key.substring(0, key.lastIndexOf('-')));
    }
    if (cc == null) {
      cc = cacheManager.getConfiguration().getDefaultCacheConfiguration();
    }
    if (cc == null) {
      cc = new CacheConfiguration();
    } else {
      cc = cc.clone();
    }
    cc.setName(key);
    return cc;
  }

  public static synchronized CacheManager getCacheManager(String confName, URL configFileURL) {
    CacheManager cacheManager = null;
    if (configFileURL == null) {
      // using the default
      cacheManager = findDefaultCacheManager(confName);
    }
    if (cacheManager == null) {
      if (configFileURL == null) {
        cacheManager = createCacheManager();
      } else {
        cacheManager = findDefaultCacheManager(confName, configFileURL);
      }
    }
    if (cacheManager == null) {
      throw new IllegalStateException("Cache manager could not be found.");
    }
    AtomicInteger a = COUNTS.get(cacheManager.getName());
    if (a == null) {
      COUNTS.putIfAbsent(cacheManager.getName(), new AtomicInteger());
      a = COUNTS.get(cacheManager.getName());
    }
    a.incrementAndGet();
    return cacheManager;
  }

  private static CacheManager findDefaultCacheManager(String confName) {

    String defaultConfigFile = "/wss4j-ehcache.xml";
    URL configFileURL = null;
    try {
      configFileURL = Loader.getResource(defaultConfigFile);
      if (configFileURL == null) {
        configFileURL = new URL(defaultConfigFile);
      }
    } catch (IOException e) {
      // Do nothing
      LOG.debug(e.getMessage());
    }
    return findDefaultCacheManager(confName, configFileURL);
  }

  private static CacheManager findDefaultCacheManager(String confName, URL configFileURL) {
    try {
      Configuration conf = ConfigurationFactory.parseConfiguration(configFileURL);
      conf.setName(confName);
      if ("java.io.tmpdir".equals(conf.getDiskStoreConfiguration().getOriginalPath())) {
        String path = conf.getDiskStoreConfiguration().getPath() + File.separator + confName;
        conf.getDiskStoreConfiguration().setPath(path);
      }
      return createCacheManager(conf);
    } catch (Exception ex) {
      return null;
    }
  }

  public static synchronized void releaseCacheManger(CacheManager cacheManager) {
    AtomicInteger a = COUNTS.get(cacheManager.getName());
    if (a == null) {
      return;
    }
    if (a.decrementAndGet() == 0) {
      cacheManager.shutdown();
    }
  }

  static CacheManager createCacheManager() throws CacheException {
    try {
      return (CacheManager) cacheManagerCreateMethodNoArg.invoke(null, (Object[]) null);
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }

  static CacheManager createCacheManager(URL url) throws CacheException {
    try {
      return (CacheManager) createMethodURLArg.invoke(null, new Object[] {url});
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }

  static CacheManager createCacheManager(Configuration conf) throws CacheException {
    try {
      return (CacheManager)
          cacheManagerCreateMethodConfigurationArg.invoke(null, new Object[] {conf});
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }
}
