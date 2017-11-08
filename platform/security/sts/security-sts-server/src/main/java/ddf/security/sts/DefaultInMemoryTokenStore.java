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

import static org.apache.cxf.common.classloader.ClassLoaderUtils.getResource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;

public class DefaultInMemoryTokenStore extends EHCacheTokenStore {

  public DefaultInMemoryTokenStore(Bus b) {
    super("STS", b, getResource("cxf-ehcache.xml", DefaultInMemoryTokenStore.class));
  }

  public DefaultInMemoryTokenStore() {
    super(
        "STS",
        BusFactory.getDefaultBus(),
        getResource("cxf-ehcache.xml", DefaultInMemoryTokenStore.class));
  }
}
