/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.itests.common.opensearch;

import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.itests.common.ServiceManager;

public class OpenSearchTestCommons {

    public static final String OPENSEARCH_SYMBOLIC_NAME = "catalog-opensearch-source";

    public static final String OPENSEARCH_FACTORY_PID = "OpenSearchSource";

    public static Map<String, Object> getOpenSearchSourceProperties(String sourceId, String openSearchUrl, ServiceManager serviceManager) {
        Map<String, Object> openSearchSourcePropertes = new HashMap<>();
        openSearchSourcePropertes.putAll(serviceManager.getMetatypeDefaults(OPENSEARCH_SYMBOLIC_NAME, OPENSEARCH_FACTORY_PID));
        openSearchSourcePropertes.put("shortname", sourceId);
        openSearchSourcePropertes.put("endpointUrl", openSearchUrl);
        return openSearchSourcePropertes;
    }
}
