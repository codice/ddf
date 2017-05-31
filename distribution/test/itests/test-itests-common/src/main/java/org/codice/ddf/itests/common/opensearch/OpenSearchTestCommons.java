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

import static org.codice.ddf.itests.common.AbstractIntegrationTest.OPENSEARCH_PATH;
import static com.jayway.restassured.RestAssured.when;

import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.itests.common.ServiceManager;

import com.jayway.restassured.response.ValidatableResponse;

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

    public static ValidatableResponse executeOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH.getUrl()).append("?")
                .append("format=")
                .append(format);

        for (String term : query) {
            buffer.append("&")
                    .append(term);
        }

        String url = buffer.toString();

        return when().get(url)
                .then();
    }
}
