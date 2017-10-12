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

import java.net.URL;
import net.sf.ehcache.CacheManager;
import org.apache.cxf.Bus;

public final class EHCacheUtils {
  public static final String GLOBAL_EHCACHE_MANAGER_NAME = "ws-security.global.ehcachemanager";

  private EHCacheUtils() {}

  public static CacheManager getCacheManager(Bus bus, URL configFileURL) {
    CacheManager cacheManager = null;

    String globalCacheManagerName = getGlobalCacheManagerName(bus);
    if (globalCacheManagerName != null) {
      cacheManager = CacheManager.getCacheManager(globalCacheManagerName);
    }

    if (cacheManager == null) {
      String confName = "";
      if (bus != null) {
        confName = bus.getId();
      }
      cacheManager = EHCacheManagerHolder.getCacheManager(confName, configFileURL);
    }
    return cacheManager;
  }

  private static String getGlobalCacheManagerName(Bus bus) {
    if (bus != null) {
      return (String) bus.getProperty(GLOBAL_EHCACHE_MANAGER_NAME);
    } else {
      return null;
    }
  }
}
