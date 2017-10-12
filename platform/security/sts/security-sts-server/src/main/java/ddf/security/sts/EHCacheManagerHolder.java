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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EHCacheManagerHolder {
  private static final Logger LOG =
      LoggerFactory.getLogger(org.apache.wss4j.common.cache.EHCacheManagerHolder.class);

  private static final ConcurrentHashMap<String, AtomicInteger> COUNTS =
      new ConcurrentHashMap(8, 0.75F, 2);

  private static Method cacheManagerCreateMethodNoArg;

  private static Method createMethodURLArg;

  private static Method cacheManagerCreateMethodConfigurationArg;

  private EHCacheManagerHolder() {}

  public static CacheConfiguration getCacheConfiguration(String key, CacheManager cacheManager) {
    CacheConfiguration cc =
        (CacheConfiguration) cacheManager.getConfiguration().getCacheConfigurations().get(key);
    if (cc == null && key.contains("-")) {
      cc =
          (CacheConfiguration)
              cacheManager
                  .getConfiguration()
                  .getCacheConfigurations()
                  .get(key.substring(0, key.lastIndexOf(45)));
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
      cacheManager = findDefaultCacheManager(confName);
    }

    if (cacheManager == null) {
      if (configFileURL == null) {
        cacheManager = createCacheManager();
      } else {
        cacheManager = findDefaultCacheManager(confName, configFileURL);
      }
    }

    AtomicInteger a = (AtomicInteger) COUNTS.get(cacheManager.getName());
    if (a == null) {
      COUNTS.putIfAbsent(cacheManager.getName(), new AtomicInteger());
      a = (AtomicInteger) COUNTS.get(cacheManager.getName());
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
    } catch (IOException var4) {
      LOG.debug(var4.getMessage());
    }

    return findDefaultCacheManager(confName, configFileURL);
  }

  private static CacheManager findDefaultCacheManager(String confName, URL configFileURL) {
    try {
      Configuration t = ConfigurationFactory.parseConfiguration(configFileURL);
      t.setName(confName);
      if ("java.io.tmpdir".equals(t.getDiskStoreConfiguration().getOriginalPath())) {
        String path = t.getDiskStoreConfiguration().getPath() + File.separator + confName;
        t.getDiskStoreConfiguration().setPath(path);
      }

      return createCacheManager(t);
    } catch (Throwable var4) {
      return null;
    }
  }

  public static synchronized void releaseCacheManger(CacheManager cacheManager) {
    AtomicInteger a = (AtomicInteger) COUNTS.get(cacheManager.getName());
    if (a != null) {
      if (a.decrementAndGet() == 0) {
        cacheManager.shutdown();
      }
    }
  }

  static CacheManager createCacheManager() throws CacheException {
    try {
      return (CacheManager) cacheManagerCreateMethodNoArg.invoke((Object) null, (Object[]) null);
    } catch (Exception var1) {
      throw new CacheException(var1);
    }
  }

  static CacheManager createCacheManager(URL url) throws CacheException {
    try {
      return (CacheManager) createMethodURLArg.invoke((Object) null, new Object[] {url});
    } catch (Exception var2) {
      throw new CacheException(var2);
    }
  }

  static CacheManager createCacheManager(Configuration conf) throws CacheException {
    try {
      return (CacheManager)
          cacheManagerCreateMethodConfigurationArg.invoke((Object) null, new Object[] {conf});
    } catch (Exception var2) {
      throw new CacheException(var2);
    }
  }

  static {
    try {
      cacheManagerCreateMethodNoArg = CacheManager.class.getMethod("newInstance", (Class[]) null);
      createMethodURLArg = CacheManager.class.getMethod("newInstance", new Class[] {URL.class});
      cacheManagerCreateMethodConfigurationArg =
          CacheManager.class.getMethod("newInstance", new Class[] {Configuration.class});
    } catch (NoSuchMethodException var3) {
      try {
        cacheManagerCreateMethodNoArg = CacheManager.class.getMethod("create", (Class[]) null);
        createMethodURLArg = CacheManager.class.getMethod("create", new Class[] {URL.class});
        cacheManagerCreateMethodConfigurationArg =
            CacheManager.class.getMethod("create", new Class[] {Configuration.class});
      } catch (Throwable var2) {
        LOG.warn(var2.getMessage());
      }
    }
  }
}
