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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.response.ValidatableResponse;

public class OpenSearchTestCommons {

    public static final String OPENSEARCH_SYMBOLIC_NAME = "catalog-opensearch-source";

    public static final String OPENSEARCH_FACTORY_PID = "OpenSearchSource";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    public static Map<String, Object> getOpenSearchSourceProperties(String sourceId,
            String openSearchUrl, ServiceManager serviceManager) {
        Map<String, Object> openSearchSourcePropertes = new HashMap<>();
        openSearchSourcePropertes.putAll(serviceManager.getMetatypeDefaults(OPENSEARCH_SYMBOLIC_NAME,
                OPENSEARCH_FACTORY_PID));
        openSearchSourcePropertes.put("shortname", sourceId);
        openSearchSourcePropertes.put("endpointUrl", openSearchUrl);
        return openSearchSourcePropertes;
    }

    /**
     * Gets the validatable response of the open search query
     * When admin is set to true, there is additional authentication specifications
     * for setting up as an admin
     *
     * @return Returns the ValidatableResponse object that will be used for validation
     * @params format       The search format to be used. Default is atom.
     * @params username     The username is used for authentication
     * @params password     The password is used for authentication
     * @params queryParams  Input search query parameters to create an OpenSearch query
     */
    public static ValidatableResponse getOpenSearch(String format, String username, String password,
            String... queryParams) {
        StringBuilder buffer =
                new StringBuilder(AbstractIntegrationTest.OPENSEARCH_PATH.getUrl()).append("?")
                        .append("format=")
                        .append(format);

        Arrays.stream(queryParams)
                .forEach(term -> buffer.append("&")
                        .append(term));

        String url = buffer.toString();
        LOGGER.debug("Getting Open Search response to {}", url);

        if (StringUtils.isBlank(username) && StringUtils.isBlank(password)) {
            return when().get(url)
                    .then();
        } else {
            return given().auth()
                    .preemptive()
                    .basic(username, password)
                    .when()
                    .get(url)
                    .then();
        }
    }
}