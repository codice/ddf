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
package org.codice.ddf.itests.common.opensearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.ServiceManager;
import org.osgi.service.cm.Configuration;

public class OpenSearchFeature {

  public static final String SYMBOLIC_NAME = "catalog-opensearch-source";

  public static final String FACTORY_PID = "OpenSearchSource";

  public static Configuration createManagedService(
      ServiceManager serviceManager, String sourceId, String user, String pass) throws IOException {
    Map propertyMap = new HashMap();
    propertyMap.putAll(serviceManager.getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
    propertyMap.put("endpointUrl", AbstractIntegrationTest.OPENSEARCH_PATH);

    propertyMap.put("shortname", sourceId);
    propertyMap.put("username", user);
    propertyMap.put("password", pass);

    return serviceManager.createManagedService(FACTORY_PID, propertyMap);
  }
}
